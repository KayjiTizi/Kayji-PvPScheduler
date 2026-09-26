package com.example.pvpscheduler.listener;

import com.example.pvpscheduler.manager.*;
import com.example.pvpscheduler.util.MessageUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPListener implements Listener {
    private final PvPManager pvpManager;
    private final StatsManager statsManager;
    private final QuestManager questManager;
    private final SessionManager sessionManager;
    private final EventTracker eventTracker;
    private final Map<UUID, Long> claimWarnCooldown = new ConcurrentHashMap<>();
    
    public PvPListener(PvPManager pvpManager, StatsManager statsManager, QuestManager questManager,
                      SessionManager sessionManager, EventTracker eventTracker) {
        this.pvpManager = pvpManager;
        this.statsManager = statsManager;
        this.questManager = questManager;
        this.sessionManager = sessionManager;
        this.eventTracker = eventTracker;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            if (event == null || event.getEntity() == null || event.getEntity().getKiller() == null) {
                return;
            }
            
            if (pvpManager == null || !pvpManager.isPvPEnabled()) {
                return;
            }
            
            UUID victimId = event.getEntity().getUniqueId();
            UUID killerId = event.getEntity().getKiller().getUniqueId();
            
            if (victimId == null || killerId == null) {
                return;
            }
            
            // Update PvP manager
            pvpManager.onPlayerKill(killerId, victimId);
            
            // Track events
            if (eventTracker != null) {
                eventTracker.trackPvPEvent(killerId, 
                    event.getEntity().getKiller().getName(), "KILL", 
                    event.getEntity().getName());
            }
            
            // Update session data
            if (sessionManager != null) {
                sessionManager.recordKill(killerId);
                sessionManager.recordDeath(victimId);
            }
        } catch (Exception e) {
            System.err.println("Error in onPlayerDeath: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        try {
            if (!(event.getEntity() instanceof Player victim)) {
                return;
            }
            
            Player damager = null;
            if (event.getDamager() instanceof Player playerDamager) {
                damager = playerDamager;
            } else if (event.getDamager() instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player playerShooter) {
                    damager = playerShooter;
                }
            }
            
            if (damager == null) {
                return;
            }
            
            if (pvpManager == null || !pvpManager.isPvPEnabled()) {
                return;
            }
            
            // Kiểm tra GriefPrevention có sẵn không
            if (!isGriefPreventionEnabled()) {
                return; // Nếu không có GriefPrevention, cho phép PvP bình thường
            }
            
            try {
                // Kiểm tra cả victim và damager có đang ở trong claim không
                Claim victimClaim = GriefPrevention.instance.dataStore
                .getClaimAt(victim.getLocation(), true, null);
                Claim damagerClaim = GriefPrevention.instance.dataStore
                    .getClaimAt(damager.getLocation(), true, null);
            
                // Nếu một trong hai (hoặc cả hai) đang ở trong claim thì chặn PvP
                if (victimClaim != null || damagerClaim != null) {
                event.setCancelled(true);
                sendClaimWarning(damager);
                }
            } catch (NoClassDefFoundError | Exception e) {
                // Nếu GriefPrevention không có hoặc có lỗi, bỏ qua check claim
                // Plugin vẫn hoạt động bình thường
            }
        } catch (Exception e) {
            System.err.println("Error in claim PvP protection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            if (event == null || event.getPlayer() == null) {
                return;
            }
            
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            if (playerId == null) {
                return;
            }
            
            // Load player stats on join
            if (statsManager != null) {
                statsManager.getStats(playerId);
            }
            
            // Remove expired quests
            if (questManager != null) {
                questManager.removeExpiredQuests();
            }
            
            // Start session
            if (sessionManager != null) {
                sessionManager.startSession(player);
            }
            
            // Track event
            if (eventTracker != null) {
                eventTracker.trackEvent("PLAYER_JOIN", playerId, player.getName(), 
                    "Player joined the server");
            }
        } catch (Exception e) {
            System.err.println("Error in onPlayerJoin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            // Save player stats on quit
            if (statsManager != null) {
                statsManager.saveStats(playerId);
                statsManager.removePlayerFromCache(playerId);
            }
            
            // Cleanup quest data (remove expired quests for this player)
            if (questManager != null) {
                questManager.removeExpiredQuestsForPlayer(playerId);
            }
            
            // Cleanup PvP session data
            if (pvpManager != null) {
                pvpManager.cleanupPlayerData(playerId);
            }
            
            // End session
            if (sessionManager != null) {
                sessionManager.endSession(playerId);
                sessionManager.cleanupPlayerData(playerId);
            }
            
            // Track event
            if (eventTracker != null) {
                eventTracker.trackEvent("PLAYER_QUIT", playerId, player.getName(), 
                    "Player left the server");
                eventTracker.cleanupPlayerData(playerId);
            }
        } catch (Exception e) {
            // Log error but don't crash - use a simple print if logger not available
            System.err.println("Error cleaning up player data for " + playerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean isGriefPreventionEnabled() {
        return Bukkit.getPluginManager().getPlugin("GriefPrevention") != null
            && Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
    }
    
    private void sendClaimWarning(Player player) {
        long now = System.currentTimeMillis();
        long last = claimWarnCooldown.getOrDefault(player.getUniqueId(), 0L);
        
        if (now - last < 2000) {
            return;
        }
        
        MessageUtil.sendError(player, "PvP trong claim GriefPrevention bi vo hieu!");
        claimWarnCooldown.put(player.getUniqueId(), now);
    }
}
