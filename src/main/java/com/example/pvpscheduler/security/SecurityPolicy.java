package com.example.pvpscheduler.security;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SecurityPolicy - Chính sách bảo mật tổng hợp
 */
public class SecurityPolicy {
    private final JavaPlugin plugin;
    private final CodeIntegrityChecker codeChecker;
    private final ClassLoaderProtection classLoaderProtection;
    private final FileIntegrityManager fileIntegrityManager;
    
    private final Set<String> whitelistedClasses = new HashSet<>();
    private final Set<String> blacklistedClasses = new HashSet<>();
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private final List<String> securityLog = new ArrayList<>();
    
    private boolean strictMode = true;
    private int maxViolations = 10;
    
    public SecurityPolicy(JavaPlugin plugin) {
        this.plugin = plugin;
        this.codeChecker = new CodeIntegrityChecker(plugin);
        this.classLoaderProtection = new ClassLoaderProtection(plugin, codeChecker);
        this.fileIntegrityManager = new FileIntegrityManager(plugin);
        
        initializePolicy();
    }
    
    private void initializePolicy() {
        // Whitelist các class cần thiết
        whitelistedClasses.addAll(Arrays.asList(
            "com.example.pvpscheduler.PvPScheduler",
            "com.example.pvpscheduler.manager.PvPManager",
            "com.example.pvpscheduler.manager.StatsManager",
            "org.bukkit.plugin.java.JavaPlugin"
        ));
        
        // Blacklist các class nguy hiểm
        blacklistedClasses.addAll(Arrays.asList(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Process",
            "javax.script.ScriptEngineManager",
            "groovy.lang.GroovyShell"
        ));
    }
    
    /**
     * Khởi động tất cả các hệ thống bảo mật
     */
    public void enable() {
        try {
            // Không cài đặt SecurityManager vì có thể xung đột với Bukkit
            // Thay vào đó, chúng ta sẽ kiểm tra ở mức class loading và file access
            
            // Bắt đầu file monitoring
            fileIntegrityManager.startMonitoring();
            
            plugin.getLogger().info("Security policy enabled with " + 
                codeChecker.getScannedClassCount() + " classes scanned");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable security policy: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tắt các hệ thống bảo mật
     */
    public void disable() {
        try {
            fileIntegrityManager.stopMonitoring();
            plugin.getLogger().info("Security policy disabled");
        } catch (Exception e) {
            plugin.getLogger().warning("Error disabling security policy: " + e.getMessage());
        }
    }
    
    /**
     * Kiểm tra class có được phép load không
     */
    public boolean isClassAllowed(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        // Kiểm tra whitelist
        if (whitelistedClasses.contains(className)) {
            return true;
        }
        
        // Kiểm tra blacklist
        if (blacklistedClasses.contains(className)) {
            recordViolation(className, "BLACKLISTED_CLASS");
            return false;
        }
        
        // Kiểm tra code integrity
        if (!codeChecker.isClassAllowed(className)) {
            recordViolation(className, "CODE_INTEGRITY_FAILED");
            return false;
        }
        
        // Kiểm tra class loader protection
        if (!classLoaderProtection.canLoadClass(className)) {
            recordViolation(className, "CLASSLOADER_BLOCKED");
            return false;
        }
        
        return true;
    }
    
    /**
     * Kiểm tra file có được phép load không
     */
    public boolean isFileAllowed(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        // Kiểm tra file integrity
        if (!fileIntegrityManager.verifyFileIntegrity(file)) {
            recordViolation(file.getName(), "FILE_INTEGRITY_FAILED");
            return false;
        }
        
        // Kiểm tra class loader protection
        if (!classLoaderProtection.canLoadFileAsClass(file)) {
            recordViolation(file.getName(), "FILE_BLOCKED");
            return false;
        }
        
        return true;
    }
    
    /**
     * Kiểm tra reflection call có được phép không
     */
    public boolean isReflectionAllowed(String className, String methodName) {
        if (className == null || methodName == null) {
            return false;
        }
        
        if (!codeChecker.isReflectionAllowed(className, methodName)) {
            recordViolation(className + "." + methodName, "REFLECTION_BLOCKED");
            return false;
        }
        
        return true;
    }
    
    /**
     * Ghi lại vi phạm bảo mật
     */
    private void recordViolation(String target, String type) {
        String key = target + ":" + type;
        int count = violationCounts.getOrDefault(key, 0) + 1;
        violationCounts.put(key, count);
        
        String logEntry = String.format("[%s] %s - %s (Count: %d)", 
            new Date(), type, target, count);
        securityLog.add(logEntry);
        
        // Giới hạn log size
        if (securityLog.size() > 1000) {
            securityLog.remove(0);
        }
        
        // Nếu vi phạm quá nhiều, log cảnh báo
        if (count >= maxViolations) {
            plugin.getLogger().severe("SECURITY ALERT: Excessive violations for " + target);
        }
    }
    
    /**
     * Lấy danh sách vi phạm
     */
    public List<String> getSecurityLog() {
        return new ArrayList<>(securityLog);
    }
    
    /**
     * Lấy số lượng vi phạm
     */
    public int getViolationCount(String target) {
        return violationCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }
    
    /**
     * Thêm class vào whitelist
     */
    public void addWhitelistedClass(String className) {
        if (className != null && !className.isEmpty()) {
            whitelistedClasses.add(className);
        }
    }
    
    /**
     * Thêm class vào blacklist
     */
    public void addBlacklistedClass(String className) {
        if (className != null && !className.isEmpty()) {
            blacklistedClasses.add(className);
            classLoaderProtection.addBlockedClass(className);
        }
    }
    
    /**
     * Bật/tắt strict mode
     */
    public void setStrictMode(boolean strict) {
        this.strictMode = strict;
    }
    
    /**
     * Kiểm tra strict mode
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    /**
     * Cleanup
     */
    public void cleanup() {
        disable();
        violationCounts.clear();
        securityLog.clear();
    }
    
    // Getters
    public CodeIntegrityChecker getCodeChecker() {
        return codeChecker;
    }
    
    public ClassLoaderProtection getClassLoaderProtection() {
        return classLoaderProtection;
    }
    
    public FileIntegrityManager getFileIntegrityManager() {
        return fileIntegrityManager;
    }
}

