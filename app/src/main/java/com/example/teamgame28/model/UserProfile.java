package com.example.teamgame28.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfile implements Serializable {

    // Javno vidljivi podaci
    private int level;
    private String title;
    private int xp; // Ukupan XP ikad sakupljen
    private String qrCode;
    private String currentEquipment;

    // Privatno vidljivi podaci (samo vlasniku)
    private int powerPoints;
    private int coins;
    private List<String> badges;
   //zameniti ovo sa pravom listom equipmenta
    private List<String> equipment;

    private List<Equipment> ownedEquipment;  // sva oprema koju korisnik poseduje
    private List<Equipment> activeEquipment; // trenutno aktivna oprema (max 1 potion, više armora i oružja)


    // Statistika
    private int activeDays; // Broj dana aktivnog korišćenja (streak)
    private long lastLoginTime; // Poslednji login timestamp
    private Map<String, Integer> xpHistory; // XP po danima (key: datum u formatu "yyyy-MM-dd", value: XP)

    public UserProfile() {
        this.level = 0; // Počinje od 0, prvi level se dostiže sa 200 XP
        this.title = "Početnik";
        this.xp = 0;
        this.powerPoints = 0;
        this.coins = 0;
        this.badges = new ArrayList<>();
        this.equipment = new ArrayList<>();
        this.currentEquipment = "";
        this.qrCode = "";
        this.activeDays = 0;
        this.lastLoginTime = 0;
        this.ownedEquipment = new ArrayList<>();
        this.activeEquipment = new ArrayList<>();
        this.xpHistory = new HashMap<>();
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

    public int getActiveDays() { return activeDays; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }

    public long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(long lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Map<String, Integer> getXpHistory() { return xpHistory; }
    public void setXpHistory(Map<String, Integer> xpHistory) { this.xpHistory = xpHistory; }
    public List<Equipment> getOwnedEquipment() {
        return ownedEquipment;
    }

    public List<Equipment> getActiveEquipment() {
        return activeEquipment;
    }

    public void setOwnedEquipment(List<Equipment> ownedEquipment) {
        this.ownedEquipment = ownedEquipment;
    }

    public void setActiveEquipment(List<Equipment> activeEquipment) {
        this.activeEquipment = activeEquipment;
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

    /**
     * Helper metoda za određivanje titule na osnovu nivoa.
     * Svaki nivo ima svoju jedinstvenu titulu.
     * Poziva se iz UserRepository nakon level up-a.
     */
    public void updateTitle() {
        switch (level) {
            case 0:
                this.title = "Početnik";
                break;
            case 1:
                this.title = "Učenik";
                break;
            case 2:
                this.title = "Borac";
                break;
            case 3:
                this.title = "Ratnik";
                break;
            default:
                // Za nivoe iznad 3, koristimo generičku titulu
                this.title = "Ratnik Nivo " + level;
                break;
        }
    }


}
