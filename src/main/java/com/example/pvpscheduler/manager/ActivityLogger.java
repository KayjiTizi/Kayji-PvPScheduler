package com.example.pvpscheduler.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActivityLogger - Ghi log tất cả hoạt động của players
 */
public class ActivityLogger implements Listener {
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, PlayerActivity> activeSessions = new ConcurrentHashMap<>();
    
    private File activityLogFile;
    private PrintWriter activityLogWriter;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public ActivityLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLogging();
    }
    
    private void setupLogging() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File logsFolder = new File(dataFolder, "activity_logs");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }
            
            String dateStr = LocalDateTime.now().format(dateFormatter);
            activityLogFile = new File(logsFolder, "activity_" + dateStr + ".log");
            
            activityLogWriter = new PrintWriter(new FileWriter(activityLogFile, true));
            logActivity("SYSTEM", "SYSTEM", "ActivityLogger initialized");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to setup activity logging: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            if (event == null || event.getPlayer() == null) return;
            
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            PlayerActivity activity = new PlayerActivity(playerId, player.getName());
            activity.setJoinTime(LocalDateTime.now());
            activity.setIpAddress(player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown");
            activeSessions.put(playerId, activity);
            
            logActivity(player.getName(), "JOIN", "Player joined the server");
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging player join: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            if (event == null || event.getPlayer() == null) return;
            
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            PlayerActivity activity = activeSessions.remove(playerId);
            if (activity != null) {
                activity.setQuitTime(LocalDateTime.now());
                long sessionDuration = java.time.Duration.between(activity.getJoinTime(), activity.getQuitTime()).getSeconds();
                
                logActivity(player.getName(), "QUIT", 
                    String.format("Player left. Session duration: %d seconds, Kills: %d, Deaths: %d",
                        sessionDuration, activity.getKills(), activity.getDeaths()));
            } else {
                logActivity(player.getName(), "QUIT", "Player left (no session data)");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging player quit: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            if (event == null || event.getEntity() == null) return;
            
            Player victim = event.getEntity();
            Player killer = victim.getKiller();
            
            UUID victimId = victim.getUniqueId();
            PlayerActivity victimActivity = activeSessions.get(victimId);
            if (victimActivity != null) {
                victimActivity.incrementDeaths();
            }
            
            if (killer != null) {
                UUID killerId = killer.getUniqueId();
                PlayerActivity killerActivity = activeSessions.get(killerId);
                if (killerActivity != null) {
                    killerActivity.incrementKills();
                }
                
                logActivity(killer.getName(), "KILL", 
                    String.format("Killed %s at %s", victim.getName(), 
                        formatLocation(victim.getLocation())));
            } else {
                logActivity(victim.getName(), "DEATH", 
                    String.format("Died at %s", formatLocation(victim.getLocation())));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging player death: " + e.getMessage());
        }
    }
    
    public void logPvPStateChange(boolean enabled) {
        logActivity("SYSTEM", "PvP_STATE_CHANGE", 
            String.format("PvP %s", enabled ? "ENABLED" : "DISABLED"));
    }
    
    public void logQuestCompleted(UUID playerId, String questName) {
        try {
            PlayerActivity activity = activeSessions.get(playerId);
            if (activity != null) {
                activity.incrementQuestsCompleted();
                logActivity(activity.getPlayerName(), "QUEST_COMPLETE", 
                    String.format("Completed quest: %s", questName));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging quest completion: " + e.getMessage());
        }
    }
    
    public void logRewardReceived(UUID playerId, String rewardType) {
        try {
            PlayerActivity activity = activeSessions.get(playerId);
            if (activity != null) {
                activity.incrementRewardsReceived();
                logActivity(activity.getPlayerName(), "REWARD", 
                    String.format("Received reward: %s", rewardType));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging reward: " + e.getMessage());
        }
    }
    
    public void logCommandExecution(String playerName, String command) {
        logActivity(playerName, "COMMAND", String.format("Executed: %s", command));
    }
    
    public void logActivity(String playerName, String action, String details) {
        logActivity(playerName, action, details, null);
    }
    
    public void logActivity(String playerName, String action, String details, UUID playerId) {
        try {
            String timestamp = LocalDateTime.now().format(timeFormatter);
            String logEntry = String.format("[%s] [%s] %s: %s",
                timestamp, action, playerName, details);
            
            if (activityLogWriter != null) {
                activityLogWriter.println(logEntry);
                activityLogWriter.flush();
            }
            
            // Log to console if debug mode
            if (plugin.getConfig().getBoolean("logging.debug", false)) {
                plugin.getLogger().info("[ACTIVITY] " + logEntry);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error writing activity log: " + e.getMessage());
        }
    }
    
    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "unknown";
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f",
            loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
            loc.getX(), loc.getY(), loc.getZ());
    }
    
    public PlayerActivity getPlayerActivity(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    public void cleanup() {
        try {
            if (activityLogWriter != null) {
                activityLogWriter.close();
            }
            activeSessions.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up ActivityLogger: " + e.getMessage());
        }
    }
    
    // Inner class để track activity của player
    public static class PlayerActivity {
        private final UUID playerId;
        private final String playerName;
        private LocalDateTime joinTime;
        private LocalDateTime quitTime;
        private String ipAddress;
        private int kills = 0;
        private int deaths = 0;
        private int questsCompleted = 0;
        private int rewardsReceived = 0;
        
        public PlayerActivity(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }
        
        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void incrementQuestsCompleted() { questsCompleted++; }
        public void incrementRewardsReceived() { rewardsReceived++; }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public LocalDateTime getJoinTime() { return joinTime; }
        public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }
        public LocalDateTime getQuitTime() { return quitTime; }
        public void setQuitTime(LocalDateTime quitTime) { this.quitTime = quitTime; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getQuestsCompleted() { return questsCompleted; }
        public int getRewardsReceived() { return rewardsReceived; }
    }
}

