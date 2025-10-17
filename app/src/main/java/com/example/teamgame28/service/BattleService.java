package com.example.teamgame28.service;

import com.example.teamgame28.model.BattleResult;
import com.example.teamgame28.model.Boss;
import com.example.teamgame28.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.teamgame28.service.BattleResultService;

import java.util.Random;

public class BattleService {

    private final BossService bossService;
    private final Random random;
    private final UserRepository userRepository;
    private final BattleResultService battleResultService;

    public BattleService(BossService bossService) {
        this.bossService = bossService;
        this.random = new Random();
        this.userRepository = new UserRepository();
        this.battleResultService = new BattleResultService();
    }

    /**
     * Kreira novog bosa na osnovu nivoa
     * Prvi bos ima 200 HP
     * Svaki naredni: HP = HP prethodnog * 2 + HP prethodnog / 2
     */
    public Boss createBoss(int bossLevel, Boss previousBoss) {
        Boss boss = new Boss();
        boss.setBossLevel(bossLevel);

        int hp;
        if (bossLevel == 1) {
            hp = 200;
        } else {
            int previousHP = previousBoss.getHp();
            hp = previousHP * 2 + previousHP / 2;
        }

        boss.setHp(hp);
        boss.setCurrentHP(hp);
        boss.setDefeated(false);
        boss.setAttemptedThisLevel(false);

        // Računanje nagrade
        int coinsReward;
        if (bossLevel == 1) {
            coinsReward = 200;
        } else {
            coinsReward = (int) (previousBoss.getCoinsReward() * 1.2);
        }

        boss.setCoinsReward(coinsReward);
        boss.setCoinsRewardPercent(1.0);

        return boss;
    }

    /**
     * Izvršava napad na bosa
     */
    public boolean performAttack(Boss boss, int playerPP, double successRate) {
        int randomNumber = random.nextInt(101);

        if (randomNumber < successRate) {
            int newHP = Math.max(0, boss.getCurrentHP() - playerPP);
            boss.setCurrentHP(newHP);

            if (newHP == 0) {
                boss.setDefeated(true);
            }

            return true; // Uspešan napad
        }

        return false; // Promašaj
    }

    /**
     * Računa nagrade nakon borbe i ažurira korisnika u Firestore
     */
    public BattleResult calculateRewards(Boss boss, int attacksRemaining) {
        BattleResult result = new BattleResult();
        result.setBossDefeated(boss.getDefeated());

        String userId = getCurrentUserId();

        if (boss.getDefeated()) {
            result.setCoinsEarned(boss.getCoinsReward());
            result.setEquipmentChance(0.20); // 20% šanse za opremu
        } else {
            double hpPercent = (double) boss.getCurrentHP() / boss.getHp();
            if (hpPercent <= 0.5) {
                result.setCoinsEarned(boss.getCoinsReward() / 2);
                result.setEquipmentChance(0.10);
            } else {
                result.setCoinsEarned(0);
                result.setEquipmentChance(0.0);
            }
        }

        // Proveri da li se dobija oprema
        result.setEquipmentDropped(rollForEquipment(result.getEquipmentChance()));
        if (result.isEquipmentDropped()) {
            result.setWeapon(random.nextInt(100) < 5); // 5% šansa za oružje
        }

        // 🔹 Ažuriraj korisnika u Firestore
        if (userId != null) {
            userRepository.addCoinsToUser(userId, result.getCoinsEarned());
            if (result.isEquipmentDropped()) {
                userRepository.addEquipmentToUser(userId, result.isWeapon() ? "Weapon" : "Armor");
            }
            battleResultService.saveBattleResult(result);
        }

        return result;
    }

    /**
     * Određuje da li će pasti oprema
     */
    private boolean rollForEquipment(double chance) {
        if (chance <= 0.0) return false;
        return random.nextDouble() < chance;
    }

    /**
     * Dohvata trenutno prijavljenog korisnika
     */
    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }
}
