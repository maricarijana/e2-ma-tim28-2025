package com.example.teamgame28.model;

public class AllianceMembership {
    private String id;
    private String userId;
    private String allianceId;
    private boolean isLeader;
    private long joinedAt;

    public AllianceMembership() {}

    public AllianceMembership(String id, String userId, String allianceId, boolean isLeader, long joinedAt) {
        this.id = id;
        this.userId = userId;
        this.allianceId = allianceId;
        this.isLeader = isLeader;
        this.joinedAt = joinedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public boolean isLeader() { return isLeader; }
    public void setLeader(boolean leader) { isLeader = leader; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}
