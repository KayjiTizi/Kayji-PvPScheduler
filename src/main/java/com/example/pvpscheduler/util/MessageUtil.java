package com.example.pvpscheduler.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtil {
    public static final String PREFIX = ChatColor.YELLOW + "[PvPScheduler] " + ChatColor.RESET;
    
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }
    
    public static void sendSuccess(CommandSender sender, String message) {
        sendMessage(sender, ChatColor.GREEN + message);
    }
    
    public static void sendError(CommandSender sender, String message) {
        sendMessage(sender, ChatColor.RED + message);
    }
    
    public static void sendInfo(CommandSender sender, String message) {
        sendMessage(sender, ChatColor.AQUA + message);
    }
    
    public static void broadcast(String message) {
        org.bukkit.Bukkit.broadcastMessage(PREFIX + message);
    }
    
    public static void broadcastSuccess(String message) {
        broadcast(ChatColor.GREEN + message);
    }
    
    public static void broadcastError(String message) {
        broadcast(ChatColor.RED + message);
    }
    
    public static String formatNumber(int number) {
        return ChatColor.GOLD + String.valueOf(number) + ChatColor.RESET;
    }
    
    public static String formatPlayer(String playerName) {
        return ChatColor.YELLOW + playerName + ChatColor.RESET;
    }
}

