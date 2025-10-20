package com.example.teamgame28.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

// ========== ALLIANCE MISSION ==========
public class AllianceMission implements Serializable {
    private String missionId;
    private String allianceId;
    private int bossHp;
    private boolean active;
    private Timestamp startTime;
    private Timestamp endTime;
    private Timestamp lastAllianceMessageDate;
    private int allianceMessageDaysCount;

    public Timestamp getLastAllianceMessageDate() { return lastAllianceMessageDate; }
    public void setLastAllianceMessageDate(Timestamp lastAllianceMessageDate) { this.lastAllianceMessageDate = lastAllianceMessageDate; }

    public int getAllianceMessageDaysCount() { return allianceMessageDaysCount; }
    public void setAllianceMessageDaysCount(int allianceMessageDaysCount) { this.allianceMessageDaysCount = allianceMessageDaysCount; }

    public AllianceMission() {}

    public AllianceMission(String missionId, String allianceId, int bossHp, boolean active, Timestamp startTime, Timestamp endTime) {
        this.missionId = missionId;
        this.allianceId = allianceId;
        this.bossHp = bossHp;
        this.active = active;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public int getBossHp() { return bossHp; }
    public void setBossHp(int bossHp) { this.bossHp = bossHp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
}
