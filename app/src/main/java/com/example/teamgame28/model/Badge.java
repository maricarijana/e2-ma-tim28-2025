package com.example.teamgame28.model;

import java.io.Serializable;

public class Badge implements Serializable {

    private String id;            // ID bedža (može biti auto-generated ili ručno)
    private String name;          // Naziv npr. "Specijalni pobednik"
    private String description;   // Opis npr. "Pobedio saveznog bossa u misiji"
    private int level;            // Nivo bedža (1, 2, 3...)
    private String imageUrl;      // URL ili naziv slike u drawable
    private long earnedAt;        // timestamp kad je osvojio

    // 👇 Dodaj ovo polje — broj puta kada je bedž osvojen
    private int count;

    public Badge() {}

    public Badge(String id, String name, String description, int level, String imageUrl, long earnedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.level = level;
        this.imageUrl = imageUrl;
        this.earnedAt = earnedAt;
        this.count = 1; // podrazumevano 1 kada se prvi put dodeli
    }

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getEarnedAt() { return earnedAt; }
    public void setEarnedAt(long earnedAt) { this.earnedAt = earnedAt; }

    // 👇 Ovo dodaj da radi kod za bedž brojanje u misiji
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
