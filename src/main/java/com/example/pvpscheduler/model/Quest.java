package com.example.pvpscheduler.model;

public class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final int target;
    private final QuestReward reward;
    private final long expiryTime;
    
    public enum QuestType {
        KILL_PLAYERS,
        KILL_STREAK,
        SURVIVE_TIME,
        TOTAL_KILLS
    }
    
    public static class QuestReward {
        private final String command;
        private final String message;
        
        public QuestReward(String command, String message) {
            this.command = command;
            this.message = message;
        }
        
        public String getCommand() {
            return command;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public Quest(String id, String name, String description, QuestType type, int target, QuestReward reward, long expiryTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.reward = reward;
        this.expiryTime = expiryTime;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public QuestType getType() {
        return type;
    }
    
    public int getTarget() {
        return target;
    }
    
    public QuestReward getReward() {
        return reward;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}

