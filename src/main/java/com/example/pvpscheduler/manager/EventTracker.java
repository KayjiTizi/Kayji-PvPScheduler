package com.example.pvpscheduler.manager;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventTracker - Theo dõi và phân tích events
 */
public class EventTracker {
    private final JavaPlugin plugin;
    private final Map<String, EventStatistics> eventStats = new ConcurrentHashMap<>();
    private final Map<UUID, List<TrackedEvent>> playerEvents = new ConcurrentHashMap<>();
    private final List<TrackedEvent> recentEvents = new ArrayList<>();
    
    private static final int MAX_RECENT_EVENTS = 500;
    private static final int MAX_PLAYER_EVENTS = 100;
    
    public EventTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void trackEvent(String eventType, UUID playerId, String playerName, String details) {
        try {
            if (eventType == null) return;
            
            TrackedEvent event = new TrackedEvent(eventType, playerId, playerName, details, LocalDateTime.now());
            
            // Update statistics
            EventStatistics stats = eventStats.computeIfAbsent(eventType, 
                k -> new EventStatistics(eventType));
            stats.recordEvent();
            
            // Track per player
            if (playerId != null) {
                List<TrackedEvent> events = playerEvents.computeIfAbsent(playerId, 
                    k -> new ArrayList<>());
                events.add(event);
                
                // Keep only recent events
                if (events.size() > MAX_PLAYER_EVENTS) {
                    events.remove(0);
                }
            }
            
            // Track recent events
            synchronized (recentEvents) {
                recentEvents.add(event);
                if (recentEvents.size() > MAX_RECENT_EVENTS) {
                    recentEvents.remove(0);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error tracking event: " + e.getMessage());
        }
    }
    
    public void trackPvPEvent(UUID playerId, String playerName, String action, String target) {
        trackEvent("PvP_" + action, playerId, playerName, 
            String.format("Target: %s", target));
    }
    
    public void trackQuestEvent(UUID playerId, String playerName, String action, String questName) {
        trackEvent("QUEST_" + action, playerId, playerName, 
            String.format("Quest: %s", questName));
    }
    
    public void trackRewardEvent(UUID playerId, String playerName, String rewardType) {
        trackEvent("REWARD", playerId, playerName, 
            String.format("Reward: %s", rewardType));
    }
    
    public EventStatistics getEventStatistics(String eventType) {
        return eventStats.get(eventType);
    }
    
    public List<TrackedEvent> getPlayerEvents(UUID playerId) {
        return new ArrayList<>(playerEvents.getOrDefault(playerId, new ArrayList<>()));
    }
    
    public List<TrackedEvent> getRecentEvents(int count) {
        synchronized (recentEvents) {
            int start = Math.max(0, recentEvents.size() - count);
            return new ArrayList<>(recentEvents.subList(start, recentEvents.size()));
        }
    }
    
    public Map<String, Integer> getEventCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, EventStatistics> entry : eventStats.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().getCount());
        }
        return counts;
    }
    
    public List<String> getTopEvents(int count) {
        return eventStats.entrySet().stream()
            .sorted(Map.Entry.<String, EventStatistics>comparingByValue(
                Comparator.comparingInt(EventStatistics::getCount).reversed()))
            .limit(count)
            .map(e -> e.getKey() + ": " + e.getValue().getCount())
            .collect(java.util.stream.Collectors.toList());
    }
    
    public void cleanupPlayerData(UUID playerId) {
        playerEvents.remove(playerId);
    }
    
    public void cleanup() {
        eventStats.clear();
        playerEvents.clear();
        synchronized (recentEvents) {
            recentEvents.clear();
        }
    }
    
    // Inner classes
    public static class TrackedEvent {
        private final String eventType;
        private final UUID playerId;
        private final String playerName;
        private final String details;
        private final LocalDateTime timestamp;
        
        public TrackedEvent(String eventType, UUID playerId, String playerName, 
                           String details, LocalDateTime timestamp) {
            this.eventType = eventType;
            this.playerId = playerId;
            this.playerName = playerName;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        public String getEventType() { return eventType; }
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class EventStatistics {
        private final String eventType;
        private int count = 0;
        private LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        
        public EventStatistics(String eventType) {
            this.eventType = eventType;
        }
        
        public void recordEvent() {
            count++;
            LocalDateTime now = LocalDateTime.now();
            if (firstOccurrence == null) {
                firstOccurrence = now;
            }
            lastOccurrence = now;
        }
        
        public String getEventType() { return eventType; }
        public int getCount() { return count; }
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
    }
}

