package com.example.teamgame28.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String email;
    private String password;
    private String username;
    private String avatar;
    private boolean activated;

    public User() {
        // Firebase traži prazan konstruktor
    }

    public User(String uid, String email, String username, String password, String avatar) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.password = password;
        this.avatar = avatar;
        this.activated = false;
    }



    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
}
