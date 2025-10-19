package com.example.teamgame28.model;

import java.io.Serializable;

// ========== ALLIANCE MISSION PROGRESS ==========
public class AllianceMissionProgress implements Serializable {
    private String missionId;
    private String userId;
    private int damageDealt;
    private int tasksCompleted;
    private int shopPurchases;
    private int messagesSent;
    private boolean noUnfinishedTasks;

    public AllianceMissionProgress() {}

    public AllianceMissionProgress(String missionId, String userId, int damageDealt, int tasksCompleted, int shopPurchases, int messagesSent, boolean noUnfinishedTasks) {
        this.missionId = missionId;
        this.userId = userId;
        this.damageDealt = damageDealt;
        this.tasksCompleted = tasksCompleted;
        this.shopPurchases = shopPurchases;
        this.messagesSent = messagesSent;
        this.noUnfinishedTasks = noUnfinishedTasks;
    }

    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getDamageDealt() { return damageDealt; }
    public void setDamageDealt(int damageDealt) { this.damageDealt = damageDealt; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public int getShopPurchases() { return shopPurchases; }
    public void setShopPurchases(int shopPurchases) { this.shopPurchases = shopPurchases; }

    public int getMessagesSent() { return messagesSent; }
    public void setMessagesSent(int messagesSent) { this.messagesSent = messagesSent; }

    public boolean isNoUnfinishedTasks() { return noUnfinishedTasks; }
    public void setNoUnfinishedTasks(boolean noUnfinishedTasks) { this.noUnfinishedTasks = noUnfinishedTasks; }
}
