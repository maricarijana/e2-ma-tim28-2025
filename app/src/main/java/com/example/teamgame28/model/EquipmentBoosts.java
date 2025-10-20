package com.example.teamgame28.model;

/**
 * Wrapper klasa za privremene boostove od opreme.
 * Ovi boostovi se primenjuju SAMO za trenutnu borbu i ne čuvaju se u Firebase.
 */
public class EquipmentBoosts {
    private int basePP;           // Bazni PP korisnika
    private int ppBoost;          // Privremeni boost PP (od ONETIME napitaka i clothing)
    private double successBoost;  // Boost success rate (od clothing - shield)
    private double extraAttackChance; // Šansa za dodatni napad (od clothing - boots)
    private double coinBoost;     // Boost coin reward (od weapons - bow)

    public EquipmentBoosts(int basePP) {
        this.basePP = basePP;
        this.ppBoost = 0;
        this.successBoost = 0.0;
        this.extraAttackChance = 0.0;
        this.coinBoost = 0.0;
    }

    /**
     * Vraća totalni PP za borbu (bazni + privremeni boost).
     */
    public int getTotalPP() {
        return basePP + ppBoost;
    }

    // Getteri i setteri
    public int getBasePP() { return basePP; }
    public void setBasePP(int basePP) { this.basePP = basePP; }

    public int getPpBoost() { return ppBoost; }
    public void setPpBoost(int ppBoost) { this.ppBoost = ppBoost; }

    public double getSuccessBoost() { return successBoost; }
    public void setSuccessBoost(double successBoost) { this.successBoost = successBoost; }

    public double getExtraAttackChance() { return extraAttackChance; }
    public void setExtraAttackChance(double extraAttackChance) { this.extraAttackChance = extraAttackChance; }

    public double getCoinBoost() { return coinBoost; }
    public void setCoinBoost(double coinBoost) { this.coinBoost = coinBoost; }

    @Override
    public String toString() {
        return "EquipmentBoosts{" +
                "basePP=" + basePP +
                ", ppBoost=" + ppBoost +
                ", totalPP=" + getTotalPP() +
                ", successBoost=" + (successBoost * 100) + "%" +
                ", extraAttackChance=" + (extraAttackChance * 100) + "%" +
                ", coinBoost=" + (coinBoost * 100) + "%" +
                '}';
    }
}
