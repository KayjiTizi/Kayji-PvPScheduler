package com.example.pvpscheduler.security;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * CodeIntegrityChecker - Kiểm tra tính toàn vẹn của code và phát hiện class/file lạ
 */
public class CodeIntegrityChecker {
    private final JavaPlugin plugin;
    private final Map<String, String> knownClassHashes = new ConcurrentHashMap<>();
    private final Set<String> allowedPackages = new HashSet<>();
    private final Set<String> blockedPackages = new HashSet<>();
    private final Set<String> suspiciousClassNames = new HashSet<>();
    private final Map<String, Long> classLoadTimes = new ConcurrentHashMap<>();
    
    private static final long MAX_CLASS_LOAD_TIME = 1000; // 1 second
    private static final String[] DANGEROUS_CLASS_PATTERNS = {
        "Runtime", "ProcessBuilder", "Process", "ClassLoader",
        "URLClassLoader", "FileInputStream", "FileOutputStream",
        "ScriptEngine", "GroovyShell", "JavaScript", "eval"
    };
    
    public CodeIntegrityChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeSecuritySettings();
        scanPluginJar();
    }
    
    private void initializeSecuritySettings() {
        // Cho phép các package của plugin
        allowedPackages.add("com.example.pvpscheduler");
        allowedPackages.add("org.bukkit");
        allowedPackages.add("org.spigotmc");
        
        // Chặn các package nguy hiểm
        blockedPackages.add("java.lang.reflect");
        blockedPackages.add("javax.script");
        blockedPackages.add("groovy");
        blockedPackages.add("org.codehaus.groovy");
        blockedPackages.add("org.mozilla.javascript");
        
        // Tên class đáng ngờ
        suspiciousClassNames.addAll(Arrays.asList(
            "Exploit", "Hack", "Backdoor", "Malware",
            "Virus", "Trojan", "Inject", "Bypass"
        ));
    }
    
    /**
     * Quét JAR file của plugin để lấy hash của các class
     */
    private void scanPluginJar() {
        try {
            String pluginPath = plugin.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            
            if (pluginPath == null) {
                plugin.getLogger().warning("Cannot find plugin JAR file for integrity check");
                return;
            }
            
            // Fix path for Windows (remove leading /)
            if (pluginPath.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
                pluginPath = pluginPath.substring(1);
            }
            
            File jarFile = new File(pluginPath);
            if (!jarFile.getName().endsWith(".jar")) {
                plugin.getLogger().warning("Plugin file is not a JAR: " + jarFile.getName());
                return;
            }
            
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace("/", ".").replace(".class", "");
                        
                        // Tính hash của class file từ JAR entry
                        try (java.io.InputStream is = jar.getInputStream(entry)) {
                            String hash = calculateSHA256(is);
                            if (hash != null) {
                                knownClassHashes.put(className, hash);
                            }
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Scanned " + knownClassHashes.size() + " classes for integrity check");
        } catch (Exception e) {
            plugin.getLogger().severe("Error scanning plugin JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Kiểm tra class có được phép load không
     */
    public boolean isClassAllowed(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        // Kiểm tra blocked packages
        for (String blocked : blockedPackages) {
            if (className.startsWith(blocked)) {
                logSecurityViolation("BLOCKED_PACKAGE", className, 
                    "Attempted to load class from blocked package: " + blocked);
                return false;
            }
        }
        
        // Kiểm tra suspicious class names
        String lowerClassName = className.toLowerCase();
        for (String suspicious : suspiciousClassNames) {
            if (lowerClassName.contains(suspicious.toLowerCase())) {
                logSecurityViolation("SUSPICIOUS_CLASS_NAME", className,
                    "Class name contains suspicious pattern: " + suspicious);
                return false;
            }
        }
        
        // Kiểm tra dangerous patterns
        for (String pattern : DANGEROUS_CLASS_PATTERNS) {
            if (className.contains(pattern)) {
                logSecurityViolation("DANGEROUS_CLASS_PATTERN", className,
                    "Class contains dangerous pattern: " + pattern);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Kiểm tra tính toàn vẹn của class đã load
     */
    public boolean verifyClassIntegrity(String className, byte[] classBytes) {
        if (className == null || classBytes == null) {
            return false;
        }
        
        // Tính hash của class bytes
        String currentHash = calculateSHA256(classBytes);
        
        // So sánh với hash đã biết
        String knownHash = knownClassHashes.get(className);
        if (knownHash != null && !knownHash.equals(currentHash)) {
            logSecurityViolation("CLASS_INTEGRITY_FAILED", className,
                "Class hash mismatch! Expected: " + knownHash + ", Got: " + currentHash);
            return false;
        }
        
        return true;
    }
    
    /**
     * Kiểm tra file có phải là file Java hợp lệ không
     */
    public boolean isValidJavaFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        
        // Chỉ cho phép file .class, .jar trong thư mục plugin
        if (fileName.endsWith(".class") || fileName.endsWith(".jar")) {
            // Kiểm tra file có nằm trong data folder của plugin không
            File dataFolder = plugin.getDataFolder();
            try {
                String filePath = file.getCanonicalPath();
                String dataPath = dataFolder.getCanonicalPath();
                
                // Không cho phép file .class hoặc .jar trong data folder
                if (filePath.startsWith(dataPath)) {
                    logSecurityViolation("INVALID_FILE_LOCATION", file.getName(),
                        "Java file found in data folder: " + filePath);
                    return false;
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Error checking file location: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    /**
     * Phát hiện class loading bất thường
     */
    public void recordClassLoad(String className) {
        if (className == null) return;
        
        long currentTime = System.currentTimeMillis();
        Long lastLoadTime = classLoadTimes.get(className);
        
        if (lastLoadTime != null) {
            long timeDiff = currentTime - lastLoadTime;
            if (timeDiff < MAX_CLASS_LOAD_TIME) {
                logSecurityViolation("RAPID_CLASS_LOAD", className,
                    "Class loaded too quickly after previous load: " + timeDiff + "ms");
            }
        }
        
        classLoadTimes.put(className, currentTime);
    }
    
    /**
     * Kiểm tra reflection calls đáng ngờ
     */
    public boolean isReflectionAllowed(String className, String methodName) {
        if (className == null || methodName == null) {
            return false;
        }
        
        // Chặn các method nguy hiểm
        String[] dangerousMethods = {
            "forName", "getDeclaredMethod", "getDeclaredField",
            "setAccessible", "invoke", "newInstance"
        };
        
        for (String dangerous : dangerousMethods) {
            if (methodName.contains(dangerous)) {
                // Chỉ cho phép trong package của plugin
                if (!className.startsWith("com.example.pvpscheduler")) {
                    logSecurityViolation("DANGEROUS_REFLECTION", className + "." + methodName,
                        "Attempted dangerous reflection call");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Tính SHA-256 hash của input stream
     */
    private String calculateSHA256(java.io.InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            plugin.getLogger().warning("SHA-256 algorithm not available: " + e.getMessage());
            return null;
        } catch (IOException e) {
            plugin.getLogger().warning("Error reading stream for hash calculation: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Tính SHA-256 hash của byte array
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            plugin.getLogger().warning("SHA-256 algorithm not available: " + e.getMessage());
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
     * Ghi log vi phạm bảo mật
     */
    private void logSecurityViolation(String type, String className, String details) {
        String message = String.format("[CODE_INTEGRITY] [%s] Class: %s - %s", 
            type, className, details);
        plugin.getLogger().severe(message);
        
        // Có thể gửi thông báo cho admin ở đây
    }
    
    /**
     * Thêm package vào whitelist
     */
    public void addAllowedPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            allowedPackages.add(packageName);
        }
    }
    
    /**
     * Thêm package vào blacklist
     */
    public void addBlockedPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            blockedPackages.add(packageName);
        }
    }
    
    /**
     * Lấy số lượng class đã scan
     */
    public int getScannedClassCount() {
        return knownClassHashes.size();
    }
}

