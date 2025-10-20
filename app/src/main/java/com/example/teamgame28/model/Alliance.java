package com.example.teamgame28.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Alliance implements Serializable {
    private String id;
    private String name;
    private String leaderId;
    private List<String> members;        // userId lista
    private List<String> pendingInvites; // userId lista pozvanih

    private Timestamp createdAt;

    public Alliance() {
        this.members = new ArrayList<>();
        this.pendingInvites = new ArrayList<>();
    }

    public Alliance(String id, String name, String leaderId, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.createdAt = createdAt;
        this.members = new ArrayList<>();
        this.pendingInvites = new ArrayList<>();
    }

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public List<String> getPendingInvites() { return pendingInvites; }
    public void setPendingInvites(List<String> pendingInvites) { this.pendingInvites = pendingInvites; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
