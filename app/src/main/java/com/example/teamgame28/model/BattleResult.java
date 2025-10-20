package com.example.teamgame28.model;

public  class BattleResult {
    private boolean bossDefeated;
    private int coinsEarned;
    private double equipmentChance;
    private boolean equipmentDropped;
    private boolean isWeapon; // true = oružje, false = odeća
    private String equipmentId; // ID konkretne opreme (npr. "sword", "gloves")
    private String equipmentName; // Ime opreme (npr. "Shield +10% Success Chance")
    private int equipmentImageResId; // Resource ID slike opreme

    public boolean isBossDefeated() {
        return bossDefeated;
    }

    public void setBossDefeated(boolean bossDefeated) {
        this.bossDefeated = bossDefeated;
    }

    public int getCoinsEarned() {
        return coinsEarned;
    }

    public void setCoinsEarned(int coinsEarned) {
        this.coinsEarned = coinsEarned;
    }

    public double getEquipmentChance() {
        return equipmentChance;
    }

    public void setEquipmentChance(double equipmentChance) {
        this.equipmentChance = equipmentChance;
    }

    public boolean isEquipmentDropped() {
        return equipmentDropped;
    }

    public void setEquipmentDropped(boolean equipmentDropped) {
        this.equipmentDropped = equipmentDropped;
    }

    public boolean isWeapon() {
        return isWeapon;
    }

    public void setWeapon(boolean weapon) {
        isWeapon = weapon;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getEquipmentImageResId() {
        return equipmentImageResId;
    }

    public void setEquipmentImageResId(int equipmentImageResId) {
        this.equipmentImageResId = equipmentImageResId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }
}
