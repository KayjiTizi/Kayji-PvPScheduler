package com.example.pvpscheduler.model;

import java.util.UUID;

public class ActiveQuest {
    private final UUID playerId;
    private final Quest quest;
    private int progress;
    private boolean completed;
    
    public ActiveQuest(UUID playerId, Quest quest) {
        this.playerId = playerId;
        this.quest = quest;
        this.progress = 0;
        this.completed = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Quest getQuest() {
        return quest;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void addProgress(int amount) {
        this.progress += amount;
        if (this.progress >= quest.getTarget() && !completed) {
            this.completed = true;
        }
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
        if (this.progress >= quest.getTarget() && !completed) {
            this.completed = true;
        }
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public double getProgressPercentage() {
        return (double) progress / quest.getTarget() * 100;
    }
}

