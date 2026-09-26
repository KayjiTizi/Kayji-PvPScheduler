package com.example.pvpscheduler.security;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileIntegrityManager - Quản lý tính toàn vẹn của file và phát hiện file lạ
 */
public class FileIntegrityManager {
    private final JavaPlugin plugin;
    private final Map<String, String> fileHashes = new ConcurrentHashMap<>();
    private final Set<String> monitoredExtensions = new HashSet<>();
    private final Set<String> blockedExtensions = new HashSet<>();
    private final Set<String> ignoredFileNames = new HashSet<>();
    private final Set<String> autoUpdatingFileNames = new HashSet<>();
    private final Map<String, Long> fileModificationTimes = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running = false;
    
    public FileIntegrityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeSettings();
    }
    
    private void initializeSettings() {
        // Các extension cần monitor
        monitoredExtensions.addAll(Arrays.asList(
            ".jar", ".class", ".java", ".yml", ".yaml", ".json", ".properties"
        ));
        
        // Các extension bị chặn hoàn toàn
        blockedExtensions.addAll(Arrays.asList(
            ".exe", ".bat", ".sh", ".dll", ".so", ".dylib"
        ));

        // Các file cấu hình thay đổi thường xuyên sẽ không cảnh báo integrity
        ignoredFileNames.add("config.yml");
        ignoredFileNames.add("config.yaml");

        autoUpdatingFileNames.add("stats.yml");
        autoUpdatingFileNames.add("stats.yaml");
    }
    
    /**
     * Khởi tạo và bắt đầu monitor file
     */
    public void startMonitoring() {
        if (running) {
            return;
        }
        
        try {
            // Tính hash cho tất cả file quan trọng
            scanPluginFiles();
            
            // Bắt đầu file watcher
            startFileWatcher();
            
            running = true;
            plugin.getLogger().info("File integrity monitoring started");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start file monitoring: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Quét và tính hash cho các file quan trọng
     */
    private void scanPluginFiles() {
        try {
            // Quét plugin JAR
            File pluginJar = getPluginJarFile();
            if (pluginJar != null && pluginJar.exists()) {
                String hash = calculateFileHash(pluginJar);
                if (hash != null) {
                    fileHashes.put(pluginJar.getAbsolutePath(), hash);
                    fileModificationTimes.put(pluginJar.getAbsolutePath(), pluginJar.lastModified());
                }
            }
            
            // Quét data folder
            File dataFolder = plugin.getDataFolder();
            if (dataFolder.exists()) {
                scanDirectory(dataFolder);
            }
            
            plugin.getLogger().info("Scanned " + fileHashes.size() + " files for integrity check");
        } catch (Exception e) {
            plugin.getLogger().warning("Error scanning plugin files: " + e.getMessage());
        }
    }
    
    /**
     * Quét thư mục đệ quy
     */
    private void scanDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Bỏ qua một số thư mục không quan trọng
                if (!file.getName().equals("logs") && !file.getName().equals("cache")) {
                    scanDirectory(file);
                }
            } else {
                String fileName = file.getName().toLowerCase();
                
                // Chỉ monitor các file có extension được chỉ định
                boolean shouldMonitor = monitoredExtensions.stream()
                    .anyMatch(fileName::endsWith);
                
                if (shouldMonitor && !isIgnoredFile(file)) {
                    String hash = calculateFileHash(file);
                    if (hash != null) {
                        fileHashes.put(file.getAbsolutePath(), hash);
                        fileModificationTimes.put(file.getAbsolutePath(), file.lastModified());
                    }
                }
            }
        }
    }
    
    /**
     * Bắt đầu file watcher để phát hiện thay đổi file
     */
    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            File dataFolder = plugin.getDataFolder();
            
            if (dataFolder.exists()) {
                Path path = dataFolder.toPath();
                path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            }
            
            watchThread = new Thread(() -> {
                while (running) {
                    try {
                        WatchKey key = watchService.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path fileName = ev.context();
                            
                            handleFileEvent(kind, fileName.toString());
                        }
                        
                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error in file watcher: " + e.getMessage());
                    }
                }
            }, "FileIntegrityWatcher");
            
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to start file watcher: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý sự kiện file
     */
    private void handleFileEvent(WatchEvent.Kind<?> kind, String fileName) {
        if (fileName == null) {
            return;
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        // Kiểm tra extension bị chặn
        for (String blocked : blockedExtensions) {
            if (lowerFileName.endsWith(blocked)) {
                logSecurityViolation("BLOCKED_FILE_DETECTED", fileName,
                    "Blocked file extension detected: " + blocked);
                return;
            }
        }
        
        File dataFolder = plugin.getDataFolder();
        File file = new File(dataFolder, fileName);
        
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            handleFileCreated(file);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            handleFileModified(file);
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            handleFileDeleted(file);
        }
    }
    
    /**
     * Xử lý file mới được tạo
     */
    private void handleFileCreated(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        String fileName = file.getName().toLowerCase();
        
        // Kiểm tra file Java/Class lạ
        if (fileName.endsWith(".class") || fileName.endsWith(".jar")) {
            logSecurityViolation("SUSPICIOUS_FILE_CREATED", file.getName(),
                "Java/Class file created in data folder: " + file.getAbsolutePath());
        }
        
        // Tính hash và lưu
        String hash = calculateFileHash(file);
        if (hash != null) {
            fileHashes.put(file.getAbsolutePath(), hash);
            fileModificationTimes.put(file.getAbsolutePath(), file.lastModified());
        }
    }
    
    /**
     * Xử lý file bị sửa đổi
     */
    private void handleFileModified(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        if (isIgnoredFile(file)) {
            updateFileHashSilently(file);
            return;
        }
        
        if (isAutoUpdatingFile(file)) {
            updateFileHashSilently(file);
            return;
        }

        String filePath = file.getAbsolutePath();
        String oldHash = fileHashes.get(filePath);
        Long oldModTime = fileModificationTimes.get(filePath);
        
        // Kiểm tra file có thay đổi không
        if (oldHash != null && oldModTime != null) {
            long currentModTime = file.lastModified();
            if (currentModTime != oldModTime) {
                String newHash = calculateFileHash(file);
                
                if (newHash != null && !newHash.equals(oldHash)) {
                    logSecurityViolation("FILE_INTEGRITY_FAILED", file.getName(),
                        "File hash changed! Old: " + oldHash.substring(0, 8) + 
                        "... New: " + newHash.substring(0, 8) + "...");
                    
                    // Cập nhật hash mới
                    fileHashes.put(filePath, newHash);
                    fileModificationTimes.put(filePath, currentModTime);
                }
            }
        }
    }
    
    /**
     * Xử lý file bị xóa
     */
    private void handleFileDeleted(File file) {
        if (file == null) {
            return;
        }
        
        if (isIgnoredFile(file) || isAutoUpdatingFile(file)) {
            fileHashes.remove(file.getAbsolutePath());
            fileModificationTimes.remove(file.getAbsolutePath());
            return;
        }

        String filePath = file.getAbsolutePath();
        
        // Kiểm tra file quan trọng có bị xóa không
        if (fileHashes.containsKey(filePath)) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || 
                fileName.endsWith(".json")) {
                logSecurityViolation("IMPORTANT_FILE_DELETED", file.getName(),
                    "Important configuration file deleted");
            }
            
            fileHashes.remove(filePath);
            fileModificationTimes.remove(filePath);
        }
    }
    
    /**
     * Kiểm tra tính toàn vẹn của file
     */
    public boolean verifyFileIntegrity(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        String filePath = file.getAbsolutePath();
        String storedHash = fileHashes.get(filePath);
        
        if (storedHash == null) {
            // File chưa được monitor, tính hash mới
            String hash = calculateFileHash(file);
            if (hash != null) {
                fileHashes.put(filePath, hash);
                fileModificationTimes.put(filePath, file.lastModified());
            }
            return true;
        }

        if (isAutoUpdatingFile(file)) {
            updateFileHashSilently(file);
            return true;
        }

        
        // So sánh hash
        String currentHash = calculateFileHash(file);
        if (currentHash == null) {
            return false;
        }
        
        if (!currentHash.equals(storedHash)) {
            logSecurityViolation("FILE_INTEGRITY_VERIFICATION_FAILED", file.getName(),
                "File integrity check failed");
            return false;
        }
        
        return true;
    }
    
    /**
     * Tính SHA-256 hash của file
     */
    private String calculateFileHash(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | IOException e) {
            plugin.getLogger().warning("Error calculating file hash: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Chuyển byte array sang hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Lấy plugin JAR file
     */
    private File getPluginJarFile() {
        try {
            String path = plugin.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            return new File(path);
        } catch (Exception e) {
            plugin.getLogger().warning("Cannot get plugin JAR file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Ghi log vi phạm bảo mật
     */
    private void logSecurityViolation(String type, String fileName, String details) {
        String message = String.format("[FILE_INTEGRITY] [%s] File: %s - %s", 
            type, fileName, details);
        plugin.getLogger().severe(message);
    }

    private boolean isIgnoredFile(File file) {
        if (file == null) {
            return false;
        }
        return ignoredFileNames.contains(file.getName().toLowerCase());
    }

    private boolean isAutoUpdatingFile(File file) {
        if (file == null) {
            return false;
        }
        return autoUpdatingFileNames.contains(file.getName().toLowerCase());
    }

    private void updateFileHashSilently(File file) {
        String filePath = file.getAbsolutePath();
        String newHash = calculateFileHash(file);
        if (newHash != null) {
            fileHashes.put(filePath, newHash);
            fileModificationTimes.put(filePath, file.lastModified());
        }
    }
    
    /**
     * Dừng monitoring
     */
    public void stopMonitoring() {
        running = false;
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                plugin.getLogger().warning("Error closing watch service: " + e.getMessage());
            }
        }
        
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }
        
        plugin.getLogger().info("File integrity monitoring stopped");
    }
    
    /**
     * Lấy số lượng file đang monitor
     */
    public int getMonitoredFileCount() {
        return fileHashes.size();
    }
}
