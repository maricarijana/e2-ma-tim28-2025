package com.example.teamgame28.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String email;
    private String password;
    private String username;
    private String avatar;
    private boolean isActivated;
    private int totalXp; // 🎯 Ukupan XP korisnika

    public User(String uid, String email, String username, String password, String avatar) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.password = password;
        this.avatar = avatar;
        this.isActivated = false; // biće true kad verifikuje email
        this.totalXp = 0; // Počinje sa 0 XP
    }

    public User() {
        this.totalXp = 0;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarName() { return avatar; }
    public void setAvatarName(String avatarName) { this.avatar = avatar; }

    public boolean isActivated() { return isActivated; }
    public void setActivated(boolean registered) { this.isActivated = registered; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    // 🎯 Metoda za dodavanje XP
    public void addXp(int xp) {
        this.totalXp += xp;
    }
}
