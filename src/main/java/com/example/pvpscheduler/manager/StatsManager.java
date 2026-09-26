package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatsManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000; // Giới hạn cache để tránh memory leak
    private File statsFile;
    private FileConfiguration statsConfig;
    
    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadStats();
    }
    
    private void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml!");
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        loadFromFile();
    }
    
    private void loadFromFile() {
        if (statsConfig.getConfigurationSection("players") == null) return;
        
        for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;
                PlayerStats stats = new PlayerStats(uuid);
                stats.addKill(); // Temporary to access private fields
                // Use reflection or make fields accessible
                statsCache.put(uuid, loadPlayerStats(uuid, path));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load stats for " + uuidStr);
            }
        }
    }
    
    private PlayerStats loadPlayerStats(UUID uuid, String path) {
        PlayerStats stats = new PlayerStats(uuid);
        stats.setKills(statsConfig.getInt(path + ".kills", 0));
        stats.setDeaths(statsConfig.getInt(path + ".deaths", 0));
        stats.setBestKillStreak(statsConfig.getInt(path + ".bestKillStreak", 0));
        stats.setTotalPlayTime(statsConfig.getLong(path + ".totalPlayTime", 0));
        return stats;
    }
    
    public PlayerStats getStats(UUID playerId) {
        if (playerId == null) {
            plugin.getLogger().warning("Attempted to get stats for null player ID");
            return null;
        }
        
        // Giới hạn cache size để tránh memory leak
        if (statsCache.size() >= MAX_CACHE_SIZE && !statsCache.containsKey(playerId)) {
            // Remove oldest entries (simple FIFO - có thể cải thiện bằng LRU)
            UUID oldestKey = statsCache.keySet().iterator().next();
            saveStatsInternal(oldestKey);
            statsCache.remove(oldestKey);
        }
        
        return statsCache.computeIfAbsent(playerId, PlayerStats::new);
    }
    
    public void removePlayerFromCache(UUID playerId) {
        if (playerId != null) {
            saveStats(playerId);
            statsCache.remove(playerId);
        }
    }
    
    public void addKill(UUID playerId) {
        try {
            getStats(playerId).addKill();
            saveStatsInternal(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add kill for " + playerId + ": " + e.getMessage());
        }
    }
    
    public void addDeath(UUID playerId) {
        try {
            getStats(playerId).addDeath();
            saveStatsInternal(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add death for " + playerId + ": " + e.getMessage());
        }
    }
    
    public void saveStats(UUID playerId) {
        try {
            PlayerStats stats = statsCache.get(playerId);
            if (stats == null) return;
            
            String path = "players." + playerId.toString();
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".deaths", stats.getDeaths());
            statsConfig.set(path + ".bestKillStreak", stats.getBestKillStreak());
            statsConfig.set(path + ".totalPlayTime", stats.getTotalPlayTime());
            saveFile();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + playerId + ": " + e.getMessage());
        }
    }
    
    private void saveStatsInternal(UUID playerId) {
        try {
            PlayerStats stats = getStats(playerId);
            String path = "players." + playerId.toString();
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".deaths", stats.getDeaths());
            statsConfig.set(path + ".bestKillStreak", stats.getBestKillStreak());
            statsConfig.set(path + ".totalPlayTime", stats.getTotalPlayTime());
            saveFile();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + playerId + ": " + e.getMessage());
        }
    }
    
    public void saveAllStats() {
        try {
            // Create a copy to avoid ConcurrentModificationException
            Set<UUID> playerIds = new HashSet<>(statsCache.keySet());
            for (UUID playerId : playerIds) {
                saveStats(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save all stats: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveFile() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml!");
        }
    }
    
    public List<Map.Entry<UUID, PlayerStats>> getTopKillers(int limit) {
        // Load lại từ file để đảm bảo có đầy đủ dữ liệu
        reloadAllStatsFromFile();
        
        return statsCache.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<Map.Entry<UUID, PlayerStats>> getTopKDR(int limit) {
        // Load lại từ file để đảm bảo có đầy đủ dữ liệu
        reloadAllStatsFromFile();
        
        return statsCache.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getKDR(), a.getValue().getKDR()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<Map.Entry<UUID, PlayerStats>> getTopKillStreaks(int limit) {
        // Load lại từ file để đảm bảo có đầy đủ dữ liệu
        reloadAllStatsFromFile();
        
        return statsCache.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getBestKillStreak(), a.getValue().getBestKillStreak()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Reload tất cả stats từ file vào cache để đảm bảo dữ liệu đầy đủ khi hiển thị top
     * Merge dữ liệu từ file và cache, ưu tiên giá trị cao hơn
     */
    private void reloadAllStatsFromFile() {
        try {
            // Reload config từ file
            statsConfig = YamlConfiguration.loadConfiguration(statsFile);
            
            if (statsConfig.getConfigurationSection("players") == null) return;
            
            // Load tất cả players từ file vào cache
            for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "players." + uuidStr;
                    
                    PlayerStats cachedStats = statsCache.get(uuid);
                    PlayerStats fileStats = loadPlayerStats(uuid, path);
                    
                    if (cachedStats == null) {
                        // Chưa có trong cache, thêm vào
                        statsCache.put(uuid, fileStats);
                    } else {
                        // Merge dữ liệu: lấy giá trị cao hơn từ cache hoặc file
                        // Vì kills/deaths chỉ tăng, nên giá trị cao hơn = dữ liệu mới hơn
                        if (fileStats.getKills() > cachedStats.getKills()) {
                            cachedStats.setKills(fileStats.getKills());
                        }
                        if (fileStats.getDeaths() > cachedStats.getDeaths()) {
                            cachedStats.setDeaths(fileStats.getDeaths());
                        }
                        if (fileStats.getBestKillStreak() > cachedStats.getBestKillStreak()) {
                            cachedStats.setBestKillStreak(fileStats.getBestKillStreak());
                        }
                        if (fileStats.getTotalPlayTime() > cachedStats.getTotalPlayTime()) {
                            cachedStats.setTotalPlayTime(fileStats.getTotalPlayTime());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to reload stats for " + uuidStr + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reload all stats from file: " + e.getMessage());
        }
    }
    
    public String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }
}

