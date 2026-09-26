package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.model.ActiveQuest;
import com.example.pvpscheduler.model.Quest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {
    private final JavaPlugin plugin;
    private final Map<String, Quest> availableQuests = new HashMap<>();
    private final Map<UUID, List<ActiveQuest>> playerQuests = new ConcurrentHashMap<>();
    private File questsFile;
    private FileConfiguration questsConfig;
    
    public QuestManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadQuests();
    }
    
    private void loadQuests() {
        questsFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questsFile.exists()) {
            createDefaultQuests();
        }
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);
        loadQuestsFromConfig();
    }
    
    private void createDefaultQuests() {
        try {
            questsFile.createNewFile();
            questsConfig = YamlConfiguration.loadConfiguration(questsFile);
            
            // Default quests
            questsConfig.set("quests.kill_5_players.id", "kill_5_players");
            questsConfig.set("quests.kill_5_players.name", "Kẻ Sát Thủ");
            questsConfig.set("quests.kill_5_players.description", "Giết 5 người chơi trong PvP");
            questsConfig.set("quests.kill_5_players.type", "KILL_PLAYERS");
            questsConfig.set("quests.kill_5_players.target", 5);
            questsConfig.set("quests.kill_5_players.reward.command", "give {player} diamond 10");
            questsConfig.set("quests.kill_5_players.reward.message", "Bạn đã hoàn thành nhiệm vụ!");
            
            questsConfig.set("quests.kill_streak_10.id", "kill_streak_10");
            questsConfig.set("quests.kill_streak_10.name", "Chuỗi Giết Người");
            questsConfig.set("quests.kill_streak_10.description", "Đạt chuỗi giết 10 người");
            questsConfig.set("quests.kill_streak_10.type", "KILL_STREAK");
            questsConfig.set("quests.kill_streak_10.target", 10);
            questsConfig.set("quests.kill_streak_10.reward.command", "give {player} emerald 5");
            questsConfig.set("quests.kill_streak_10.reward.message", "Chuỗi giết người tuyệt vời!");
            
            questsConfig.save(questsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create quests.yml!");
        }
    }
    
    private void loadQuestsFromConfig() {
        if (questsConfig.getConfigurationSection("quests") == null) return;
        
        for (String key : questsConfig.getConfigurationSection("quests").getKeys(false)) {
            String path = "quests." + key;
            try {
                String id = questsConfig.getString(path + ".id");
                String name = questsConfig.getString(path + ".name");
                String description = questsConfig.getString(path + ".description");
                Quest.QuestType type = Quest.QuestType.valueOf(questsConfig.getString(path + ".type"));
                int target = questsConfig.getInt(path + ".target");
                String rewardCmd = questsConfig.getString(path + ".reward.command");
                String rewardMsg = questsConfig.getString(path + ".reward.message");
                
                Quest.QuestReward reward = new Quest.QuestReward(rewardCmd, rewardMsg);
                long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
                
                Quest quest = new Quest(id, name, description, type, target, reward, expiryTime);
                availableQuests.put(id, quest);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load quest: " + key);
            }
        }
    }
    
    public void assignQuest(UUID playerId, String questId) {
        Quest quest = availableQuests.get(questId);
        if (quest == null) return;
        
        List<ActiveQuest> quests = playerQuests.computeIfAbsent(playerId, k -> new ArrayList<>());
        if (quests.stream().anyMatch(aq -> aq.getQuest().getId().equals(questId) && !aq.isCompleted())) {
            return; // Already has this quest
        }
        
        quests.add(new ActiveQuest(playerId, quest));
    }
    
    public void assignRandomQuest(UUID playerId) {
        if (availableQuests.isEmpty()) return;
        List<Quest> questList = new ArrayList<>(availableQuests.values());
        Quest randomQuest = questList.get(new Random().nextInt(questList.size()));
        assignQuest(playerId, randomQuest.getId());
    }
    
    public void updateQuestProgress(UUID playerId, Quest.QuestType type, int amount) {
        List<ActiveQuest> quests = playerQuests.get(playerId);
        if (quests == null) return;
        
        for (ActiveQuest activeQuest : quests) {
            if (activeQuest.isCompleted()) continue;
            if (activeQuest.getQuest().getType() == type) {
                activeQuest.addProgress(amount);
                if (activeQuest.isCompleted()) {
                    completeQuest(playerId, activeQuest);
                }
            }
        }
    }
    
    public void updateKillStreakQuest(UUID playerId, int killStreak) {
        List<ActiveQuest> quests = playerQuests.get(playerId);
        if (quests == null) return;
        
        for (ActiveQuest activeQuest : quests) {
            if (activeQuest.isCompleted()) continue;
            if (activeQuest.getQuest().getType() == Quest.QuestType.KILL_STREAK) {
                activeQuest.setProgress(killStreak);
                if (activeQuest.isCompleted()) {
                    completeQuest(playerId, activeQuest);
                }
            }
        }
    }
    
    private void completeQuest(UUID playerId, ActiveQuest activeQuest) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        
        Quest quest = activeQuest.getQuest();
        Quest.QuestReward reward = quest.getReward();
        
        // Execute reward command
        String command = reward.getCommand().replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        
        // Send reward message
        player.sendMessage(reward.getMessage());
    }
    
    public List<ActiveQuest> getPlayerQuests(UUID playerId) {
        return playerQuests.getOrDefault(playerId, new ArrayList<>());
    }
    
    public void removeExpiredQuests() {
        try {
            for (List<ActiveQuest> quests : playerQuests.values()) {
                if (quests != null) {
                    quests.removeIf(aq -> aq != null && aq.getQuest() != null && aq.getQuest().isExpired());
                }
            }
            // Remove empty entries to prevent memory leak
            playerQuests.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing expired quests: " + e.getMessage());
        }
    }
    
    public void removeExpiredQuestsForPlayer(UUID playerId) {
        try {
            List<ActiveQuest> quests = playerQuests.get(playerId);
            if (quests != null) {
                quests.removeIf(aq -> aq != null && aq.getQuest() != null && aq.getQuest().isExpired());
                // Remove entry if empty
                if (quests.isEmpty()) {
                    playerQuests.remove(playerId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing expired quests for player " + playerId + ": " + e.getMessage());
        }
    }
    
    public Collection<Quest> getAvailableQuests() {
        return availableQuests.values();
    }
}

