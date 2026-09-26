package com.example.pvpscheduler.manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> activeAnimations = new ConcurrentHashMap<>();
    
    public AnimationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void cancelPlayerAnimations(UUID playerId) {
        BukkitTask task = activeAnimations.remove(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    public void cleanup() {
        for (BukkitTask task : activeAnimations.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeAnimations.clear();
    }
    
    public void playTop1RewardAnimation(Player player) {
        if (player == null || !player.isOnline()) return;
        
        try {
            // Cancel existing animation for this player
            cancelPlayerAnimations(player.getUniqueId());
            
            Location loc = player.getLocation();
            World world = player.getWorld();
            if (world == null || loc == null) return;
            
            // Hiệu ứng vòng xung quanh người chơi
            BukkitTask ringTask = new BukkitRunnable() {
            private int ticks = 0;
            private final int duration = 100; // 5 giây
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location center = player.getLocation();
                
                // Draw a clearer two-layer reward ring
                Color primaryColor = Color.fromRGB(255, 215, 0);
                Color accentColor = Color.fromRGB(255, 140, 0);
                float pulseSize = 1.1f + (float)(Math.sin(ticks * 0.15) * 0.4f);
                Particle.DustOptions primaryDust = new Particle.DustOptions(primaryColor, pulseSize);
                Particle.DustOptions accentDust = new Particle.DustOptions(accentColor, 0.9f);
                
                double outerRadius = 2.2;
                double innerRadius = 1.4;
                
                for (int angleDeg = 0; angleDeg < 360; angleDeg += 5) {
                    double angle = Math.toRadians(angleDeg);
                    double wobble = Math.sin(Math.toRadians(angleDeg * 2) + ticks * 0.1) * 0.15;
                    
                    double outerX = center.getX() + Math.cos(angle) * (outerRadius + wobble);
                    double outerZ = center.getZ() + Math.sin(angle) * (outerRadius + wobble);
                    Location outerLoc = new Location(world, outerX, center.getY() + 0.2, outerZ);
                    world.spawnParticle(Particle.REDSTONE, outerLoc, 1, primaryDust);
                    
                    double innerX = center.getX() + Math.cos(angle) * (innerRadius + wobble * 0.5);
                    double innerZ = center.getZ() + Math.sin(angle) * (innerRadius + wobble * 0.5);
                    Location innerLoc = new Location(world, innerX, center.getY() + 1.0, innerZ);
                    world.spawnParticle(Particle.REDSTONE, innerLoc, 1, accentDust);
                    
                    if (angleDeg % 60 == 0) {
                        for (double h = 0.2; h <= 1.8; h += 0.35) {
                            Location pillarLoc = new Location(world, outerX, center.getY() + h, outerZ);
                            world.spawnParticle(Particle.END_ROD, pillarLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }
                
                double swirlAngle = Math.toRadians((ticks * 6) % 360);
                for (double height = 0.2; height <= 1.6; height += 0.2) {
                    double swirlRadius = 0.8 + height * 0.5;
                    double x = center.getX() + Math.cos(swirlAngle + height) * swirlRadius;
                    double z = center.getZ() + Math.sin(swirlAngle + height) * swirlRadius;
                    Location swirlLoc = new Location(world, x, center.getY() + height, z);
                    world.spawnParticle(Particle.FIREWORKS_SPARK, swirlLoc, 1, 0, 0, 0, 0.02);
                }
                
                // Hiệu ứng trên đầu
                if (ticks % 10 == 0) {
                    Location headLoc = center.clone().add(0, 2, 0);
                    world.spawnParticle(
                        Particle.TOTEM,
                        headLoc,
                        20,
                        0.5, 0.5, 0.5, 0.1
                    );
                    
                    world.spawnParticle(
                        Particle.END_ROD,
                        headLoc,
                        10,
                        0.3, 0.3, 0.3, 0.05
                    );
                }
                
                // Hiệu ứng âm thanh
                if (ticks % 20 == 0) {
                    world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f + ticks * 0.01f);
                }
                
                ticks++;
            }
            }.runTaskTimer(plugin, 0L, 1L);
            
            // Track animation task
            activeAnimations.put(player.getUniqueId(), ringTask);
            
            // Hiệu ứng title
            player.sendTitle(
                "§6§l🎉 CHÚC MỪNG! 🎉",
                "§eBạn đã đạt TOP 1!",
                10, 60, 20
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Error playing top1 animation: " + e.getMessage());
        }
    }
    
    public void playRewardSpinAnimation(Player player, RewardManager.Reward reward) {
        if (player == null || !player.isOnline() || reward == null) return;
        
        try {
            Location loc = player.getLocation();
            World world = player.getWorld();
            if (world == null || loc == null) return;
            
            // Animation quay thưởng
            BukkitTask spinTask = new BukkitRunnable() {
            private int ticks = 0;
            private final int duration = 60; // 3 giây
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    // Kết thúc animation, trao thưởng
                    giveReward(player, reward);
                    cancel();
                    return;
                }
                
                Location center = player.getLocation();
                
                // Hiệu ứng quay
                for (int i = 0; i < 360; i += 20) {
                    double angle = Math.toRadians(i + ticks * 10);
                    double radius = 2.0;
                    
                    double x = center.getX() + Math.cos(angle) * radius;
                    double y = center.getY() + 1.0 + Math.sin(ticks * 0.2) * 0.5;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    
                    // Particle theo màu rarity
                    Color color = getRarityColor(reward.getRarity());
                    world.spawnParticle(
                        Particle.REDSTONE,
                        particleLoc,
                        1,
                        new Particle.DustOptions(color, 1.5f)
                    );
                }
                
                // Hiệu ứng ở giữa
                Location centerLoc = center.clone().add(0, 1.5, 0);
                world.spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    centerLoc,
                    30,
                    0.5, 0.5, 0.5, 0.2
                );
                
                // Âm thanh
                if (ticks % 10 == 0) {
                    world.playSound(center, Sound.UI_TOAST_IN, 0.5f, 1.0f + ticks * 0.02f);
                }
                
                ticks++;
            }
            }.runTaskTimer(plugin, 0L, 1L);
            
            // Track animation task
            activeAnimations.put(player.getUniqueId(), spinTask);
            
            // Title thông báo
            player.sendTitle(
                "§6§lĐANG QUAY...",
                "§e" + reward.getName(),
                5, 70, 15
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Error playing reward spin animation: " + e.getMessage());
        }
    }
    
    private Color getRarityColor(RewardManager.Reward.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Color.fromRGB(128, 128, 128); // Gray
            case UNCOMMON -> Color.fromRGB(0, 255, 0); // Green
            case RARE -> Color.fromRGB(0, 150, 255); // Blue
            case EPIC -> Color.fromRGB(128, 0, 128); // Purple
            case LEGENDARY -> Color.fromRGB(255, 215, 0); // Gold
            case MYTHIC -> Color.fromRGB(255, 0, 0); // Red
        };
    }
    
    private void giveReward(Player player, RewardManager.Reward reward) {
        ItemStack item = reward.getItem();
        
        // Thêm vào inventory
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            // Inventory đầy, drop xuống đất
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        
        // Thông báo
        player.sendMessage("§6§l═══════════════════════════");
        player.sendMessage("§e§lBẠN ĐÃ NHẬN ĐƯỢC:");
        player.sendMessage(reward.getName());
        player.sendMessage("§6§l═══════════════════════════");
        
        // Hiệu ứng cuối
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        world.spawnParticle(
            Particle.FIREWORKS_SPARK,
            loc.clone().add(0, 1, 0),
            100,
            1, 1, 1, 0.3
        );
        
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
}

