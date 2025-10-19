package com.example.teamgame28.model;

public class Clothing extends Equipment {
    private double ppBoostPercent;       // npr. rukavice +10% PP
    private double successChanceBoost;   // npr. štit +10% šanse uspeha
    private double extraAttackChance;    // npr. čizme +40% šanse za dodatni napad
    private int battlesRemaining;        // traje 2 borbe

    public Clothing() {
        super();
        this.type = EquipmentType.CLOTHING;
        this.battlesRemaining = 2;
    }

    @Override
    public void applyEffect(UserProfile user) {
        int boost = (int)(user.getPowerPoints() * ppBoostPercent);
        user.setPowerPoints(user.getPowerPoints() + boost);
        active = true;
    }

    public void decreaseBattleDuration() {
        battlesRemaining--;
        if (battlesRemaining <= 0) {
            active = false;
        }
    }
    public Clothing(String id, String name, int cost,
                    double ppBoostPercent,
                    double successChanceBoost,
                    double extraAttackChance,
                    int imageResId) {
        super(id, name, EquipmentType.CLOTHING,cost, imageResId);
        this.ppBoostPercent = ppBoostPercent;
        this.successChanceBoost = successChanceBoost;
        this.extraAttackChance = extraAttackChance;
        this.battlesRemaining = 2; // default traje 2 borbe
    }

    // Getteri/setteri
    public double getPpBoostPercent() { return ppBoostPercent; }
    public void setPpBoostPercent(double ppBoostPercent) { this.ppBoostPercent = ppBoostPercent; }

    public double getSuccessChanceBoost() { return successChanceBoost; }
    public void setSuccessChanceBoost(double successChanceBoost) { this.successChanceBoost = successChanceBoost; }

    public double getExtraAttackChance() { return extraAttackChance; }
    public void setExtraAttackChance(double extraAttackChance) { this.extraAttackChance = extraAttackChance; }

    public int getBattlesRemaining() { return battlesRemaining; }
    public void setBattlesRemaining(int battlesRemaining) { this.battlesRemaining = battlesRemaining; }
}
