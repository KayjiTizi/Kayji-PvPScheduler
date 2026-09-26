package com.example.pvpscheduler.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConfigUtil {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public static LocalTime getTime(FileConfiguration config, String path, LocalTime defaultValue) {
        String timeStr = config.getString(path);
        if (timeStr == null) return defaultValue;
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public static List<String> getStringList(FileConfiguration config, String path, List<String> defaultValue) {
        List<String> list = config.getStringList(path);
        return list.isEmpty() ? defaultValue : list;
    }
    
    public static int getInt(FileConfiguration config, String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    public static boolean getBoolean(FileConfiguration config, String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public static String getString(FileConfiguration config, String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
}

