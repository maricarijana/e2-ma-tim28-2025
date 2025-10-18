package com.example.teamgame28.model;

/**
 * Model koji drži sve pripremljene podatke za borbu sa bosom.
 * Uključuje bazne podatke korisnika + boostove od opreme.
 */
public class BattleData {
    private int bossLevel;
    private int totalPP;           // bazni PP + boostovi od opreme
    private double successRate;    // bazna stopa + boost od štita
    private int totalAttacks;      // 5 + bonus napadi od čizama
    private String activeEquipmentNames;

    // Boss podaci
    private String bossId;         // Firestore document ID (samo ako je existing boss)
    private int bossHP;            // Max HP bosa
    private int bossCurrentHP;     // Trenutni HP bosa (ako je nepobeđeni)
    private int bossCoinsReward;   // Nagrada novčića
    private boolean isExistingBoss; // Da li je ovo nepobeđeni boss ili nov

    public BattleData() {}

    public BattleData(int bossLevel, int totalPP, double successRate, int totalAttacks, String activeEquipmentNames) {
        this.bossLevel = bossLevel;
        this.totalPP = totalPP;
        this.successRate = successRate;
        this.totalAttacks = totalAttacks;
        this.activeEquipmentNames = activeEquipmentNames;
    }

    public BattleData(int bossLevel, int totalPP, double successRate, int totalAttacks, String activeEquipmentNames,
                      int bossHP, int bossCurrentHP, int bossCoinsReward, boolean isExistingBoss) {
        this.bossLevel = bossLevel;
        this.totalPP = totalPP;
        this.successRate = successRate;
        this.totalAttacks = totalAttacks;
        this.activeEquipmentNames = activeEquipmentNames;
        this.bossHP = bossHP;
        this.bossCurrentHP = bossCurrentHP;
        this.bossCoinsReward = bossCoinsReward;
        this.isExistingBoss = isExistingBoss;
    }

    public BattleData(int bossLevel, int totalPP, double successRate, int totalAttacks, String activeEquipmentNames,
                      String bossId, int bossHP, int bossCurrentHP, int bossCoinsReward, boolean isExistingBoss) {
        this.bossLevel = bossLevel;
        this.totalPP = totalPP;
        this.successRate = successRate;
        this.totalAttacks = totalAttacks;
        this.activeEquipmentNames = activeEquipmentNames;
        this.bossId = bossId;
        this.bossHP = bossHP;
        this.bossCurrentHP = bossCurrentHP;
        this.bossCoinsReward = bossCoinsReward;
        this.isExistingBoss = isExistingBoss;
    }

    public int getBossLevel() {
        return bossLevel;
    }

    public void setBossLevel(int bossLevel) {
        this.bossLevel = bossLevel;
    }

    public int getTotalPP() {
        return totalPP;
    }

    public void setTotalPP(int totalPP) {
        this.totalPP = totalPP;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public int getTotalAttacks() {
        return totalAttacks;
    }

    public void setTotalAttacks(int totalAttacks) {
        this.totalAttacks = totalAttacks;
    }

    public String getActiveEquipmentNames() {
        return activeEquipmentNames;
    }

    public void setActiveEquipmentNames(String activeEquipmentNames) {
        this.activeEquipmentNames = activeEquipmentNames;
    }

    public int getBossHP() {
        return bossHP;
    }

    public void setBossHP(int bossHP) {
        this.bossHP = bossHP;
    }

    public int getBossCurrentHP() {
        return bossCurrentHP;
    }

    public void setBossCurrentHP(int bossCurrentHP) {
        this.bossCurrentHP = bossCurrentHP;
    }

    public int getBossCoinsReward() {
        return bossCoinsReward;
    }

    public void setBossCoinsReward(int bossCoinsReward) {
        this.bossCoinsReward = bossCoinsReward;
    }

    public boolean isExistingBoss() {
        return isExistingBoss;
    }

    public void setExistingBoss(boolean existingBoss) {
        isExistingBoss = existingBoss;
    }

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
    }

    @Override
    public String toString() {
        return "BattleData{" +
                "bossLevel=" + bossLevel +
                ", totalPP=" + totalPP +
                ", successRate=" + successRate +
                ", totalAttacks=" + totalAttacks +
                ", activeEquipment='" + activeEquipmentNames + '\'' +
                ", bossId='" + bossId + '\'' +
                ", bossHP=" + bossHP +
                ", bossCurrentHP=" + bossCurrentHP +
                ", bossCoinsReward=" + bossCoinsReward +
                ", isExistingBoss=" + isExistingBoss +
                '}';
    }
}
