package com.example.teamgame28.model;

public class Weapon extends Equipment {
    private double ppBoostPercent;
    private double coinBoostPercent;
    private double probability; // verovatnoća efekta
    private int upgradeLevel;

    public Weapon() {
        super();
        this.type = EquipmentType.WEAPON;
        this.upgradeLevel = 0;
        this.probability = 0.0;
    }

    public Weapon(String id, String name, int cost,
                  double ppBoostPercent, double coinBoostPercent,int imageResId) {
        super(id, name, EquipmentType.WEAPON, cost,imageResId);
        this.ppBoostPercent = ppBoostPercent;
        this.coinBoostPercent = coinBoostPercent;
        this.probability = 0.0;
        this.upgradeLevel = 0;
    }

    @Override
    public void applyEffect(UserProfile user) {
        int boost = (int)(user.getPowerPoints() * ppBoostPercent);
        user.setPowerPoints(user.getPowerPoints() + boost);
        active = true;
    }

    public void upgrade() {
        upgradeLevel++;
        probability += 0.01;
    }

    public void addDuplicate() {
        probability += 0.02;
    }

    // Getteri i setteri
    public double getPpBoostPercent() { return ppBoostPercent; }
    public void setPpBoostPercent(double ppBoostPercent) { this.ppBoostPercent = ppBoostPercent; }

    public double getCoinBoostPercent() { return coinBoostPercent; }
    public void setCoinBoostPercent(double coinBoostPercent) { this.coinBoostPercent = coinBoostPercent; }

    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }
}
