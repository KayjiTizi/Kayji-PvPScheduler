package com.example.pvpscheduler.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimiter - Giới hạn rate để chống spam/abuse
 */
public class RateLimiter {
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, RateLimitData>> playerLimits = new ConcurrentHashMap<>();
    private int commandLimit;
    private int messageLimit;
    private int actionLimit;
    
    // Default limits
    private static final int DEFAULT_COMMAND_LIMIT = 10; // commands per second
    private static final int DEFAULT_MESSAGE_LIMIT = 5; // messages per second
    private static final int DEFAULT_ACTION_LIMIT = 20; // actions per second
    
    public RateLimiter(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }
    
    public boolean checkRateLimit(Player player, String actionType) {
        return checkRateLimit(player, actionType, getDefaultLimit(actionType));
    }
    
    public boolean checkRateLimit(Player player, String actionType, int maxActions) {
        if (player == null) return false;
        
        UUID playerId = player.getUniqueId();
        Map<String, RateLimitData> limits = playerLimits.computeIfAbsent(playerId, 
            k -> new ConcurrentHashMap<>());
        
        RateLimitData data = limits.computeIfAbsent(actionType, 
            k -> new RateLimitData(maxActions));
        
        long currentTime = System.currentTimeMillis();
        
        // Remove old timestamps (older than 1 second)
        data.getTimestamps().removeIf(timestamp -> currentTime - timestamp > 1000);
        
        // Check if limit exceeded
        if (data.getTimestamps().size() >= maxActions) {
            return false; // Rate limit exceeded
        }
        
        // Record this action
        data.getTimestamps().add(currentTime);
        return true; // Within limit
    }
    
    public boolean checkCommandRateLimit(Player player) {
        return checkRateLimit(player, "command", DEFAULT_COMMAND_LIMIT);
    }
    
    public boolean checkMessageRateLimit(Player player) {
        return checkRateLimit(player, "message", DEFAULT_MESSAGE_LIMIT);
    }
    
    public boolean checkActionRateLimit(Player player) {
        return checkRateLimit(player, "action", DEFAULT_ACTION_LIMIT);
    }
    
    public int getRemainingActions(Player player, String actionType) {
        if (player == null) return 0;
        
        UUID playerId = player.getUniqueId();
        Map<String, RateLimitData> limits = playerLimits.get(playerId);
        if (limits == null) return getDefaultLimit(actionType);
        
        RateLimitData data = limits.get(actionType);
        if (data == null) return getDefaultLimit(actionType);
        
        long currentTime = System.currentTimeMillis();
        data.getTimestamps().removeIf(timestamp -> currentTime - timestamp > 1000);
        
        return Math.max(0, data.getMaxActions() - data.getTimestamps().size());
    }
    
    public void resetRateLimit(Player player, String actionType) {
        if (player == null) return;
        
        UUID playerId = player.getUniqueId();
        Map<String, RateLimitData> limits = playerLimits.get(playerId);
        if (limits != null) {
            limits.remove(actionType);
        }
    }
    
    public void resetAllRateLimits(Player player) {
        if (player == null) return;
        playerLimits.remove(player.getUniqueId());
    }
    
    private int getDefaultLimit(String actionType) {
        switch (actionType.toLowerCase()) {
            case "command": return commandLimit;
            case "message": return messageLimit;
            case "action": return actionLimit;
            default: return actionLimit;
        }
    }
    
    public void cleanupPlayerData(UUID playerId) {
        playerLimits.remove(playerId);
    }
    
    public void cleanup() {
        playerLimits.clear();
    }
    
    public void reloadFromConfig() {
        this.commandLimit = Math.max(1, plugin.getConfig().getInt("rate-limit.command-per-second", DEFAULT_COMMAND_LIMIT));
        this.messageLimit = Math.max(1, plugin.getConfig().getInt("rate-limit.message-per-second", DEFAULT_MESSAGE_LIMIT));
        this.actionLimit = Math.max(1, plugin.getConfig().getInt("rate-limit.action-per-second", DEFAULT_ACTION_LIMIT));
        plugin.getLogger().fine(String.format("[RateLimiter] Loaded limits - command: %d/s, message: %d/s, action: %d/s",
            commandLimit, messageLimit, actionLimit));
    }
    
    public void setRateLimit(String actionType, int maxActions) {
        int sanitized = Math.max(1, maxActions);
        switch (actionType.toLowerCase()) {
            case "command":
                commandLimit = sanitized;
                plugin.getConfig().set("rate-limit.command-per-second", sanitized);
                break;
            case "message":
                messageLimit = sanitized;
                plugin.getConfig().set("rate-limit.message-per-second", sanitized);
                break;
            case "action":
            default:
                actionLimit = sanitized;
                plugin.getConfig().set("rate-limit.action-per-second", sanitized);
                break;
        }
        plugin.saveConfig();
        plugin.getLogger().info("[RateLimiter] Updated " + actionType + " limit to " + sanitized + " actions/second");
    }
    
    // Inner class
    private static class RateLimitData {
        private final int maxActions;
        private final List<Long> timestamps = new ArrayList<>();
        
        public RateLimitData(int maxActions) {
            this.maxActions = maxActions;
        }
        
        public int getMaxActions() { return maxActions; }
        public List<Long> getTimestamps() { return timestamps; }
    }
}

