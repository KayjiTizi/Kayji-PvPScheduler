package com.example.pvpscheduler.security;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassLoaderProtection - Bảo vệ class loader khỏi load class lạ
 */
public class ClassLoaderProtection {
    private final JavaPlugin plugin;
    private final CodeIntegrityChecker integrityChecker;
    private final Set<String> allowedClassLoaders = new HashSet<>();
    private final Map<String, Integer> classLoadAttempts = new ConcurrentHashMap<>();
    private final Set<String> blockedClasses = new HashSet<>();
    
    private static final int MAX_LOAD_ATTEMPTS = 5;
    
    public ClassLoaderProtection(JavaPlugin plugin, CodeIntegrityChecker integrityChecker) {
        this.plugin = plugin;
        this.integrityChecker = integrityChecker;
        initializeProtection();
    }
    
    private void initializeProtection() {
        // Cho phép các class loader hợp lệ
        allowedClassLoaders.add("org.bukkit.plugin.java.PluginClassLoader");
        allowedClassLoaders.add("java.net.URLClassLoader");
        allowedClassLoaders.add("sun.misc.Launcher$AppClassLoader");
        
        // Chặn các class nguy hiểm
        blockedClasses.addAll(Arrays.asList(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Process",
            "java.lang.reflect.Method",
            "javax.script.ScriptEngineManager",
            "groovy.lang.GroovyShell"
        ));
    }
    
    public void checkPackageAccess(String pkg) {
        if (pkg == null) {
            return;
        }
        
        // Kiểm tra package có bị chặn không
        if (!integrityChecker.isClassAllowed(pkg)) {
            logSecurityViolation("BLOCKED_PACKAGE_ACCESS", pkg,
                "Attempted to access blocked package");
            throw new SecurityException("Package access denied: " + pkg);
        }
    }
    
    /**
     * Kiểm tra class loader có hợp lệ không
     */
    public boolean isValidClassLoader(ClassLoader loader) {
        if (loader == null) {
            return false;
        }
        
        String loaderName = loader.getClass().getName();
        
        // Kiểm tra class loader có trong whitelist không
        for (String allowed : allowedClassLoaders) {
            if (loaderName.contains(allowed)) {
                return true;
            }
        }
        
        // Kiểm tra URLClassLoader có load từ nguồn không đáng tin không
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader) loader;
            URL[] urls = urlLoader.getURLs();
            
            for (URL url : urls) {
                String urlString = url.toString();
                
                // Chặn load từ file system ngoài plugin folder
                if (urlString.startsWith("file:")) {
                    File pluginFolder = plugin.getDataFolder().getParentFile();
                    try {
                        File urlFile = new File(url.toURI());
                        if (!urlFile.getCanonicalPath().startsWith(
                            pluginFolder.getCanonicalPath())) {
                            logSecurityViolation("INVALID_CLASSLOADER_SOURCE", urlString,
                                "ClassLoader attempting to load from external location");
                            return false;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error checking ClassLoader URL: " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        
        logSecurityViolation("UNKNOWN_CLASSLOADER", loaderName,
            "Unknown or suspicious ClassLoader detected");
        return false;
    }
    
    /**
     * Kiểm tra class có được phép load không
     */
    public boolean canLoadClass(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        // Kiểm tra class có bị chặn không
        if (blockedClasses.contains(className)) {
            logSecurityViolation("BLOCKED_CLASS_LOAD", className,
                "Attempted to load blocked class");
            return false;
        }
        
        // Kiểm tra số lần cố gắng load
        int attempts = classLoadAttempts.getOrDefault(className, 0);
        if (attempts >= MAX_LOAD_ATTEMPTS) {
            logSecurityViolation("EXCESSIVE_CLASS_LOAD_ATTEMPTS", className,
                "Too many load attempts: " + attempts);
            return false;
        }
        
        // Kiểm tra tính toàn vẹn
        if (!integrityChecker.isClassAllowed(className)) {
            classLoadAttempts.put(className, attempts + 1);
            return false;
        }
        
        return true;
    }
    
    /**
     * Ghi lại việc load class thành công
     */
    public void recordSuccessfulClassLoad(String className) {
        if (className != null) {
            classLoadAttempts.remove(className);
            integrityChecker.recordClassLoad(className);
        }
    }
    
    /**
     * Kiểm tra file có được phép load như class không
     */
    public boolean canLoadFileAsClass(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        // Chỉ cho phép file .class
        if (!file.getName().endsWith(".class")) {
            logSecurityViolation("INVALID_CLASS_FILE", file.getName(),
                "Attempted to load non-class file as class");
            return false;
        }
        
        // Kiểm tra tính hợp lệ của file
        if (!integrityChecker.isValidJavaFile(file)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Ghi log vi phạm bảo mật
     */
    private void logSecurityViolation(String type, String className, String details) {
        String message = String.format("[CLASSLOADER_PROTECTION] [%s] %s - %s", 
            type, className, details);
        plugin.getLogger().severe(message);
    }
    
    /**
     * Thêm class vào blacklist
     */
    public void addBlockedClass(String className) {
        if (className != null && !className.isEmpty()) {
            blockedClasses.add(className);
        }
    }
    
    /**
     * Xóa class khỏi blacklist
     */
    public void removeBlockedClass(String className) {
        if (className != null) {
            blockedClasses.remove(className);
        }
    }
    
    /**
     * Reset attempt counter cho class
     */
    public void resetClassLoadAttempts(String className) {
        if (className != null) {
            classLoadAttempts.remove(className);
        }
    }
}

