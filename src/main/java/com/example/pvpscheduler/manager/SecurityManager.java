package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.security.SecurityPolicy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SecurityManager - Quản lý bảo mật và phát hiện hoạt động đáng ngờ
 */
public class SecurityManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerSecurityData> playerData = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastViolationTime = new ConcurrentHashMap<>();
    private SecurityPolicy securityPolicy;
    
    private static final int MAX_VIOLATIONS = 5;
    private static final long VIOLATION_RESET_TIME = 300000; // 5 minutes
    
    private File securityLogFile;
    private PrintWriter securityLogWriter;
    
    public SecurityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLogging();
        initializeSecurityPolicy();
    }
    
    private void initializeSecurityPolicy() {
        try {
            securityPolicy = new SecurityPolicy(plugin);
            securityPolicy.enable();
            plugin.getLogger().info("Security policy initialized and enabled");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize security policy: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupLogging() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File logsFolder = new File(dataFolder, "security_logs");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }
            
            securityLogFile = new File(logsFolder, "security_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log");
            
            securityLogWriter = new PrintWriter(new FileWriter(securityLogFile, true));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to setup security logging: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            if (event == null || event.getPlayer() == null) return;
            
            UUID playerId = event.getPlayer().getUniqueId();
            playerData.put(playerId, new PlayerSecurityData());
            
            // Kiểm tra IP đáng ngờ (có thể mở rộng)
            checkSuspiciousActivity(event.getPlayer());
        } catch (Exception e) {
            plugin.getLogger().warning("Error in SecurityManager.onPlayerJoin: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            if (event == null || event.getPlayer() == null) return;
            
            UUID playerId = event.getPlayer().getUniqueId();
            playerData.remove(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error in SecurityManager.onPlayerQuit: " + e.getMessage());
        }
    }
    
    private void addViolation(UUID playerId) {
        if (playerId == null) return;
        
        int newCount = violationCounts.getOrDefault(playerId, 0) + 1;
        violationCounts.put(playerId, newCount);
        lastViolationTime.put(playerId, System.currentTimeMillis());

        if (newCount >= MAX_VIOLATIONS) {
            Player player = plugin.getServer().getPlayer(playerId);
            String playerName = player != null ? player.getName() : "Unknown";

            logSecurityEvent(
                playerId,
                playerName,
                "VIOLATION_THRESHOLD",
                "Exceeded maximum allowed security violations (" + newCount + "/" + MAX_VIOLATIONS + ")"
            );
            notifyAdmins("§c[SECURITY] " + playerName + " đã vượt quá giới hạn vi phạm bảo mật!");

            violationCounts.put(playerId, 0);
            lastViolationTime.put(playerId, System.currentTimeMillis());
        }
    }
    
    private int getViolationCount(UUID playerId) {
        if (playerId == null) return 0;
        
        Long lastTime = lastViolationTime.get(playerId);
        if (lastTime != null && System.currentTimeMillis() - lastTime > VIOLATION_RESET_TIME) {
            violationCounts.remove(playerId);
            lastViolationTime.remove(playerId);
            return 0;
        }
        
        return violationCounts.getOrDefault(playerId, 0);
    }
    
    private void checkSuspiciousActivity(Player player) {
        // Có thể thêm logic kiểm tra IP, tên đăng nhập đáng ngờ, etc.
        // Ví dụ: kiểm tra nếu player join từ IP đã bị ban trước đó
    }
    
    public void logSecurityEvent(UUID playerId, String playerName, String eventType, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = String.format("[%s] [%s] Player: %s (%s) - Details: %s",
                timestamp, eventType, playerName, playerId, details);
            
            plugin.getLogger().warning("[SECURITY] " + logEntry);
            
            if (securityLogWriter != null) {
                securityLogWriter.println(logEntry);
                securityLogWriter.flush();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error logging security event: " + e.getMessage());
        }
    }
    
    private void notifyAdmins(String message) {
        plugin.getServer().getOnlinePlayers().stream()
            .filter(p -> p.hasPermission("pvpscheduler.admin") || p.isOp())
            .forEach(p -> p.sendMessage(message));
    }
    
    public void detectSuspiciousKillPattern(UUID playerId, String playerName) {
        // Phát hiện kill pattern đáng ngờ (ví dụ: quá nhiều kill trong thời gian ngắn)
        PlayerSecurityData data = playerData.get(playerId);
        if (data != null && data.getRecentKills() > 20) {
            logSecurityEvent(playerId, playerName, "SUSPICIOUS_KILL_PATTERN", 
                "Killed " + data.getRecentKills() + " players in short time");
            addViolation(playerId);
            int currentViolations = getViolationCount(playerId);
            notifyAdmins("§c[SECURITY] " + playerName + " có pattern kill đáng ngờ! (Vi phạm: "
                + currentViolations + "/" + MAX_VIOLATIONS + ")");
        }
    }
    
    public void recordKill(UUID playerId) {
        PlayerSecurityData data = playerData.get(playerId);
        if (data != null) {
            data.recordKill();
        }
    }
    
    public void cleanup() {
        try {
            if (securityPolicy != null) {
                securityPolicy.cleanup();
            }
            if (securityLogWriter != null) {
                securityLogWriter.close();
            }
            playerData.clear();
            violationCounts.clear();
            lastViolationTime.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up SecurityManager: " + e.getMessage());
        }
    }
    
    /**
     * Lấy SecurityPolicy
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }
    
    /**
     * Kiểm tra class có được phép load không
     */
    public boolean isClassAllowed(String className) {
        if (securityPolicy != null) {
            return securityPolicy.isClassAllowed(className);
        }
        return true; // Nếu không có policy, cho phép (fallback)
    }
    
    /**
     * Kiểm tra file có được phép load không
     */
    public boolean isFileAllowed(File file) {
        if (securityPolicy != null) {
            return securityPolicy.isFileAllowed(file);
        }
        return true; // Nếu không có policy, cho phép (fallback)
    }
    
    // Inner class để lưu trữ dữ liệu bảo mật của player
    private static class PlayerSecurityData {
        private int recentKills = 0;
        private long lastKillTime = 0;
        private static final long KILL_RESET_TIME = 60000; // 1 minute
        
        public void recordKill() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKillTime > KILL_RESET_TIME) {
                recentKills = 0;
            }
            recentKills++;
            lastKillTime = currentTime;
        }
        
        public int getRecentKills() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKillTime > KILL_RESET_TIME) {
                recentKills = 0;
            }
            return recentKills;
        }
    }
}

