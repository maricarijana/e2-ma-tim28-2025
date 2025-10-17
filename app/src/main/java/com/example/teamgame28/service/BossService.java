package com.example.teamgame28.service;

import androidx.lifecycle.LiveData;

import com.example.teamgame28.model.Boss;
import com.example.teamgame28.repository.BossRepository;

import java.util.List;

public class BossService {

    private BossRepository bossRepository;

    public BossService(BossRepository bossRepository) {
        this.bossRepository = bossRepository;
    }

    /**
     * Kreira novog bosa ili vraća postojećeg neporaženog
     * Logika: Nakon svakog nivoa korisnik se suočava sa bosom
     */
    public Boss getOrCreateBossForLevel(String userId, int currentLevel, Boss previousBoss) {
        // Ovo se poziva iz Activity/ViewModel gde se čeka LiveData rezultat
        // Ovde vraćamo Boss objekat za kreiranje

        int bossLevel = calculateBossLevel(currentLevel);
        Boss newBoss = new Boss();
        newBoss.setUserId(userId);
        newBoss.setBossLevel(bossLevel);

        // Računanje HP
        int hp;
        if (previousBoss == null || bossLevel == 1) {
            hp = 200; // Prvi bos ima 200 HP
        } else {
            int previousHP = previousBoss.getHp();
            hp = previousHP * 2 + previousHP / 2;
        }

        newBoss.setHp(hp);
        newBoss.setCurrentHP(hp);
        newBoss.setDefeated(false);
        newBoss.setAttemptedThisLevel(false);

        // Računanje nagrade novčića
        int coinsReward;
        if (previousBoss == null || bossLevel == 1) {
            coinsReward = 200; // Prvi bos daje 200 novčića
        } else {
            coinsReward = (int) (previousBoss.getCoinsReward() * 1.2); // 20% više nego prethodni
        }
        newBoss.setCoinsReward(coinsReward);

        return newBoss;
    }

    /**
     * Vraća trenutnog neporaženog bosa za korisnika
     */
    public LiveData<Boss> getCurrentUndefeatedBoss(String userId) {
        return bossRepository.getCurrentUndefeatedBoss(userId);
    }

    /**
     * Vraća poslednjeg poraženog bosa za korisnika
     */
    public LiveData<Boss> getLastDefeatedBoss(String userId) {
        return bossRepository.getLastDefeatedBoss(userId);
    }

    /**
     * Vraća sve bosove za korisnika
     */
    public LiveData<List<Boss>> getAllBosses(String userId) {
        return bossRepository.getAllBossesByUserId(userId);
    }

    /**
     * Dodaje novog bosa
     */
    public LiveData<String> addBoss(Boss boss) {
        return bossRepository.insertBoss(boss);
    }

    /**
     * Ažurira bosa nakon borbe
     */
    public LiveData<Boolean> updateBossAfterBattle(String bossId, Boss boss) {
        return bossRepository.updateBoss(bossId, boss);
    }

    /**
     * Izračunava nivo bosa na osnovu trenutnog nivoa korisnika
     * Logika: Svaki nivo ima svog bosa
     */
    private int calculateBossLevel(int playerLevel) {
        return playerLevel;
    }

    /**
     * Proverava da li korisnik treba da se bori sa bosom nakon ovog nivoa
     * Logika: Svaki neporaženi bos se ponovo pojavljuje nakon sledećeg nivoa
     */
    public boolean shouldFaceBoss(int currentLevel, List<Boss> allBosses) {
        // Proveri da li postoji neporaženi bos za ovaj ili prethodni nivo
        for (Boss boss : allBosses) {
            if (!boss.getDefeated() && boss.getBossLevel() <= currentLevel) {
                return true;
            }
        }
        return true; // Uvek se pojavljuje bos nakon nivoa
    }
}
