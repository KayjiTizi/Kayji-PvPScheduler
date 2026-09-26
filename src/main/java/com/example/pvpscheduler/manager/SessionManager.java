package com.example.pvpscheduler.manager;

import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Quản lý sessions và tracking
 */
public class SessionManager {
    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, List<PlayerSession>> playerSessionHistory = new ConcurrentHashMap<>();
    
    public SessionManager() {
    }
    
    public void startSession(Player player) {
        if (player == null) return;
        
        UUID playerId = player.getUniqueId();
        
        // End previous session if exists
        endSession(playerId);
        
        // Create new session
        PlayerSession session = new PlayerSession(playerId, player.getName());
        session.setStartTime(LocalDateTime.now());
        session.setIpAddress(player.getAddress() != null ? 
            player.getAddress().getAddress().getHostAddress() : "unknown");
        
        activeSessions.put(playerId, session);
        
        // Add to history
        List<PlayerSession> history = playerSessionHistory.computeIfAbsent(playerId, 
            k -> new ArrayList<>());
        history.add(session);
        
        // Keep only recent sessions
        if (history.size() > 50) {
            history.remove(0);
        }
    }
    
    public void endSession(UUID playerId) {
        if (playerId == null) return;
        
        PlayerSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.setEndTime(LocalDateTime.now());
            session.calculateDuration();
        }
    }
    
    public PlayerSession getActiveSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    public List<PlayerSession> getSessionHistory(UUID playerId) {
        return new ArrayList<>(playerSessionHistory.getOrDefault(playerId, new ArrayList<>()));
    }
    
    public long getTotalPlayTime(UUID playerId) {
        List<PlayerSession> history = playerSessionHistory.get(playerId);
        if (history == null) return 0;
        
        long totalSeconds = 0;
        for (PlayerSession session : history) {
            if (session.getDurationSeconds() > 0) {
                totalSeconds += session.getDurationSeconds();
            }
        }
        
        // Add current session if active
        PlayerSession current = activeSessions.get(playerId);
        if (current != null && current.getStartTime() != null) {
            totalSeconds += java.time.Duration.between(
                current.getStartTime(), LocalDateTime.now()).getSeconds();
        }
        
        return totalSeconds;
    }
    
    public int getTotalSessions(UUID playerId) {
        List<PlayerSession> history = playerSessionHistory.get(playerId);
        return history != null ? history.size() : 0;
    }
    
    public void recordKill(UUID playerId) {
        PlayerSession session = activeSessions.get(playerId);
        if (session != null) {
            session.incrementKills();
        }
    }
    
    public void recordDeath(UUID playerId) {
        PlayerSession session = activeSessions.get(playerId);
        if (session != null) {
            session.incrementDeaths();
        }
    }
    
    public void recordQuestCompleted(UUID playerId) {
        PlayerSession session = activeSessions.get(playerId);
        if (session != null) {
            session.incrementQuestsCompleted();
        }
    }
    
    public void recordRewardReceived(UUID playerId) {
        PlayerSession session = activeSessions.get(playerId);
        if (session != null) {
            session.incrementRewardsReceived();
        }
    }
    
    public Map<UUID, PlayerSession> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }
    
    public void cleanupPlayerData(UUID playerId) {
        endSession(playerId);
        playerSessionHistory.remove(playerId);
    }
    
    public void cleanup() {
        // End all active sessions
        for (UUID playerId : new HashSet<>(activeSessions.keySet())) {
            endSession(playerId);
        }
        activeSessions.clear();
        playerSessionHistory.clear();
    }
    
    // Inner class
    public static class PlayerSession {
        private final UUID playerId;
        private final String playerName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds = 0;
        private String ipAddress;
        private int kills = 0;
        private int deaths = 0;
        private int questsCompleted = 0;
        private int rewardsReceived = 0;
        
        public PlayerSession(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }
        
        public void calculateDuration() {
            if (startTime != null && endTime != null) {
                durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            }
        }
        
        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void incrementQuestsCompleted() { questsCompleted++; }
        public void incrementRewardsReceived() { rewardsReceived++; }
        
        // Getters and Setters
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDurationSeconds() { return durationSeconds; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getQuestsCompleted() { return questsCompleted; }
        public int getRewardsReceived() { return rewardsReceived; }
    }
}

