package com.example.pvpscheduler;

import com.example.pvpscheduler.command.PvPCommand;
import com.example.pvpscheduler.command.PvPCommandTabCompleter;
import com.example.pvpscheduler.listener.PvPListener;
import com.example.pvpscheduler.listener.SecurityListener;
import com.example.pvpscheduler.manager.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PvPScheduler extends JavaPlugin {
    private PvPManager pvpManager;
    private StatsManager statsManager;
    private QuestManager questManager;
    private LeaderboardManager leaderboardManager;
    private RewardManager rewardManager;
    private AntiCheatManager antiCheatManager;
    private AnimationManager animationManager;
    private AdminMenuManager adminMenuManager;
    
    // New managers
    private com.example.pvpscheduler.manager.SecurityManager securityManager;
    private ActivityLogger activityLogger;
    private PerformanceMonitor performanceMonitor;
    // DISABLED: BackupManager backup feature disabled to prevent lag
    // private BackupManager backupManager;
    private EventTracker eventTracker;
    private AuditLog auditLog;
    private NotificationManager notificationManager;
    private RateLimiter rateLimiter;
    private SessionManager sessionManager;
    
    private BukkitRunnable autoSaveTask;
    private BukkitRunnable questCleanupTask;
    
    @Override
    public void onEnable() {
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize core managers
            statsManager = new StatsManager(this);
            questManager = new QuestManager(this);
            leaderboardManager = new LeaderboardManager(statsManager);
            rewardManager = new RewardManager(this);
            antiCheatManager = new AntiCheatManager(this);
            animationManager = new AnimationManager(this);
            pvpManager = new PvPManager(this, statsManager, questManager);
        
        // Initialize new security and monitoring managers
        // SecurityManager phải được khởi tạo đầu tiên để bảo vệ các manager khác
        securityManager = new com.example.pvpscheduler.manager.SecurityManager(this);
        activityLogger = new ActivityLogger(this);
        performanceMonitor = new PerformanceMonitor(this);
        // DISABLED: BackupManager backup feature disabled to prevent lag
        // backupManager = new BackupManager(this);
        eventTracker = new EventTracker(this);
        auditLog = new AuditLog(this);
        notificationManager = new NotificationManager(this);
        rateLimiter = new RateLimiter(this);
        sessionManager = new SessionManager();
        
        // Set managers for PvPManager
        pvpManager.setManagers(rewardManager, antiCheatManager, animationManager);
        
        // Initialize AdminMenuManager
        adminMenuManager = new AdminMenuManager(this, this, pvpManager);
        
        // Set AdminMenuManager to PvPManager
        pvpManager.setAdminMenuManager(adminMenuManager);
        
        // Load config
        pvpManager.loadConfig(getConfig());
        
        // Register listeners
        getServer().getPluginManager().registerEvents(
            new PvPListener(pvpManager, statsManager, questManager,
                sessionManager, eventTracker),
            this
        );
        getServer().getPluginManager().registerEvents(activityLogger, this);
        getServer().getPluginManager().registerEvents(securityManager, this);
        getServer().getPluginManager().registerEvents(
            new SecurityListener(securityManager), this);
        
        // DISABLED: Backup system disabled to prevent lag
        // if (getConfig().getBoolean("backup.enabled", false)) {
        //     backupManager.startAutoBackup();
        // }
        
        // Register commands
        try {
            PluginCommand command = getCommand("pvp");
            if (command != null) {
                command.setExecutor(
                    new PvPCommand(this, pvpManager, questManager, leaderboardManager, adminMenuManager)
                );
                command.setTabCompleter(new PvPCommandTabCompleter());
            } else {
                getLogger().severe("Command 'pvp' not found in plugin.yml! Plugin may not work correctly.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Start scheduler
        pvpManager.startScheduler();
        pvpManager.applyInitialPvPState();
        
        // Start auto-save task
        startAutoSave();
        
        // Start quest cleanup task
        startQuestCleanup();
        
            getLogger().info("PvPScheduler v2.0 đã được kích hoạt!");
            getLogger().info("Hệ thống PvP, Thống kê, Nhiệm vụ và Bảng xếp hạng đã sẵn sàng!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable PvPScheduler: " + e.getMessage());
            e.printStackTrace();
            // Disable plugin if critical error
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (pvpManager != null) {
            pvpManager.stop();
        }

        cancelTask(autoSaveTask);
        cancelTask(questCleanupTask);

        // DISABLED: BackupManager backup feature disabled
        // safeInvoke(backupManager, BackupManager::stopAutoBackup, "Error stopping backup manager");
        safeInvoke(animationManager, AnimationManager::cleanup, "Error cleaning up animations");
        safeInvoke(securityManager, com.example.pvpscheduler.manager.SecurityManager::cleanup, "Error cleaning up security manager");
        safeInvoke(activityLogger, ActivityLogger::cleanup, "Error cleaning up activity logger");
        safeInvoke(performanceMonitor, PerformanceMonitor::cleanup, "Error cleaning up performance monitor");
        safeInvoke(eventTracker, EventTracker::cleanup, "Error cleaning up event tracker");
        safeInvoke(auditLog, AuditLog::cleanup, "Error cleaning up audit log");
        safeInvoke(notificationManager, NotificationManager::cleanup, "Error cleaning up notification manager");
        safeInvoke(rateLimiter, RateLimiter::cleanup, "Error cleaning up rate limiter");
        safeInvoke(sessionManager, SessionManager::cleanup, "Error cleaning up session manager");

        if (statsManager != null) {
            try {
                statsManager.saveAllStats();
            } catch (Exception e) {
                getLogger().severe("Error saving stats on shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }

        getLogger().info("PvPScheduler đã được tắt. Tất cả dữ liệu đã được lưu.");
    }

    private void startAutoSave() {
        // Cancel existing task if any
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }
        
        int interval = getConfig().getInt("stats.auto-save-interval", 300) * 20; // Convert to ticks
        
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (statsManager != null && 
                        (adminMenuManager == null || adminMenuManager.isAutoSaveEnabled())) {
                        statsManager.saveAllStats();
                        getLogger().fine("Đã tự động lưu thống kê.");
                    }
                } catch (Exception e) {
                    getLogger().warning("Error in auto-save task: " + e.getMessage());
                }
            }
        };
        autoSaveTask.runTaskTimer(this, interval, interval);
    }
    
    private void startQuestCleanup() {
        // Cancel existing task if any
        if (questCleanupTask != null && !questCleanupTask.isCancelled()) {
            questCleanupTask.cancel();
        }
        
        // Clean up expired quests every 5 minutes
        questCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (questManager != null) {
                        questManager.removeExpiredQuests();
                    }
                } catch (Exception e) {
                    getLogger().warning("Error in quest cleanup task: " + e.getMessage());
                }
            }
        };
        questCleanupTask.runTaskTimer(this, 6000L, 6000L); // 5 minutes
    }
    
    // Getters for managers (if needed by other plugins)
    public PvPManager getPvPManager() {
        return pvpManager;
    }
    
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
    
    public AdminMenuManager getAdminMenuManager() {
        return adminMenuManager;
    }
    
    public com.example.pvpscheduler.manager.SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public ActivityLogger getActivityLogger() {
        return activityLogger;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public BackupManager getBackupManager() {
        // DISABLED: BackupManager backup feature disabled
        return null;
    }
    
    public EventTracker getEventTracker() {
        return eventTracker;
    }
    
    public AuditLog getAuditLog() {
        return auditLog;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    private void cancelTask(BukkitRunnable task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private <T> void safeInvoke(T target, UnsafeConsumer<T> action, String failureMessage) {
        if (target == null) {
            return;
        }
        try {
            action.accept(target);
        } catch (Exception e) {
            getLogger().warning(failureMessage + ": " + e.getMessage());
        }
    }



    @FunctionalInterface
    private interface UnsafeConsumer<T> {
        void accept(T target) throws Exception;
    }
}
