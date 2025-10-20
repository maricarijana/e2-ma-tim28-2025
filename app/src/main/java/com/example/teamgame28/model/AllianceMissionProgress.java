package com.example.teamgame28.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class AllianceMissionProgress implements Serializable {
    private String missionId;
    private String userId;
    private int damageDealt;
    private int tasksCompleted;
    private int shopPurchases;
    private int messagesSent;
    private boolean noUnfinishedTasks;

    private Timestamp lastMessageTimestamp;

    // 🔹 Postojeća nova polja
    private int bossHits; // broj uspešnih udaraca na bossa tokom specijalne misije
    private int daysWithMessages; // broj dana u kojima je korisnik slao poruke savezu

    // 🔹 NOVO POLJE za specijalne zadatke (max 10)
    private int taskPoints; // broj "poena" iz završenih zadataka (ograničeno na 10)

    public AllianceMissionProgress() {}

    public AllianceMissionProgress(String missionId, String userId, int damageDealt, int tasksCompleted,
                                   int shopPurchases, int messagesSent, boolean noUnfinishedTasks) {
        this.missionId = missionId;
        this.userId = userId;
        this.damageDealt = damageDealt;
        this.tasksCompleted = tasksCompleted;
        this.shopPurchases = shopPurchases;
        this.messagesSent = messagesSent;
        this.noUnfinishedTasks = noUnfinishedTasks;
        this.bossHits = 0;
        this.daysWithMessages = 0;
        this.taskPoints = 0;
    }

    // ✅ Getteri i setteri
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

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getBossHits() { return bossHits; }
    public void setBossHits(int bossHits) { this.bossHits = bossHits; }

    public int getDaysWithMessages() { return daysWithMessages; }
    public void setDaysWithMessages(int daysWithMessages) { this.daysWithMessages = daysWithMessages; }

    // 🔹 NOVO: TaskPoints (za misiju)
    public int getTaskPoints() { return taskPoints; }
    public void setTaskPoints(int taskPoints) { this.taskPoints = taskPoints; }
}
