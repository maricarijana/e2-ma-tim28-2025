package com.example.teamgame28.model;

public class Boss {

    private String id;  // Firestore document ID
    private Integer hp;

    private String userId;

    private Integer currentHP;

    private Boolean isDefeated;

    private Integer bossLevel;

    private Integer coinsReward;
    private double coinsRewardPercent;

    private boolean attemptedThisLevel;

    public Boss(Integer HP) {

        this.hp = HP;
        this.attemptedThisLevel = false;
    }

    public Boss() {
    }

    public Boss(String id, Integer hp, String userId, Integer currentHP, Boolean isDefeated, Integer bossLevel, Integer coinsReward, double coinsRewardPercent, boolean attemptedThisLevel) {
        this.id = id;
        this.hp = hp;
        this.userId = userId;
        this.currentHP = currentHP;
        this.isDefeated = isDefeated;
        this.bossLevel = bossLevel;
        this.coinsReward = coinsReward;
        this.coinsRewardPercent = coinsRewardPercent;
        this.attemptedThisLevel = attemptedThisLevel;
    }

    public Integer getHp() {
        return hp;
    }

    public void setHp(Integer hp) {
        this.hp = hp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(Integer currentHP) {
        this.currentHP = currentHP;
    }

    public Boolean getDefeated() {
        return isDefeated;
    }

    public void setDefeated(Boolean defeated) {
        isDefeated = defeated;
    }

    public Integer getBossLevel() {
        return bossLevel;
    }

    public void setBossLevel(Integer bossLevel) {
        this.bossLevel = bossLevel;
    }

    public Integer getCoinsReward() {
        return coinsReward;
    }

    public void setCoinsReward(Integer coinsReward) {
        this.coinsReward = coinsReward;
    }

    public double getCoinsRewardPercent() {
        return coinsRewardPercent;
    }

    public void setCoinsRewardPercent(double coinsRewardPercent) {
        this.coinsRewardPercent = coinsRewardPercent;
    }

    public boolean isAttemptedThisLevel() {
        return attemptedThisLevel;
    }

    public void setAttemptedThisLevel(boolean attemptedThisLevel) {
        this.attemptedThisLevel = attemptedThisLevel;
    }

    @Override
    public String toString() {
        return "Boss{" +
                "id=" + id +
                ", hp=" + hp +
                ", userId='" + userId + '\'' +
                ", currentHP=" + currentHP +
                ", isDefeated=" + isDefeated +
                ", bossLevel=" + bossLevel +
                ", coinsReward=" + coinsReward +
                ", coinsRewardPercent=" + coinsRewardPercent +
                ", attemptedThisLevel=" + attemptedThisLevel +
                '}';
    }
}
