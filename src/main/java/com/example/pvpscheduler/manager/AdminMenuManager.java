package com.example.pvpscheduler.manager;

import com.example.pvpscheduler.PvPScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminMenuManager implements Listener {
    private final JavaPlugin plugin;
    private final PvPScheduler mainPlugin;
    private final PvPManager pvpManager;
    
    // Settings flags
    private boolean pvpSystemEnabled = true;
    private boolean questSystemEnabled = true;
    private boolean antiCheatEnabled = true;
    private boolean animationEnabled = true;
    private boolean rewardSystemEnabled = true;
    private boolean leaderboardEnabled = true;
    private boolean statsSystemEnabled = true;
    private boolean broadcastEnabled = true;
    private boolean autoSaveEnabled = true;
    private boolean autoAssignQuests = true;
    
    public AdminMenuManager(JavaPlugin plugin, PvPScheduler mainPlugin,
                           PvPManager pvpManager) {
        this.plugin = plugin;
        this.mainPlugin = mainPlugin;
        this.pvpManager = pvpManager;
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadSettings();
    }
    
    public void openAdminMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "§6§lQuản Lý PvP Scheduler");
        
        // Fill borders with glass panes
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, border);
            }
        }
        
        // PvP System Toggle
        menu.setItem(10, createToggleItem(
            Material.DIAMOND_SWORD,
            "§6§lHệ Thống PvP",
            pvpSystemEnabled,
            Arrays.asList(
                "§7Bật/tắt hệ thống PvP chính",
                "§7Khi tắt, PvP sẽ không hoạt động",
                "",
                pvpSystemEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Quest System Toggle
        menu.setItem(11, createToggleItem(
            Material.BOOK,
            "§6§lHệ Thống Nhiệm Vụ",
            questSystemEnabled,
            Arrays.asList(
                "§7Bật/tắt hệ thống nhiệm vụ",
                "§7Người chơi sẽ không nhận được nhiệm vụ khi tắt",
                "",
                questSystemEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Anti-Cheat System Toggle
        menu.setItem(12, createToggleItem(
            Material.SHIELD,
            "§6§lHệ Thống Anti-Cheat",
            antiCheatEnabled,
            Arrays.asList(
                "§7Bật/tắt hệ thống chống gian lận",
                "§7Khi tắt, không kiểm tra gian lận",
                "",
                antiCheatEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Animation System Toggle
        menu.setItem(13, createToggleItem(
            Material.FIREWORK_ROCKET,
            "§6§lHệ Thống Animation",
            animationEnabled,
            Arrays.asList(
                "§7Bật/tắt hiệu ứng animation",
                "§7Khi tắt, không có hiệu ứng",
                "",
                animationEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Reward System Toggle
        menu.setItem(14, createToggleItem(
            Material.GOLD_INGOT,
            "§6§lHệ Thống Thưởng",
            rewardSystemEnabled,
            Arrays.asList(
                "§7Bật/tắt hệ thống thưởng",
                "§7Khi tắt, không trao thưởng",
                "",
                rewardSystemEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Leaderboard System Toggle
        menu.setItem(15, createToggleItem(
            Material.PAPER,
            "§6§lBảng Xếp Hạng",
            leaderboardEnabled,
            Arrays.asList(
                "§7Bật/tắt bảng xếp hạng",
                "§7Khi tắt, không hiển thị bảng xếp hạng",
                "",
                leaderboardEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Stats System Toggle
        menu.setItem(16, createToggleItem(
            Material.EMERALD,
            "§6§lHệ Thống Thống Kê",
            statsSystemEnabled,
            Arrays.asList(
                "§7Bật/tắt hệ thống thống kê",
                "§7Khi tắt, không lưu thống kê",
                "",
                statsSystemEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Broadcast Messages Toggle
        menu.setItem(19, createToggleItem(
            Material.BELL,
            "§6§lThông Báo Broadcast",
            broadcastEnabled,
            Arrays.asList(
                "§7Bật/tắt thông báo broadcast",
                "§7Khi tắt, không gửi thông báo",
                "",
                broadcastEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Auto-Save Stats Toggle
        menu.setItem(20, createToggleItem(
            Material.WRITABLE_BOOK,
            "§6§lTự Động Lưu Thống Kê",
            autoSaveEnabled,
            Arrays.asList(
                "§7Bật/tắt tự động lưu thống kê",
                "§7Khi tắt, chỉ lưu khi tắt server",
                "",
                autoSaveEnabled ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Auto-Assign Quests Toggle
        menu.setItem(21, createToggleItem(
            Material.ENCHANTED_BOOK,
            "§6§lTự Động Gán Nhiệm Vụ",
            autoAssignQuests,
            Arrays.asList(
                "§7Bật/tắt tự động gán nhiệm vụ",
                "§7Khi tắt, người chơi phải tự nhận nhiệm vụ",
                "",
                autoAssignQuests ? "§a§lĐANG BẬT" : "§c§lĐANG TẮT"
            )
        ));
        
        // Reload Config Button
        menu.setItem(40, createItem(
            Material.REDSTONE,
            "§c§lReload Config",
            Arrays.asList(
                "§7Nhấn để reload lại config",
                "§7Tất cả cài đặt sẽ được tải lại",
                "",
                "§eNhấn để reload!"
            )
        ));
        
        // Save & Close Button
        menu.setItem(49, createItem(
            Material.EMERALD_BLOCK,
            "§a§lLưu & Đóng",
            Arrays.asList(
                "§7Lưu tất cả cài đặt và đóng menu",
                "",
                "§eNhấn để lưu!"
            )
        ));
        
        player.openInventory(menu);
    }
    
    private ItemStack createToggleItem(Material material, String name, boolean enabled, List<String> lore) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> finalLore = new ArrayList<>(lore);
            finalLore.add("");
            finalLore.add(enabled ? "§a► Nhấn để TẮT" : "§c► Nhấn để BẬT");
            meta.setLore(finalLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lQuản Lý PvP Scheduler")) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!player.hasPermission("pvpscheduler.admin")) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage("§cBạn không có quyền sử dụng menu này!");
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Handle toggles
        switch (slot) {
            case 10: // PvP System
                togglePvPSystem(player);
                break;
            case 11: // Quest System
                toggleQuestSystem(player);
                break;
            case 12: // Anti-Cheat
                toggleAntiCheat(player);
                break;
            case 13: // Animation
                toggleAnimation(player);
                break;
            case 14: // Reward System
                toggleRewardSystem(player);
                break;
            case 15: // Leaderboard
                toggleLeaderboard(player);
                break;
            case 16: // Stats System
                toggleStatsSystem(player);
                break;
            case 19: // Broadcast
                toggleBroadcast(player);
                break;
            case 20: // Auto-Save
                toggleAutoSave(player);
                break;
            case 21: // Auto-Assign Quests
                toggleAutoAssignQuests(player);
                break;
            case 40: // Reload Config
                reloadConfig(player);
                break;
            case 49: // Save & Close
                saveSettings();
                player.closeInventory();
                player.sendMessage("§aĐã lưu tất cả cài đặt!");
                break;
        }
        
        // Update menu
        if (slot != 40 && slot != 49) {
            openAdminMenu(player);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§6§lQuản Lý PvP Scheduler")) {
            // Auto-save when closing
            saveSettings();
        }
    }
    
    private void togglePvPSystem(Player player) {
        pvpSystemEnabled = !pvpSystemEnabled;
        if (!pvpSystemEnabled && pvpManager.isPvPEnabled()) {
            pvpManager.setPvP(false);
        }
        player.sendMessage(pvpSystemEnabled ? "§aĐã bật hệ thống PvP!" : "§cĐã tắt hệ thống PvP!");
    }
    
    private void toggleQuestSystem(Player player) {
        questSystemEnabled = !questSystemEnabled;
        player.sendMessage(questSystemEnabled ? "§aĐã bật hệ thống nhiệm vụ!" : "§cĐã tắt hệ thống nhiệm vụ!");
    }
    
    private void toggleAntiCheat(Player player) {
        antiCheatEnabled = !antiCheatEnabled;
        player.sendMessage(antiCheatEnabled ? "§aĐã bật hệ thống anti-cheat!" : "§cĐã tắt hệ thống anti-cheat!");
    }
    
    private void toggleAnimation(Player player) {
        animationEnabled = !animationEnabled;
        player.sendMessage(animationEnabled ? "§aĐã bật hệ thống animation!" : "§cĐã tắt hệ thống animation!");
    }
    
    private void toggleRewardSystem(Player player) {
        rewardSystemEnabled = !rewardSystemEnabled;
        player.sendMessage(rewardSystemEnabled ? "§aĐã bật hệ thống thưởng!" : "§cĐã tắt hệ thống thưởng!");
    }
    
    private void toggleLeaderboard(Player player) {
        leaderboardEnabled = !leaderboardEnabled;
        player.sendMessage(leaderboardEnabled ? "§aĐã bật bảng xếp hạng!" : "§cĐã tắt bảng xếp hạng!");
    }
    
    private void toggleStatsSystem(Player player) {
        statsSystemEnabled = !statsSystemEnabled;
        player.sendMessage(statsSystemEnabled ? "§aĐã bật hệ thống thống kê!" : "§cĐã tắt hệ thống thống kê!");
    }
    
    private void toggleBroadcast(Player player) {
        broadcastEnabled = !broadcastEnabled;
        player.sendMessage(broadcastEnabled ? "§aĐã bật thông báo broadcast!" : "§cĐã tắt thông báo broadcast!");
    }
    
    private void toggleAutoSave(Player player) {
        autoSaveEnabled = !autoSaveEnabled;
        player.sendMessage(autoSaveEnabled ? "§aĐã bật tự động lưu thống kê!" : "§cĐã tắt tự động lưu thống kê!");
    }
    
    private void toggleAutoAssignQuests(Player player) {
        autoAssignQuests = !autoAssignQuests;
        player.sendMessage(autoAssignQuests ? "§aĐã bật tự động gán nhiệm vụ!" : "§cĐã tắt tự động gán nhiệm vụ!");
    }
    
    private void reloadConfig(Player player) {
        plugin.reloadConfig();
        mainPlugin.getPvPManager().loadConfig(plugin.getConfig());
        loadSettings();
        player.sendMessage("§aĐã reload config!");
        openAdminMenu(player);
    }
    
    public void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        
        pvpSystemEnabled = config.getBoolean("admin-settings.pvp-system-enabled", true);
        questSystemEnabled = config.getBoolean("admin-settings.quest-system-enabled", true);
        antiCheatEnabled = config.getBoolean("admin-settings.anti-cheat-enabled", true);
        animationEnabled = config.getBoolean("admin-settings.animation-enabled", true);
        rewardSystemEnabled = config.getBoolean("admin-settings.reward-system-enabled", true);
        leaderboardEnabled = config.getBoolean("admin-settings.leaderboard-enabled", true);
        statsSystemEnabled = config.getBoolean("admin-settings.stats-system-enabled", true);
        broadcastEnabled = config.getBoolean("admin-settings.broadcast-enabled", true);
        autoSaveEnabled = config.getBoolean("admin-settings.auto-save-enabled", true);
        autoAssignQuests = config.getBoolean("admin-settings.auto-assign-quests", true);
    }
    
    public void saveSettings() {
        FileConfiguration config = plugin.getConfig();
        
        config.set("admin-settings.pvp-system-enabled", pvpSystemEnabled);
        config.set("admin-settings.quest-system-enabled", questSystemEnabled);
        config.set("admin-settings.anti-cheat-enabled", antiCheatEnabled);
        config.set("admin-settings.animation-enabled", animationEnabled);
        config.set("admin-settings.reward-system-enabled", rewardSystemEnabled);
        config.set("admin-settings.leaderboard-enabled", leaderboardEnabled);
        config.set("admin-settings.stats-system-enabled", statsSystemEnabled);
        config.set("admin-settings.broadcast-enabled", broadcastEnabled);
        config.set("admin-settings.auto-save-enabled", autoSaveEnabled);
        config.set("admin-settings.auto-assign-quests", autoAssignQuests);
        
        plugin.saveConfig();
    }
    
    // Getters for other managers to check if features are enabled
    public boolean isPvPSystemEnabled() {
        return pvpSystemEnabled;
    }
    
    public boolean isQuestSystemEnabled() {
        return questSystemEnabled;
    }
    
    public boolean isAntiCheatEnabled() {
        return antiCheatEnabled;
    }
    
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }
    
    public boolean isRewardSystemEnabled() {
        return rewardSystemEnabled;
    }
    
    public boolean isLeaderboardEnabled() {
        return leaderboardEnabled;
    }
    
    public boolean isStatsSystemEnabled() {
        return statsSystemEnabled;
    }
    
    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }
    
    public boolean isAutoAssignQuests() {
        return autoAssignQuests;
    }
}

