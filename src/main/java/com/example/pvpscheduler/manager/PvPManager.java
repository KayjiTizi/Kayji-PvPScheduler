package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.model.Quest;
import com.example.pvpscheduler.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {
    private final JavaPlugin plugin;
    private final StatsManager statsManager;
    private final QuestManager questManager;
    private RewardManager rewardManager;
    private AntiCheatManager antiCheatManager;
    private AnimationManager animationManager;
    private AdminMenuManager adminMenuManager;
    
    private LocalTime startTime;
    private LocalTime endTime;
    private int notifyBefore;
    private Set<DayOfWeek> enabledDays = new HashSet<>();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<UUID, Integer> sessionKills = new ConcurrentHashMap<>();
    private BukkitRunnable schedulerTask;
    private BukkitRunnable countdownTask;
    private boolean currentPvP = false;
    private final Set<Integer> notifiedCountdowns = ConcurrentHashMap.newKeySet();
    
    public PvPManager(JavaPlugin plugin, StatsManager statsManager, QuestManager questManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.questManager = questManager;
    }
    
    public void setManagers(RewardManager rewardManager, AntiCheatManager antiCheatManager,
                           AnimationManager animationManager) {
        this.rewardManager = rewardManager;
        this.antiCheatManager = antiCheatManager;
        this.animationManager = animationManager;
    }
    
    public void setAdminMenuManager(AdminMenuManager adminMenuManager) {
        this.adminMenuManager = adminMenuManager;
    }
    
    public void loadConfig(FileConfiguration config) {
        startTime = LocalTime.parse(config.getString("start-time", "18:00"), fmt);
        endTime = LocalTime.parse(config.getString("end-time", "06:00"), fmt);
        notifyBefore = config.getInt("notify-before", 30);
        
        // Load enabled days
        enabledDays.clear();
        List<String> days = config.getStringList("enabled-days");
        if (days.isEmpty()) {
            // Default: all days
            enabledDays.addAll(Arrays.asList(DayOfWeek.values()));
        } else {
            for (String day : days) {
                try {
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    enabledDays.add(dayOfWeek);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ngày không hợp lệ trong config: " + day);
                }
            }
        }
    }
    
    public void startScheduler() {
        if (schedulerTask != null) schedulerTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                DayOfWeek currentDay = java.time.LocalDate.now().getDayOfWeek();
                
                // Kiểm tra xem hôm nay có được bật PvP không
                if (!enabledDays.contains(currentDay)) {
                    if (currentPvP) {
                        setPvP(false);
                    }
                    return;
                }
                
                boolean shouldBeEnabled = computeShouldBeEnabled(now);
                
                // Toggle PvP when crossing threshold
                if (shouldBeEnabled && !currentPvP) {
                    setPvP(true);
                } else if (!shouldBeEnabled && currentPvP) {
                    setPvP(false);
                }
            }
        };
        schedulerTask.runTaskTimer(plugin, 0L, 20L);
        
        // Start countdown task
        startCountdownTask();
    }
    
    private void startCountdownTask() {
        if (countdownTask != null) countdownTask.cancel();
        
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                DayOfWeek currentDay = java.time.LocalDate.now().getDayOfWeek();
                
                if (!enabledDays.contains(currentDay)) return;
                
                // Countdown trước khi bật
                long secsToStart = getSecondsUntil(startTime);
                if (secsToStart > 0 && secsToStart <= notifyBefore && !currentPvP) {
                    showCountdown(secsToStart, true);
                }
                
                // Countdown trước khi tắt
                long secsToEnd = getSecondsUntil(endTime);
                if (secsToEnd > 0 && secsToEnd <= notifyBefore && currentPvP) {
                    showCountdown(secsToEnd, false);
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    private long getSecondsUntil(LocalTime targetTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(targetTime)) {
            return Duration.between(now, targetTime).getSeconds();
        } else {
            // Nếu đã qua thời gian hôm nay, tính cho ngày mai
            return Duration.between(now, LocalTime.MAX).getSeconds() + 
                   Duration.between(LocalTime.MIN, targetTime).getSeconds() + 1;
        }
    }
    
    private void showCountdown(long seconds, boolean isStart) {
        boolean broadcastEnabled = adminMenuManager == null || adminMenuManager.isBroadcastEnabled();
        
        int[] countdownPoints = {60, 30, 10, 5, 3, 2, 1};
        
        for (int point : countdownPoints) {
            if (seconds == point && !notifiedCountdowns.contains(point)) {
                if (broadcastEnabled) {
                    if (isStart) {
                        MessageUtil.broadcast("§6[⚔ PvP] §ePvP sẽ bắt đầu sau §c§l" + point + " §egiây!");
                    } else {
                        MessageUtil.broadcast("§c[⚔ PvP] §ePvP sẽ kết thúc sau §c§l" + point + " §egiây!");
                    }
                    
                    // Title cho người chơi
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (isStart) {
                            player.sendTitle("§6§l⚔ PvP SẮP BẮT ĐẦU ⚔", "§eCòn §c§l" + point + " §egiây", 5, 40, 10);
                        } else {
                            player.sendTitle("§c§l⚔ PvP SẮP KẾT THÚC ⚔", "§eCòn §c§l" + point + " §egiây", 5, 40, 10);
                        }
                    }
                }
                
                notifiedCountdowns.add(point);
                break;
            }
        }
        
        // Reset notifications khi đã qua
        if (seconds == 0) {
            notifiedCountdowns.clear();
        }
    }
    
    private boolean computeShouldBeEnabled(LocalTime now) {
        if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
            return !now.isBefore(startTime) && now.isBefore(endTime);
        } else {
            return now.isAfter(startTime) || now.isBefore(endTime);
        }
    }
    
    public void applyInitialPvPState() {
        LocalTime now = LocalTime.now();
        boolean shouldBeEnabled = computeShouldBeEnabled(now);
        setPvP(shouldBeEnabled);
    }
    
    public void setPvP(boolean enable) {
        // Check if PvP system is enabled
        if (adminMenuManager != null && !adminMenuManager.isPvPSystemEnabled()) {
            enable = false;
        }
        
        for (World world : Bukkit.getWorlds()) {
            world.setPVP(enable);
        }
        currentPvP = enable;
        notifiedCountdowns.clear();
        
        boolean broadcastEnabled = adminMenuManager == null || adminMenuManager.isBroadcastEnabled();
        
        if (enable) {
            if (broadcastEnabled) {
                // Thông báo chat đẹp hơn
                MessageUtil.broadcast("");
                MessageUtil.broadcast("§6╔════════════════════════════════════╗");
                MessageUtil.broadcast("§6║  §e⚔ §6§lPvP ĐÃ ĐƯỢC BẬT! §e⚔ §6║");
                MessageUtil.broadcast("§6╚════════════════════════════════════╝");
                MessageUtil.broadcast("");
                
                // Title cho tất cả người chơi
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle("§6§l⚔ PvP ĐÃ BẬT ⚔", "§eHãy cẩn thận khi ra ngoài!", 10, 60, 20);
                }
            }
            sessionKills.clear();
            if (antiCheatManager != null && (adminMenuManager == null || adminMenuManager.isAntiCheatEnabled())) {
                antiCheatManager.resetAllActivities();
            }
        } else {
            if (broadcastEnabled) {
                // Thông báo chat đẹp hơn
                MessageUtil.broadcast("");
                MessageUtil.broadcast("§c╔════════════════════════════════════╗");
                MessageUtil.broadcast("§c║  §7⚔ §c§lPvP ĐÃ ĐƯỢC TẮT! §7⚔ §c║");
                MessageUtil.broadcast("§c╚════════════════════════════════════╝");
                MessageUtil.broadcast("");
                
                // Title cho tất cả người chơi
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle("§c§l⚔ PvP ĐÃ TẮT ⚔", "§7Bạn đã an toàn!", 10, 60, 20);
                }
            }
            
            // Công bố top và trao thưởng
            announceTopAndReward();
            sessionKills.clear();
        }
    }
    
    private void announceTopAndReward() {
        if (sessionKills.isEmpty()) {
            if (adminMenuManager == null || adminMenuManager.isBroadcastEnabled()) {
                MessageUtil.broadcast("§7Không có kill nào được ghi nhận trong phiên này.");
            }
            return;
        }
        
        boolean broadcastEnabled = adminMenuManager == null || adminMenuManager.isBroadcastEnabled();
        boolean leaderboardEnabled = adminMenuManager == null || adminMenuManager.isLeaderboardEnabled();
        
        // Sắp xếp theo kills
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(sessionKills.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Công bố top
        if (broadcastEnabled && leaderboardEnabled) {
            MessageUtil.broadcast("§6§l═══════════════════════════");
            MessageUtil.broadcast("§6§l     BẢNG XẾP HẠNG PvP");
            MessageUtil.broadcast("§6§l═══════════════════════════");
            
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : sorted) {
                if (rank > 10) break; // Chỉ hiển thị top 10
                
                UUID playerId = entry.getKey();
                int kills = entry.getValue();
                String name = statsManager.getPlayerName(playerId);
                
                String rankColor = getRankColor(rank);
                MessageUtil.broadcast(rankColor + rank + ". §f" + name + " §7- §c" + kills + " kills");
                rank++;
            }
            
            MessageUtil.broadcast("§6§l═══════════════════════════");
        }
        
        // Trao thưởng cho top 1
        if (!sorted.isEmpty() && rewardManager != null && animationManager != null) {
            // Check if reward and animation systems are enabled
            if (adminMenuManager != null && 
                (!adminMenuManager.isRewardSystemEnabled() || !adminMenuManager.isAnimationEnabled())) {
                return;
            }
            
            UUID top1Id = sorted.get(0).getKey();
            Player top1Player = Bukkit.getPlayer(top1Id);
            
            if (top1Player != null && top1Player.isOnline()) {
                // Kiểm tra anti-cheat
                boolean isLegitimate = antiCheatManager == null || 
                                      (adminMenuManager != null && !adminMenuManager.isAntiCheatEnabled()) ||
                                      antiCheatManager.isPlayerLegitimate(top1Id);
                
                if (isLegitimate) {
                    // Quay thưởng
                    RewardManager.Reward reward = rewardManager.spinReward(true);
                    
                    // Hiệu ứng animation
                    if (adminMenuManager == null || adminMenuManager.isAnimationEnabled()) {
                        animationManager.playTop1RewardAnimation(top1Player);
                    }
                    
                    // Delay một chút rồi quay thưởng
                    if (adminMenuManager == null || adminMenuManager.isAnimationEnabled()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                animationManager.playRewardSpinAnimation(top1Player, reward);
                            }
                        }.runTaskLater(plugin, 100L); // 5 giây
                    }
                    
                    if (adminMenuManager == null || adminMenuManager.isBroadcastEnabled()) {
                        MessageUtil.broadcast("§6§l🎉 " + top1Player.getName() + " §6§lđã đạt TOP 1 và nhận được phần thưởng đặc biệt! 🎉");
                    }
                } else {
                    if (adminMenuManager == null || adminMenuManager.isBroadcastEnabled()) {
                        MessageUtil.broadcast("§c§l⚠ " + top1Player.getName() + " bị nghi ngờ gian lận, không nhận được phần thưởng!");
                    }
                }
            }
        }
    }
    
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6§l"; // Gold
            case 2 -> "§7§l"; // Silver
            case 3 -> "§c§l"; // Bronze
            default -> "§e"; // Yellow
        };
    }
    
    public void onPlayerKill(UUID killerId, UUID victimId) {
        if (!currentPvP) return;
        
        // Check if PvP system is enabled
        if (adminMenuManager != null && !adminMenuManager.isPvPSystemEnabled()) {
            return;
        }
        
        // Update anti-cheat
        if (antiCheatManager != null && (adminMenuManager == null || adminMenuManager.isAntiCheatEnabled())) {
            antiCheatManager.onPlayerKill(killerId, victimId);
        }
        
        // Update session kills
        sessionKills.put(killerId, sessionKills.getOrDefault(killerId, 0) + 1);
        
        // Update stats
        if (adminMenuManager == null || adminMenuManager.isStatsSystemEnabled()) {
            statsManager.addKill(killerId);
            statsManager.addDeath(victimId);
        }
        
        // Update quests
        if (adminMenuManager == null || adminMenuManager.isQuestSystemEnabled()) {
            questManager.updateQuestProgress(killerId, Quest.QuestType.KILL_PLAYERS, 1);
            
            // Update kill streak quest
            com.example.pvpscheduler.model.PlayerStats stats = statsManager.getStats(killerId);
            questManager.updateKillStreakQuest(killerId, stats.getKillStreak());
        }
    }
    
    public boolean isPvPEnabled() {
        return currentPvP;
    }
    
    public Map<UUID, Integer> getSessionKills() {
        return sessionKills;
    }
    
    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }
    
    public Set<DayOfWeek> getEnabledDays() {
        return new HashSet<>(enabledDays);
    }
    
    public void cleanupPlayerData(UUID playerId) {
        try {
            if (playerId != null) {
                sessionKills.remove(playerId);
                // Cleanup anti-cheat data if available
                if (antiCheatManager != null) {
                    antiCheatManager.resetPlayerActivity(playerId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up player data for " + playerId + ": " + e.getMessage());
        }
    }
}

