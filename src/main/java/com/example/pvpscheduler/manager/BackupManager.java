package com.example.pvpscheduler.manager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * BackupManager - Tự động backup data (DISABLED - Tối ưu hóa để giảm lag)
 * Chỉ sử dụng khi cần thiết, được tối ưu để tránh ảnh hưởng đến performance
 */
public class BackupManager {
    private final JavaPlugin plugin;
    private BukkitRunnable backupTask;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private static final int MAX_BACKUPS = 5; // Giảm từ 10 xuống 5 để tiết kiệm không gian
    private static final long BACKUP_INTERVAL = 3600000; // 1 hour (in milliseconds)
    private static final int BUFFER_SIZE = 8192; // Tăng từ 1024 lên 8192 - tối ưu I/O
    
    public BackupManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startAutoBackup() {
        if (backupTask != null && !backupTask.isCancelled()) {
            backupTask.cancel();
        }
        
        long interval = plugin.getConfig().getLong("backup.interval", BACKUP_INTERVAL) / 50; // Convert to ticks
        
        backupTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    createBackup();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in auto backup: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        backupTask.runTaskTimerAsynchronously(plugin, interval, interval);
    }
    
    public boolean createBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder == null || !dataFolder.exists()) {
                plugin.getLogger().warning("Data folder does not exist, cannot create backup");
                return false;
            }
            
            File backupFolder = new File(dataFolder.getParentFile(), "PvPScheduler_Backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(dateFormatter);
            File backupFile = new File(backupFolder, "backup_" + timestamp + ".zip");
            
            plugin.getLogger().info("Creating backup: " + backupFile.getName());
            
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
                addDirectoryToZip(dataFolder, dataFolder, zos);
            }
            
            plugin.getLogger().info("Backup created successfully: " + backupFile.getName());
            
            // Cleanup old backups
            cleanupOldBackups(backupFolder);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void addDirectoryToZip(File sourceDir, File baseDir, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Skip backup folder itself
                if (file.getName().equals("PvPScheduler_Backups")) {
                    continue;
                }
                addDirectoryToZip(file, baseDir, zos);
            } else {
                // Skip log files if configured
                if (plugin.getConfig().getBoolean("backup.exclude-logs", false) && 
                    (file.getName().endsWith(".log") || file.getParentFile().getName().contains("log"))) {
                    continue;
                }
                
                String relativePath = baseDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zos.putNextEntry(zipEntry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE]; // Sử dụng buffer lớn hơn
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }
    
    private void cleanupOldBackups(File backupFolder) {
        // Chạy cleanup bất đồng bộ để không block thread chính
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    File[] backups = backupFolder.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".zip"));
                    if (backups == null || backups.length <= MAX_BACKUPS) {
                        return;
                    }
                    
                    // Sắp xếp trực tiếp theo lastModified để tối ưu hơn
                    java.util.Arrays.sort(backups, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                    
                    // Xoá backup cũ nhất - chỉ xoá một file một lúc
                    int toDelete = backups.length - MAX_BACKUPS;
                    for (int i = 0; i < toDelete && i < backups.length; i++) {
                        if (backups[i].delete()) {
                            plugin.getLogger().fine("Deleted old backup: " + backups[i].getName());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error cleaning up old backups: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    public boolean restoreBackup(String backupFileName) {
        try {
            File dataFolder = plugin.getDataFolder();
            File backupFolder = new File(dataFolder.getParentFile(), "PvPScheduler_Backups");
            File backupFile = new File(backupFolder, backupFileName);
            
            if (!backupFile.exists()) {
                plugin.getLogger().severe("Backup file not found: " + backupFileName);
                return false;
            }
            
            plugin.getLogger().info("Restoring backup: " + backupFileName);
            
            // Create temporary backup of current data
            String tempBackupName = "temp_backup_before_restore_" + 
                LocalDateTime.now().format(dateFormatter) + ".zip";
            File tempBackup = new File(backupFolder, tempBackupName);
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempBackup))) {
                addDirectoryToZip(dataFolder, dataFolder, zos);
            }
            
            // Extract backup
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    new FileInputStream(backupFile))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File file = new File(dataFolder, entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        file.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[BUFFER_SIZE]; // Sử dụng buffer lớn hơn
                            int length;
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            
            plugin.getLogger().info("Backup restored successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<String> listBackups() {
        List<String> backups = new ArrayList<>();
        try {
            File dataFolder = plugin.getDataFolder();
            File backupFolder = new File(dataFolder.getParentFile(), "PvPScheduler_Backups");
            
            if (!backupFolder.exists()) {
                return backups;
            }
            
            File[] backupFiles = backupFolder.listFiles((dir, name) -> 
                name.startsWith("backup_") && name.endsWith(".zip"));
            
            if (backupFiles != null) {
                for (File backup : backupFiles) {
                    backups.add(backup.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error listing backups: " + e.getMessage());
        }
        return backups;
    }
    
    public void stopAutoBackup() {
        if (backupTask != null && !backupTask.isCancelled()) {
            backupTask.cancel();
        }
    }
}

