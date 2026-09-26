package com.example.pvpscheduler.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NotificationManager - Quản lý thông báo hệ thống
 */
public class NotificationManager {
    private final JavaPlugin plugin;
    private final Map<UUID, List<Notification>> playerNotifications = new ConcurrentHashMap<>();
    private final Map<UUID, NotificationSettings> playerSettings = new ConcurrentHashMap<>();
    
    public NotificationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getLogger().fine("[NotificationManager] Initialized");
    }
    
    public void sendNotification(Player player, String title, String message, NotificationType type) {
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        NotificationSettings settings = playerSettings.get(playerId);
        
        // Check if player has disabled this notification type
        if (settings != null && !settings.isEnabled(type)) {
            return;
        }
        
        Notification notification = new Notification(title, message, type, System.currentTimeMillis());
        
        // Add to player's notification list
        List<Notification> notifications = playerNotifications.computeIfAbsent(playerId, 
            k -> new ArrayList<>());
        notifications.add(notification);
        
        // Keep only recent notifications
        if (notifications.size() > 50) {
            notifications.remove(0);
        }
        
        // Send to player
        sendToPlayer(player, notification);
    }
    
    public void sendBroadcast(String title, String message, NotificationType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendNotification(player, title, message, type);
        }
    }
    
    public void sendToAdmins(String title, String message, NotificationType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pvpscheduler.admin") || player.isOp()) {
                sendNotification(player, title, message, type);
            }
        }
    }
    
    private void sendToPlayer(Player player, Notification notification) {
        // Send title and subtitle
        player.sendTitle(
            "§6" + notification.getTitle(),
            "§7" + notification.getMessage(),
            10, 40, 10
        );
        
        // Also send chat message
        String color = getColorForType(notification.getType());
        player.sendMessage(color + "[PvPScheduler] " + notification.getTitle() + ": " + 
            notification.getMessage());
    }
    
    private String getColorForType(NotificationType type) {
        switch (type) {
            case INFO: return "§b";
            case SUCCESS: return "§a";
            case WARNING: return "§e";
            case ERROR: return "§c";
            case REWARD: return "§6";
            case QUEST: return "§d";
            default: return "§f";
        }
    }
    
    public void sendPvPStartNotification() {
        sendBroadcast("§c⚔ PvP Đã Bật", "Hệ thống PvP đã được kích hoạt!", NotificationType.WARNING);
    }
    
    public void sendPvPStopNotification() {
        sendBroadcast("§a✓ PvP Đã Tắt", "Hệ thống PvP đã được tắt!", NotificationType.INFO);
    }
    
    public void sendCountdownNotification(int seconds) {
        String message = "PvP sẽ bật sau " + seconds + " giây!";
        if (seconds <= 10) {
            sendBroadcast("§c⚠ Cảnh Báo PvP", message, NotificationType.WARNING);
        } else {
            sendBroadcast("§e⏰ Thông Báo PvP", message, NotificationType.INFO);
        }
    }
    
    public void sendQuestNotification(Player player, String questName, boolean completed) {
        if (completed) {
            sendNotification(player, "§a✓ Nhiệm Vụ Hoàn Thành", 
                "Bạn đã hoàn thành nhiệm vụ: " + questName, NotificationType.QUEST);
        } else {
            sendNotification(player, "§d📋 Nhiệm Vụ Mới", 
                "Bạn có nhiệm vụ mới: " + questName, NotificationType.QUEST);
        }
    }
    
    public void sendRewardNotification(Player player, String rewardType) {
        sendNotification(player, "§6🎁 Phần Thưởng", 
            "Bạn đã nhận được: " + rewardType, NotificationType.REWARD);
    }
    
    public void sendLeaderboardNotification(Player player, int position) {
        sendNotification(player, "§b🏆 Bảng Xếp Hạng", 
            "Bạn đang ở vị trí #" + position, NotificationType.INFO);
    }
    
    public List<Notification> getPlayerNotifications(UUID playerId) {
        return new ArrayList<>(playerNotifications.getOrDefault(playerId, new ArrayList<>()));
    }
    
    public void setNotificationEnabled(UUID playerId, NotificationType type, boolean enabled) {
        NotificationSettings settings = playerSettings.computeIfAbsent(playerId, 
            k -> new NotificationSettings());
        settings.setEnabled(type, enabled);
    }
    
    public void cleanupPlayerData(UUID playerId) {
        playerNotifications.remove(playerId);
        playerSettings.remove(playerId);
    }
    
    public void cleanup() {
        playerNotifications.clear();
        playerSettings.clear();
    }
    
    // Inner classes
    public static class Notification {
        private final String title;
        private final String message;
        private final NotificationType type;
        private final long timestamp;
        
        public Notification(String title, String message, NotificationType type, long timestamp) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class NotificationSettings {
        private final Map<NotificationType, Boolean> enabledTypes = new HashMap<>();
        
        public NotificationSettings() {
            // Default: all enabled
            for (NotificationType type : NotificationType.values()) {
                enabledTypes.put(type, true);
            }
        }
        
        public boolean isEnabled(NotificationType type) {
            return enabledTypes.getOrDefault(type, true);
        }
        
        public void setEnabled(NotificationType type, boolean enabled) {
            enabledTypes.put(type, enabled);
        }
    }
    
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR, REWARD, QUEST, SYSTEM
    }
}

