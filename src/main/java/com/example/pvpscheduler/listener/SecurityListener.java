package com.example.pvpscheduler.listener;

import com.example.pvpscheduler.manager.SecurityManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * SecurityListener - Listener cho SecurityManager
 */
public class SecurityListener implements Listener {
    private final SecurityManager securityManager;
    
    public SecurityListener(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            if (event == null || event.getEntity() == null) return;
            
            if (event.getEntity().getKiller() != null) {
                securityManager.recordKill(event.getEntity().getKiller().getUniqueId());
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
}

