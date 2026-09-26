package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.model.PlayerStats;
import com.example.pvpscheduler.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardManager {
    private final StatsManager statsManager;
    
    public LeaderboardManager(StatsManager statsManager) {
        this.statsManager = statsManager;
    }
    
    public void showTopKillers(CommandSender sender, int limit) {
        List<Map.Entry<UUID, PlayerStats>> topKillers = statsManager.getTopKillers(limit);
        
        sender.sendMessage(MessageUtil.PREFIX + "§6=== TOP KILLERS ===");
        if (topKillers.isEmpty()) {
            sender.sendMessage("§7Không có dữ liệu.");
            return;
        }
        
        int rank = 1;
        for (Map.Entry<UUID, PlayerStats> entry : topKillers) {
            String name = statsManager.getPlayerName(entry.getKey());
            int kills = entry.getValue().getKills();
            sender.sendMessage("§e" + rank + ". §f" + name + " §7- §c" + kills + " kills");
            rank++;
        }
    }
    
    public void showTopKDR(CommandSender sender, int limit) {
        List<Map.Entry<UUID, PlayerStats>> topKDR = statsManager.getTopKDR(limit);
        
        sender.sendMessage(MessageUtil.PREFIX + "§6=== TOP K/D RATIO ===");
        if (topKDR.isEmpty()) {
            sender.sendMessage("§7Không có dữ liệu.");
            return;
        }
        
        int rank = 1;
        for (Map.Entry<UUID, PlayerStats> entry : topKDR) {
            String name = statsManager.getPlayerName(entry.getKey());
            double kdr = entry.getValue().getKDR();
            sender.sendMessage("§e" + rank + ". §f" + name + " §7- §a" + String.format("%.2f", kdr) + " K/D");
            rank++;
        }
    }
    
    public void showTopKillStreaks(CommandSender sender, int limit) {
        List<Map.Entry<UUID, PlayerStats>> topStreaks = statsManager.getTopKillStreaks(limit);
        
        sender.sendMessage(MessageUtil.PREFIX + "§6=== TOP KILL STREAKS ===");
        if (topStreaks.isEmpty()) {
            sender.sendMessage("§7Không có dữ liệu.");
            return;
        }
        
        int rank = 1;
        for (Map.Entry<UUID, PlayerStats> entry : topStreaks) {
            String name = statsManager.getPlayerName(entry.getKey());
            int streak = entry.getValue().getBestKillStreak();
            sender.sendMessage("§e" + rank + ". §f" + name + " §7- §d" + streak + " streak");
            rank++;
        }
    }
    
    public void showPlayerStats(CommandSender sender, UUID playerId) {
        PlayerStats stats = statsManager.getStats(playerId);
        String name = statsManager.getPlayerName(playerId);
        
        sender.sendMessage(MessageUtil.PREFIX + "§6=== THỐNG KÊ: §f" + name + " §6===");
        sender.sendMessage("§7Kills: §c" + stats.getKills());
        sender.sendMessage("§7Deaths: §c" + stats.getDeaths());
        sender.sendMessage("§7K/D Ratio: §a" + String.format("%.2f", stats.getKDR()));
        sender.sendMessage("§7Kill Streak: §d" + stats.getKillStreak());
        sender.sendMessage("§7Best Kill Streak: §d" + stats.getBestKillStreak());
    }
}

