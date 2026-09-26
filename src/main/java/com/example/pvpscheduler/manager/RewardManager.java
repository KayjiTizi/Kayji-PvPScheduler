package com.example.pvpscheduler.manager;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RewardManager {
    private final JavaPlugin plugin;
    private final List<Reward> rewards = new ArrayList<>();
    private final Random random = new Random();
    
    public RewardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }
    
    private void loadRewards() {
        rewards.clear();
        
        // Tạo 1000 phần thưởng với tỉ lệ khác nhau
        // Common (70%) - 700 phần thưởng
        for (int i = 0; i < 700; i++) {
            rewards.add(createCommonReward(i));
        }
        
        // Uncommon (20%) - 200 phần thưởng
        for (int i = 0; i < 200; i++) {
            rewards.add(createUncommonReward(i));
        }
        
        // Rare (7%) - 70 phần thưởng
        for (int i = 0; i < 70; i++) {
            rewards.add(createRareReward(i));
        }
        
        // Epic (2.5%) - 25 phần thưởng
        for (int i = 0; i < 25; i++) {
            rewards.add(createEpicReward(i));
        }
        
        // Legendary (0.4%) - 4 phần thưởng
        for (int i = 0; i < 4; i++) {
            rewards.add(createLegendaryReward(i));
        }
        
        // Mythic (0.1%) - 1 phần thưởng
        rewards.add(createMythicReward());
        
        // Shuffle để random
        Collections.shuffle(rewards);
        
        plugin.getLogger().info("[RewardManager] Loaded " + rewards.size() + " rewards.");
    }
    
    private Reward createCommonReward(int index) {
        Material[] commonMaterials = {
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COAL,
            Material.LEATHER, Material.STRING, Material.BONE,
            Material.ROTTEN_FLESH, Material.SPIDER_EYE, Material.FEATHER
        };
        
        Material material = commonMaterials[index % commonMaterials.length];
        int amount = random.nextInt(5) + 1; // 1-5 items
        
        return new Reward(
            "§7" + getMaterialName(material) + " x" + amount,
            new ItemStack(material, amount),
            Reward.Rarity.COMMON,
            70.0
        );
    }
    
    private Reward createUncommonReward(int index) {
        Material[] uncommonMaterials = {
            Material.DIAMOND, Material.EMERALD, Material.QUARTZ,
            Material.REDSTONE, Material.LAPIS_LAZULI, Material.IRON_BLOCK,
            Material.GOLD_BLOCK, Material.ENCHANTED_BOOK
        };
        
        Material material = uncommonMaterials[index % uncommonMaterials.length];
        int amount = random.nextInt(3) + 1; // 1-3 items
        
        return new Reward(
            "§a" + getMaterialName(material) + " x" + amount,
            new ItemStack(material, amount),
            Reward.Rarity.UNCOMMON,
            20.0
        );
    }
    
    private Reward createRareReward(int index) {
        Material[] rareMaterials = {
            Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
            Material.NETHERITE_INGOT, Material.BEACON,
            Material.ELYTRA, Material.TOTEM_OF_UNDYING
        };
        
        Material material = rareMaterials[index % rareMaterials.length];
        int amount = 1;
        
        return new Reward(
            "§b" + getMaterialName(material),
            new ItemStack(material, amount),
            Reward.Rarity.RARE,
            7.0
        );
    }
    
    private Reward createEpicReward(int index) {
        Material[] epicMaterials = {
            Material.NETHERITE_BLOCK, Material.ENCHANTED_GOLDEN_APPLE,
            Material.NETHER_STAR, Material.DRAGON_EGG
        };
        
        Material material = epicMaterials[index % epicMaterials.length];
        int amount = 1;
        
        return new Reward(
            "§5" + getMaterialName(material),
            new ItemStack(material, amount),
            Reward.Rarity.EPIC,
            2.5
        );
    }
    
    private Reward createLegendaryReward(int index) {
        String[] legendaryNames = {
            "§6§lTHẦN KIẾM BẤT TỬ",
            "§6§lÁO GIÁP THIÊN THẦN",
            "§6§lVƯƠNG MIỆN QUYỀN LỰC",
            "§6§lNHẪN VÔ HẠN"
        };
        
        Material[] legendaryMaterials = {
            Material.NETHERITE_SWORD,
            Material.NETHERITE_CHESTPLATE,
            Material.GOLDEN_HELMET,
            Material.GOLDEN_APPLE
        };
        
        return new Reward(
            legendaryNames[index],
            new ItemStack(legendaryMaterials[index], 1),
            Reward.Rarity.LEGENDARY,
            0.4
        );
    }
    
    private Reward createMythicReward() {
        return new Reward(
            "§c§l§k||| §c§lPHẦN THƯỞNG THẦN THÁNH §c§l§k|||",
            new ItemStack(Material.BEACON, 1),
            Reward.Rarity.MYTHIC,
            0.1
        );
    }
    
    private String getMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase())
                  .append(word.substring(1));
        }
        return result.toString();
    }
    
    public Reward spinReward(boolean isTop1) {
        // Top 1 có tỉ lệ cao hơn
        double roll = random.nextDouble() * 100;
        
        if (isTop1) {
            // Top 1: 50% rare+, 30% uncommon, 20% common
            if (roll < 0.1) {
                return getRandomRewardByRarity(Reward.Rarity.MYTHIC);
            } else if (roll < 0.5) {
                return getRandomRewardByRarity(Reward.Rarity.LEGENDARY);
            } else if (roll < 5.0) {
                return getRandomRewardByRarity(Reward.Rarity.EPIC);
            } else if (roll < 20.0) {
                return getRandomRewardByRarity(Reward.Rarity.RARE);
            } else if (roll < 50.0) {
                return getRandomRewardByRarity(Reward.Rarity.UNCOMMON);
            } else {
                return getRandomRewardByRarity(Reward.Rarity.COMMON);
            }
        } else {
            // Người khác: tỉ lệ bình thường
            if (roll < 0.1) {
                return getRandomRewardByRarity(Reward.Rarity.MYTHIC);
            } else if (roll < 0.5) {
                return getRandomRewardByRarity(Reward.Rarity.LEGENDARY);
            } else if (roll < 3.0) {
                return getRandomRewardByRarity(Reward.Rarity.EPIC);
            } else if (roll < 10.0) {
                return getRandomRewardByRarity(Reward.Rarity.RARE);
            } else if (roll < 30.0) {
                return getRandomRewardByRarity(Reward.Rarity.UNCOMMON);
            } else {
                return getRandomRewardByRarity(Reward.Rarity.COMMON);
            }
        }
    }
    
    private Reward getRandomRewardByRarity(Reward.Rarity rarity) {
        List<Reward> filtered = rewards.stream()
            .filter(r -> r.getRarity() == rarity)
            .toList();
        
        if (filtered.isEmpty()) {
            // Fallback to common
            return rewards.stream()
                .filter(r -> r.getRarity() == Reward.Rarity.COMMON)
                .findFirst()
                .orElse(rewards.get(0));
        }
        
        return filtered.get(random.nextInt(filtered.size()));
    }
    
    public static class Reward {
        private final String name;
        private final ItemStack item;
        private final Rarity rarity;
        private final double chance;
        
        public Reward(String name, ItemStack item, Rarity rarity, double chance) {
            this.name = name;
            this.item = item;
            this.rarity = rarity;
            this.chance = chance;
        }
        
        public String getName() {
            return name;
        }
        
        public ItemStack getItem() {
            return item.clone();
        }
        
        public Rarity getRarity() {
            return rarity;
        }
        
        public double getChance() {
            return chance;
        }
        
        public enum Rarity {
            COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
        }
    }
}

