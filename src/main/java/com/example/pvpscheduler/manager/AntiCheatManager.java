package com.example.pvpscheduler.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerActivity> playerActivities = new ConcurrentHashMap<>();
    
    // Ngưỡng nghi ngờ (giữ lại giá trị tham khảo cho các phép tính khác nếu cần)
    private static final double MAX_DAMAGE_PER_SECOND = 50.0; // Quá cao = có thể hack
    private static final double MAX_MOVEMENT_SPEED = 1.5; // Quá nhanh = có thể speed hack
    
    public AntiCheatManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        try {
            if (!(event.getDamager() instanceof Player attacker)) return;
            if (!(event.getEntity() instanceof Player)) return;
            
            UUID attackerId = attacker.getUniqueId();
            if (attackerId == null) return;
            
            PlayerActivity activity = playerActivities.computeIfAbsent(
                attackerId, k -> new PlayerActivity()
            );
            
            long currentTime = System.currentTimeMillis();
            activity.addDamage(currentTime, event.getFinalDamage());
            
            // Kiểm tra damage per second
            double dps = activity.getDamagePerSecond();
            if (dps > MAX_DAMAGE_PER_SECOND) {
                addSuspiciousAction(attackerId, "Damage quá cao: " + String.format("%.2f", dps) + " DPS");
            }
            
            // Kiểm tra kill time (giết quá nhanh)
            if (activity.getRecentKills() > 3 && activity.getAverageKillTime() < 1000) {
                addSuspiciousAction(attackerId, "Giết người quá nhanh");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in onPlayerDamage: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) return;
            
            UUID playerId = player.getUniqueId();
            if (playerId == null) return;
            
            if (event.getFrom() == null || event.getTo() == null) return;
            if (event.getFrom().distance(event.getTo()) == 0) return;
            
            PlayerActivity activity = playerActivities.computeIfAbsent(
                playerId, k -> new PlayerActivity()
            );
            
            double speed = event.getFrom().distance(event.getTo()) * 20; // blocks per second
            if (speed > MAX_MOVEMENT_SPEED && !player.isFlying() && !player.isInsideVehicle()) {
                addSuspiciousAction(playerId, "Di chuyển quá nhanh: " + String.format("%.2f", speed) + " b/s");
            }
            
            activity.recordMovement(event.getTo());
        } catch (Exception e) {
            plugin.getLogger().warning("Error in onPlayerMove: " + e.getMessage());
        }
    }
    
    public void onPlayerKill(UUID killerId, UUID victimId) {
        try {
            if (killerId == null || victimId == null) return;
            
            PlayerActivity activity = playerActivities.computeIfAbsent(
                killerId, k -> new PlayerActivity()
            );
            
            long killTime = System.currentTimeMillis();
            activity.recordKill(killTime);
            
            // Kiểm tra kill streak bất thường
            if (activity.getRecentKills() > 10) {
                long timeSpan = killTime - activity.getFirstKillTime();
                if (timeSpan < 30000) { // 10 kills trong 30 giây
                    addSuspiciousAction(killerId, "Kill streak bất thường");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in onPlayerKill: " + e.getMessage());
        }
    }
    
    private void addSuspiciousAction(UUID playerId, String reason) {
        // Anti-cheat suspicion has been disabled per request, so we no longer track or log it.
    }
    
    public boolean isPlayerLegitimate(UUID playerId) {
        return true;
    }
    
    public void resetPlayerActivity(UUID playerId) {
        playerActivities.remove(playerId);
    }
    
    public void resetAllActivities() {
        playerActivities.clear();
    }
    
    private static class PlayerActivity {
        private final List<DamageRecord> damageRecords = new ArrayList<>();
        private final List<Long> killTimes = new ArrayList<>();
        private final List<org.bukkit.Location> movements = new ArrayList<>();
        
        public void addDamage(long time, double damage) {
            damageRecords.add(new DamageRecord(time, damage));
            // Chỉ giữ 5 giây gần nhất
            damageRecords.removeIf(r -> time - r.time > 5000);
        }
        
        public void recordKill(long time) {
            killTimes.add(time);
            // Chỉ giữ 1 phút gần nhất
            killTimes.removeIf(t -> time - t > 60000);
        }
        
        public void recordMovement(org.bukkit.Location location) {
            movements.add(location);
            // Chỉ giữ 100 movement gần nhất
            if (movements.size() > 100) {
                movements.remove(0);
            }
        }
        
        public double getDamagePerSecond() {
            if (damageRecords.isEmpty()) return 0;
            
            long now = System.currentTimeMillis();
            double totalDamage = damageRecords.stream()
                .filter(r -> now - r.time <= 1000)
                .mapToDouble(r -> r.damage)
                .sum();
            
            return totalDamage;
        }
        
        public int getRecentKills() {
            return killTimes.size();
        }
        
        public long getFirstKillTime() {
            return killTimes.isEmpty() ? System.currentTimeMillis() : killTimes.get(0);
        }
        
        public double getAverageKillTime() {
            if (killTimes.size() < 2) return 0;
            
            long totalTime = 0;
            for (int i = 1; i < killTimes.size(); i++) {
                totalTime += killTimes.get(i) - killTimes.get(i - 1);
            }
            
            return totalTime / (double) (killTimes.size() - 1);
        }
        
        private static class DamageRecord {
            final long time;
            final double damage;
            
            DamageRecord(long time, double damage) {
                this.time = time;
                this.damage = damage;
            }
        }
    }
}

