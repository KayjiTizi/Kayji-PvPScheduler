package com.example.pvpscheduler.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AuditLog - Ghi log audit cho admin actions
 */
public class AuditLog {
    private final JavaPlugin plugin;
    private PrintWriter auditLogWriter;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public AuditLog(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLogging();
    }
    
    private void setupLogging() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File logsFolder = new File(dataFolder, "audit_logs");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }
            
            String dateStr = LocalDateTime.now().format(dateFormatter);
            File auditLogFile = new File(logsFolder, "audit_" + dateStr + ".log");
            
            auditLogWriter = new PrintWriter(new FileWriter(auditLogFile, true));
            logAudit("SYSTEM", "SYSTEM", "AuditLog initialized", null, "System initialization");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to setup audit logging: " + e.getMessage());
        }
    }
    
    public void logAdminAction(Player admin, String action, String target, String details) {
        if (admin == null) return;
        logAudit(admin.getName(), admin.getUniqueId().toString(), action, target, details);
    }
    
    public void logConfigChange(Player admin, String configKey, String oldValue, String newValue) {
        if (admin == null) return;
        logAudit(admin.getName(), admin.getUniqueId().toString(), "CONFIG_CHANGE", 
            configKey, String.format("Old: %s -> New: %s", oldValue, newValue));
    }
    
    public void logPlayerDataModification(Player admin, String targetPlayer, String action, String details) {
        if (admin == null) return;
        logAudit(admin.getName(), admin.getUniqueId().toString(), "PLAYER_DATA_MODIFY", 
            targetPlayer, String.format("%s: %s", action, details));
    }
    
    public void logPermissionChange(Player admin, String targetPlayer, String permission, boolean granted) {
        if (admin == null) return;
        logAudit(admin.getName(), admin.getUniqueId().toString(), "PERMISSION_CHANGE", 
            targetPlayer, String.format("Permission: %s, Granted: %s", permission, granted));
    }
    
    public void logSystemAction(String action, String details) {
        logAudit("SYSTEM", "SYSTEM", action, null, details);
    }
    
    public void logSecurityEvent(String playerName, String action, String details) {
        logAudit(playerName, "UNKNOWN", "SECURITY_EVENT", null, 
            String.format("%s: %s", action, details));
    }
    
    private void logAudit(String actor, String actorId, String action, String target, String details) {
        try {
            String timestamp = LocalDateTime.now().format(timeFormatter);
            String logEntry;
            
            if (target != null) {
                logEntry = String.format("[%s] [%s] Actor: %s (%s) | Target: %s | Details: %s",
                    timestamp, action, actor, actorId, target, details);
            } else {
                logEntry = String.format("[%s] [%s] Actor: %s (%s) | Details: %s",
                    timestamp, action, actor, actorId, details);
            }
            
            if (auditLogWriter != null) {
                auditLogWriter.println(logEntry);
                auditLogWriter.flush();
            }
            
            // Also log to console for important actions
            if (action.contains("SECURITY") || action.contains("CONFIG_CHANGE") || 
                action.contains("PLAYER_DATA_MODIFY")) {
                plugin.getLogger().warning("[AUDIT] " + logEntry);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error writing audit log: " + e.getMessage());
        }
    }
    
    public void cleanup() {
        try {
            if (auditLogWriter != null) {
                auditLogWriter.close();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up AuditLog: " + e.getMessage());
        }
    }
}

