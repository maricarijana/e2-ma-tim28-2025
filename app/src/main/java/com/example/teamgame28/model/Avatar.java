package com.example.teamgame28.model;

public class Avatar {
    private String name;
    private int imageResId; // npr. R.drawable.avatar1

    public Avatar(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
