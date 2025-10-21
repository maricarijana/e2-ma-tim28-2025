package com.example.teamgame28.model;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String toUserId;
    private long timestamp;
    private String status; // PENDING, ACCEPTED, REJECTED

    public FriendRequest() {}

    public FriendRequest(String id, String fromUserId, String toUserId, long timestamp, String status) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
