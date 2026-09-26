package com.example.pvpscheduler.model;

import java.util.UUID;

public class PlayerStats {
    private final UUID playerId;
    private int kills;
    private int deaths;
    private int killStreak;
    private int bestKillStreak;
    private long totalPlayTime;
    private long lastKillTime;
    
    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.kills = 0;
        this.deaths = 0;
        this.killStreak = 0;
        this.bestKillStreak = 0;
        this.totalPlayTime = 0;
        this.lastKillTime = 0;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void addKill() {
        this.kills++;
        this.killStreak++;
        this.lastKillTime = System.currentTimeMillis();
        if (this.killStreak > this.bestKillStreak) {
            this.bestKillStreak = this.killStreak;
        }
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void addDeath() {
        this.deaths++;
        this.killStreak = 0;
    }
    
    public int getKillStreak() {
        return killStreak;
    }
    
    public int getBestKillStreak() {
        return bestKillStreak;
    }
    
    public double getKDR() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }
    
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    public void addPlayTime(long time) {
        this.totalPlayTime += time;
    }
    
    public long getLastKillTime() {
        return lastKillTime;
    }
    
    public void resetSession() {
        this.killStreak = 0;
    }
    
    // Setters for loading from file
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    public void setBestKillStreak(int bestKillStreak) {
        this.bestKillStreak = bestKillStreak;
    }
    
    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }
}

