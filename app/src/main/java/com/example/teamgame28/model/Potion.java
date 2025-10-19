package com.example.teamgame28.model;

public class Potion extends Equipment {
    private PotionType potionType;
    private double ppBoostPercent; // npr. 0.2 = +20% PP
    private boolean consumed;


    public Potion() {
        super();
        this.type = EquipmentType.POTION;
    }

    public Potion(String id, String name, int cost, PotionType potionType,
                  double ppBoostPercent, int imageResId) {
        super(id, name, EquipmentType.POTION, cost, imageResId);
        this.potionType = potionType;
        this.ppBoostPercent = ppBoostPercent;
        this.consumed = false;
    }

    @Override
    public void applyEffect(UserProfile user) {
        // Logika je prebačena u EquipmentService.activateEquipment()
        // i calculateEquipmentBoosts()
    }

    // Getteri i setteri
    public PotionType getPotionType() { return potionType; }
    public void setPotionType(PotionType potionType) { this.potionType = potionType; }

    public double getPpBoostPercent() { return ppBoostPercent; }
    public void setPpBoostPercent(double ppBoostPercent) { this.ppBoostPercent = ppBoostPercent; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }
}
