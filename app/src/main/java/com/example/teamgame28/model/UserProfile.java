package com.example.teamgame28.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserProfile implements Serializable {

    // Javno vidljivi podaci
    private int level;
    private String title;
    private int xp;
    private String qrCode;
    private String currentEquipment;

    // Privatno vidljivi podaci (samo vlasniku)
    private int powerPoints;
    private int coins;
    private List<String> badges;
    private List<String> equipment;

    public UserProfile() {
        this.level = 1;
        this.title = "Početnik";
        this.xp = 0;
        this.powerPoints = 0;
        this.coins = 0;
        this.badges = new ArrayList<>();
        this.equipment = new ArrayList<>();
        this.currentEquipment = "";
        this.qrCode = "";
    }

    // Getteri i setteri
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getCurrentEquipment() { return currentEquipment; }
    public void setCurrentEquipment(String currentEquipment) { this.currentEquipment = currentEquipment; }

    public int getPowerPoints() { return powerPoints; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }

    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }


    public void addXp(int xpToAdd) {
        this.xp += xpToAdd;
        checkLevelUp();
        // Ovde kasnije možete dodati i logiku za level up
    }
    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void addBadge(String badge) {
        if (!this.badges.contains(badge)) {
            this.badges.add(badge);
        }
    }

    public void addEquipment(String item) {
        if (!this.equipment.contains(item)) {
            this.equipment.add(item);
        }
    }

    private void checkLevelUp() {
        int requiredXp = level * 100;
        if (xp >= requiredXp) {
            level++;
            updateTitle();
        }
    }

    private void updateTitle() {
        if (level >= 50) this.title = "Legenda";
        else if (level >= 30) this.title = "Majstor";
        else if (level >= 20) this.title = "Ekspert";
        else if (level >= 10) this.title = "Ratnik";
        else if (level >= 5) this.title = "Učenik";
        else this.title = "Početnik";
    }


}
