package com.example.teamgame28.model;

import java.io.Serializable;

// ========== ALLIANCE MISSION ==========
public class AllianceMission implements Serializable {
    private String missionId;
    private String allianceId;
    private int bossHp;
    private boolean active;
    private long startTime;
    private long endTime;

    public AllianceMission() {}

    public AllianceMission(String missionId, String allianceId, int bossHp, boolean active, long startTime, long endTime) {
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

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
}
