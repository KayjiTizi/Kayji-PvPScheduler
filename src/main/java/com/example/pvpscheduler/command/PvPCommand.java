package com.example.pvpscheduler.command;

import com.example.pvpscheduler.manager.AdminMenuManager;
import com.example.pvpscheduler.manager.LeaderboardManager;
import com.example.pvpscheduler.manager.PvPManager;
import com.example.pvpscheduler.manager.QuestManager;
import com.example.pvpscheduler.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PvPCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PvPManager pvpManager;
    private final QuestManager questManager;
    private final LeaderboardManager leaderboardManager;
    private final AdminMenuManager adminMenuManager;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
    
    public PvPCommand(JavaPlugin plugin, PvPManager pvpManager,
                     QuestManager questManager, LeaderboardManager leaderboardManager,
                     AdminMenuManager adminMenuManager) {
        this.plugin = plugin;
        this.pvpManager = pvpManager;
        this.questManager = questManager;
        this.leaderboardManager = leaderboardManager;
        this.adminMenuManager = adminMenuManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "setstart":
                return handleSetStart(sender, args);
            case "setend":
                return handleSetEnd(sender, args);
            case "status":
                return handleStatus(sender);
            case "enable":
                return handleEnable(sender);
            case "disable":
                return handleDisable(sender);
            case "top":
                return handleTop(sender, args);
            case "stats":
                return handleStats(sender, args);
            case "quest":
                return handleQuest(sender, args);
            case "admin":
            case "menu":
                return handleAdminMenu(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                MessageUtil.sendError(sender, "Lệnh không hợp lệ! Sử dụng /pvp help để xem danh sách lệnh.");
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("pvpscheduler.reload")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        plugin.reloadConfig();
        pvpManager.loadConfig(plugin.getConfig());
        pvpManager.startScheduler();
        pvpManager.applyInitialPvPState();
        MessageUtil.sendSuccess(sender, "Đã tải lại cấu hình!");
        return true;
    }
    
    private boolean handleSetStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pvpscheduler.admin")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length != 2) {
            MessageUtil.sendError(sender, "Sử dụng: /pvp setstart <HH:mm>");
            return true;
        }
        
        try {
            LocalTime.parse(args[1], fmt);
            plugin.getConfig().set("start-time", args[1]);
            plugin.saveConfig();
            pvpManager.loadConfig(plugin.getConfig());
            pvpManager.startScheduler();
            pvpManager.applyInitialPvPState();
            MessageUtil.sendSuccess(sender, "Thời gian bắt đầu đã được đặt thành " + args[1]);
        } catch (Exception e) {
            MessageUtil.sendError(sender, "Định dạng thời gian không hợp lệ! Sử dụng HH:mm");
        }
        return true;
    }
    
    private boolean handleSetEnd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pvpscheduler.admin")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (args.length != 2) {
            MessageUtil.sendError(sender, "Sử dụng: /pvp setend <HH:mm>");
            return true;
        }
        
        try {
            LocalTime.parse(args[1], fmt);
            plugin.getConfig().set("end-time", args[1]);
            plugin.saveConfig();
            pvpManager.loadConfig(plugin.getConfig());
            pvpManager.startScheduler();
            pvpManager.applyInitialPvPState();
            MessageUtil.sendSuccess(sender, "Thời gian kết thúc đã được đặt thành " + args[1]);
        } catch (Exception e) {
            MessageUtil.sendError(sender, "Định dạng thời gian không hợp lệ! Sử dụng HH:mm");
        }
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        MessageUtil.sendInfo(sender, "Trạng thái PvP: " + (pvpManager.isPvPEnabled() ? "§aBẬT" : "§cTẮT"));
        return true;
    }
    
    private boolean handleEnable(CommandSender sender) {
        if (!sender.hasPermission("pvpscheduler.admin")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        pvpManager.setPvP(true);
        MessageUtil.sendSuccess(sender, "PvP đã được bật!");
        return true;
    }
    
    private boolean handleDisable(CommandSender sender) {
        if (!sender.hasPermission("pvpscheduler.admin")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        pvpManager.setPvP(false);
        MessageUtil.sendSuccess(sender, "PvP đã được tắt!");
        return true;
    }
    
    private boolean handleTop(CommandSender sender, String[] args) {
        String type = args.length > 1 ? args[1].toLowerCase() : "kills";
        int limit = args.length > 2 ? parseInt(args[2], 10) : 10;
        
        switch (type) {
            case "kills":
            case "kill":
                leaderboardManager.showTopKillers(sender, limit);
                break;
            case "kdr":
            case "kd":
                leaderboardManager.showTopKDR(sender, limit);
                break;
            case "streak":
            case "streaks":
                leaderboardManager.showTopKillStreaks(sender, limit);
                break;
            default:
                MessageUtil.sendError(sender, "Loại top không hợp lệ! Sử dụng: kills, kdr, streak");
                return true;
        }
        return true;
    }
    
    private boolean handleStats(CommandSender sender, String[] args) {
        UUID targetId;
        
        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendError(sender, "Người chơi không tìm thấy!");
                return true;
            }
            targetId = target.getUniqueId();
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendError(sender, "Bạn phải là người chơi hoặc chỉ định người chơi!");
                return true;
            }
            targetId = ((Player) sender).getUniqueId();
        }
        
        leaderboardManager.showPlayerStats(sender, targetId);
        return true;
    }
    
    private boolean handleQuest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Lệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        if (args.length > 1 && args[1].equalsIgnoreCase("assign")) {
            if (!sender.hasPermission("pvpscheduler.quest.assign")) {
                MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
                return true;
            }
            questManager.assignRandomQuest(playerId);
            MessageUtil.sendSuccess(sender, "Đã nhận nhiệm vụ ngẫu nhiên!");
            return true;
        }
        
        // Show player's quests
        var quests = questManager.getPlayerQuests(playerId);
        if (quests.isEmpty()) {
            MessageUtil.sendInfo(sender, "Bạn không có nhiệm vụ nào!");
            return true;
        }
        
        MessageUtil.sendInfo(sender, "=== NHIỆM VỤ CỦA BẠN ===");
        for (var activeQuest : quests) {
            var quest = activeQuest.getQuest();
            double progress = activeQuest.getProgressPercentage();
            String status = activeQuest.isCompleted() ? "§aHoàn thành" : "§eĐang làm";
            sender.sendMessage("§6" + quest.getName() + " §7- " + status);
            sender.sendMessage("§7" + quest.getDescription());
            sender.sendMessage("§7Tiến độ: §e" + activeQuest.getProgress() + "/" + quest.getTarget() + 
                    " §7(" + String.format("%.1f", progress) + "%)");
        }
        return true;
    }
    
    private boolean handleAdminMenu(CommandSender sender) {
        if (!sender.hasPermission("pvpscheduler.admin")) {
            MessageUtil.sendError(sender, "Bạn không có quyền sử dụng lệnh này!");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Lệnh này chỉ dành cho người chơi!");
            return true;
        }
        
        adminMenuManager.openAdminMenu((Player) sender);
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== PvPScheduler Commands ===");
        sender.sendMessage("§e/pvp help §7- Hiển thị trợ giúp");
        sender.sendMessage("§e/pvp status §7- Xem trạng thái PvP");
        sender.sendMessage("§e/pvp top <kills|kdr|streak> [limit] §7- Xem bảng xếp hạng");
        sender.sendMessage("§e/pvp stats [player] §7- Xem thống kê");
        sender.sendMessage("§e/pvp quest §7- Xem nhiệm vụ");
        if (sender.hasPermission("pvpscheduler.admin")) {
            sender.sendMessage("§c/pvp admin §7- Mở menu quản lý admin");
            sender.sendMessage("§c/pvp reload §7- Tải lại cấu hình");
            sender.sendMessage("§c/pvp setstart <HH:mm> §7- Đặt thời gian bắt đầu");
            sender.sendMessage("§c/pvp setend <HH:mm> §7- Đặt thời gian kết thúc");
            sender.sendMessage("§c/pvp enable §7- Bật PvP");
            sender.sendMessage("§c/pvp disable §7- Tắt PvP");
        }
    }
    
    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

