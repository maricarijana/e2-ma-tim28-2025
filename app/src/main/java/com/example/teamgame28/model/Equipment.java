package com.example.teamgame28.model;

import java.io.Serializable;

public abstract class Equipment implements Serializable {
    protected String id;
    protected String name;
    protected EquipmentType type;
    protected int cost;        // cena u coinima
    protected boolean active;  // da li je trenutno aktivirana
    protected int imageResId;
    public Equipment() {}

    public Equipment(String id, String name, EquipmentType type, int cost,int imageResId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.cost = cost;
        this.active = false;
        this.imageResId = imageResId;
    }

    // Apstraktni metod za efekat opreme
    public abstract void applyEffect(UserProfile user);

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

}
