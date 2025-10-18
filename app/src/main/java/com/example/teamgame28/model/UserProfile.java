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
    // VAŽNO: powerPoints je TRAJNI BOOST od PERMANENT potions, ne bazni PP!
    // Bazni PP se računa iz nivoa (LevelingService.getTotalPpForLevel(level))
    // Ukupan PP = bazni PP (iz nivoa) + powerPoints (trajni boost)
    private int powerPoints;
    private int coins;
    private List<String> badges;

    // VAŽNO: Firestore ne može deserijalizovati List<Equipment> jer je Equipment abstract!
    // Zato delimo liste po konkretnim tipovima:
    private List<Potion> ownedPotions;
    private List<Clothing> ownedClothing;
    private List<Weapon> ownedWeapons;

    private List<Potion> activePotions;
    private List<Clothing> activeClothing;
    private List<Weapon> activeWeapons;


    // Statistika
    private int activeDays; // Broj dana aktivnog korišćenja (streak)
    private long lastLoginTime; // Poslednji login timestamp
    private long currentLevelStartTimestamp; // Timestamp kada je korisnik dostigao trenutni nivo (za etape)
    private Map<String, Integer> xpHistory; // XP po danima (key: datum u formatu "yyyy-MM-dd", value: XP)

    public UserProfile() {
        this.level = 0; // Počinje od 0, prvi level se dostiže sa 200 XP
        this.title = "Početnik";
        this.xp = 0;
        this.powerPoints = 0;
        this.coins = 0;
        this.badges = new ArrayList<>();
        this.currentEquipment = "";
        this.qrCode = "";
        this.activeDays = 0;
        this.lastLoginTime = 0;
        this.currentLevelStartTimestamp = System.currentTimeMillis(); // Početak etape
        this.ownedPotions = new ArrayList<>();
        this.ownedClothing = new ArrayList<>();
        this.ownedWeapons = new ArrayList<>();
        this.activePotions = new ArrayList<>();
        this.activeClothing = new ArrayList<>();
        this.activeWeapons = new ArrayList<>();
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

    public int getActiveDays() { return activeDays; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }

    public long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(long lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public long getCurrentLevelStartTimestamp() { return currentLevelStartTimestamp; }
    public void setCurrentLevelStartTimestamp(long currentLevelStartTimestamp) { this.currentLevelStartTimestamp = currentLevelStartTimestamp; }

    public Map<String, Integer> getXpHistory() { return xpHistory; }
    public void setXpHistory(Map<String, Integer> xpHistory) { this.xpHistory = xpHistory; }

    // Getteri/setteri za owned equipment
    public List<Potion> getOwnedPotions() { return ownedPotions; }
    public void setOwnedPotions(List<Potion> ownedPotions) { this.ownedPotions = ownedPotions; }

    public List<Clothing> getOwnedClothing() { return ownedClothing; }
    public void setOwnedClothing(List<Clothing> ownedClothing) { this.ownedClothing = ownedClothing; }

    public List<Weapon> getOwnedWeapons() { return ownedWeapons; }
    public void setOwnedWeapons(List<Weapon> ownedWeapons) { this.ownedWeapons = ownedWeapons; }

    // Getteri/setteri za active equipment
    public List<Potion> getActivePotions() { return activePotions; }
    public void setActivePotions(List<Potion> activePotions) { this.activePotions = activePotions; }

    public List<Clothing> getActiveClothing() { return activeClothing; }
    public void setActiveClothing(List<Clothing> activeClothing) { this.activeClothing = activeClothing; }

    public List<Weapon> getActiveWeapons() { return activeWeapons; }
    public void setActiveWeapons(List<Weapon> activeWeapons) { this.activeWeapons = activeWeapons; }

    // Helper metode za dobijanje svih equipment kao jedne liste (za kompatibilnost)
    public List<Equipment> getOwnedEquipment() {
        List<Equipment> all = new ArrayList<>();
        if (ownedPotions != null) all.addAll(ownedPotions);
        if (ownedClothing != null) all.addAll(ownedClothing);
        if (ownedWeapons != null) all.addAll(ownedWeapons);
        return all;
    }

    public List<Equipment> getActiveEquipment() {
        List<Equipment> all = new ArrayList<>();
        if (activePotions != null) all.addAll(activePotions);
        if (activeClothing != null) all.addAll(activeClothing);
        if (activeWeapons != null) all.addAll(activeWeapons);
        return all;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void addBadge(String badge) {
        if (!this.badges.contains(badge)) {
            this.badges.add(badge);
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
