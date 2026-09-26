package com.example.pvpscheduler.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PvPCommandTabCompleter implements TabCompleter {
    
    private static final List<String> MAIN_COMMANDS = Arrays.asList(
        "help", "status", "top", "stats", "quest", "reload", 
        "setstart", "setend", "enable", "disable"
    );
    
    private static final List<String> TOP_TYPES = Arrays.asList(
        "kills", "kill", "kdr", "kd", "streak", "streaks"
    );
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - main commands
            StringUtil.copyPartialMatches(args[0], getAvailableCommands(sender), completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "top":
                    // Top types
                    StringUtil.copyPartialMatches(args[1], TOP_TYPES, completions);
                    break;
                case "stats":
                    // Player names
                    StringUtil.copyPartialMatches(args[1], 
                        Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()), 
                        completions);
                    break;
                case "quest":
                    // Quest subcommands
                    StringUtil.copyPartialMatches(args[1], Arrays.asList("assign"), completions);
                    break;
                case "setstart":
                case "setend":
                    // Time suggestions
                    StringUtil.copyPartialMatches(args[1], getTimeSuggestions(), completions);
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("top")) {
                // Limit numbers
                StringUtil.copyPartialMatches(args[2], 
                    Arrays.asList("5", "10", "15", "20", "25", "30"), 
                    completions);
            }
        }
        
        return completions;
    }
    
    private List<String> getAvailableCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>(MAIN_COMMANDS);
        
        // Remove admin commands if no permission
        if (!sender.hasPermission("pvpscheduler.admin")) {
            commands.remove("reload");
            commands.remove("setstart");
            commands.remove("setend");
            commands.remove("enable");
            commands.remove("disable");
        }
        
        if (!sender.hasPermission("pvpscheduler.reload")) {
            commands.remove("reload");
        }
        
        return commands;
    }
    
    private List<String> getTimeSuggestions() {
        List<String> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                times.add(String.format("%02d:%02d", hour, minute));
            }
        }
        return times;
    }
}

