package com.example.teamgame28.service;

import com.example.teamgame28.model.BattleData;
import com.example.teamgame28.model.BattleResult;
import com.example.teamgame28.model.Boss;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.EquipmentBoosts;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.example.teamgame28.repository.EquipmentRepository;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.viewmodels.EquipmentViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.teamgame28.service.BattleResultService;

import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleService {

    private final BossService bossService;
    private final Random random;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final BattleResultService battleResultService;
    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;

    public BattleService(BossService bossService, Context context) {
        this.bossService = bossService;
        this.random = new Random();
        this.userRepository = new UserRepository();
        this.taskRepository = TaskRepository.getInstance(context);
        this.battleResultService = new BattleResultService();
        this.equipmentService = new EquipmentService();
        this.equipmentRepository= EquipmentRepository.getInstance();
    }

    private int calculateBossHPForLevel(int level) {
        // Zaštita od beskonačne rekurzije
        if (level <= 0) {
            return 200; // Default level 1
        }
        if (level == 1) {
            return 200;
        }
        int previousHP = calculateBossHPForLevel(level - 1);
        return previousHP * 2 + previousHP / 2;
    }

    /**
     * Rekurzivno računa nagradu bossa za određeni nivo.
     * Level 1: 200 coina
     * Level N: Reward(N-1) * 1.2
     */
    private int calculateBossRewardForLevel(int level) {
        // Zaštita od beskonačne rekurzije
        if (level <= 0) {
            return 200; // Default level 1
        }
        if (level == 1) {
            return 200;
        }
        int previousReward = calculateBossRewardForLevel(level - 1);
        return (int) (previousReward * 1.2);
    }

    /**
     * Izvršava napad na bosa
     */
    public boolean performAttack(Boss boss, int playerPP, double successRate) {
        int randomNumber = random.nextInt(101);

        android.util.Log.d("BattleService", "🎯 Attack attempt:");
        android.util.Log.d("BattleService", "  - Player PP (damage): " + playerPP);
        android.util.Log.d("BattleService", "  - Success rate: " + successRate + "%");
        android.util.Log.d("BattleService", "  - Random roll: " + randomNumber);
        android.util.Log.d("BattleService", "  - Boss HP before: " + boss.getCurrentHP() + "/" + boss.getHp());

        if (randomNumber < successRate) {
            int oldHP = boss.getCurrentHP();
            int newHP = Math.max(0, boss.getCurrentHP() - playerPP);
            boss.setCurrentHP(newHP);

            android.util.Log.d("BattleService", "  ✅ HIT! Dealt " + playerPP + " damage");
            android.util.Log.d("BattleService", "  - Boss HP after: " + newHP + "/" + boss.getHp() + " (reduced by " + (oldHP - newHP) + ")");

            if (newHP == 0) {
                boss.setDefeated(true);
                android.util.Log.d("BattleService", "  💀 BOSS DEFEATED!");
            }

            return true; // Uspešan napad
        }

        android.util.Log.d("BattleService", "  ❌ MISS! No damage dealt");
        return false; // Promašaj
    }

    /**
     * Računa nagrade nakon borbe i ažurira korisnika u Firestore
     * VAŽNO: Ova metoda je asinkrona! Koristi callback.
     */
    public void calculateRewards(Boss boss, int attacksRemaining, RewardsCallback callback) {
        BattleResult result = new BattleResult();
        result.setBossDefeated(boss.getDefeated());

        String userId = getCurrentUserId();

        android.util.Log.d("BattleService", "========== CALCULATING REWARDS ==========");
        android.util.Log.d("BattleService", "Boss defeated: " + boss.getDefeated());
        android.util.Log.d("BattleService", "Boss HP: " + boss.getCurrentHP() + " / " + boss.getHp());
        android.util.Log.d("BattleService", "Boss coins reward: " + boss.getCoinsReward());

        // Učitaj coin boost od weapon-a
        calculateCoinBoostAndRewards(boss, result, userId, callback);
    }

    private void calculateCoinBoostAndRewards(Boss boss, BattleResult result, String userId, RewardsCallback callback) {
        // Učitaj user profil da primenimo coin boost
        userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                // Izračunaj coin boost od aktivnih weapon-a
                EquipmentBoosts boosts = equipmentService.calculateEquipmentBoosts(
                        userProfile.getPowerPoints(),
                        userProfile.getActiveEquipment()
                );
                double coinBoostPercent = boosts.getCoinBoost();

                int baseCoins;
                if (boss.getDefeated()) {
                    baseCoins = boss.getCoinsReward();
                    result.setEquipmentChance(0.99); // 20% šanse za opremu
                } else {
                    double hpPercent = (double) boss.getCurrentHP() / boss.getHp();
                    if (hpPercent <= 0.5) {
                        baseCoins = boss.getCoinsReward() / 2;
                        result.setEquipmentChance(0.10);
                    } else {
                        baseCoins = 0;
                        result.setEquipmentChance(0.0);
                    }
                }

                // Primeni coin boost
                int finalCoins = (int) (baseCoins * (1 + coinBoostPercent));
                result.setCoinsEarned(finalCoins);

                android.util.Log.d("BattleService", "💰 Coins: " + baseCoins + " base + " + (coinBoostPercent * 100) + "% boost = " + finalCoins + " total");

                continueCalculateRewards(result, userId, callback);
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("BattleService", "❌ Greška pri učitavanju profila za coin boost", e);
                // Fallback - bez boosta
                if (boss.getDefeated()) {
                    result.setCoinsEarned(boss.getCoinsReward());
                    result.setEquipmentChance(0.99);
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
                continueCalculateRewards(result, userId, callback);
            }
        });
    }

    private void continueCalculateRewards(BattleResult result, String userId, RewardsCallback callback) {

        // Proveri da li se dobija oprema (20% šansa)
        result.setEquipmentDropped(rollForEquipment(result.getEquipmentChance()));
        Equipment droppedEquipment = null;

        if (result.isEquipmentDropped()) {
            boolean isWeapon = random.nextInt(100) < 5; // AKO padne oprema: 5% weapon, 95% clothing
            result.setWeapon(isWeapon);

            // 🔹 Izaberi KONKRETAN equipment iz static store!
            if (isWeapon) {
                List<Weapon> weapons = com.example.teamgame28.staticData.WeaponStore.getWeapons();
                Weapon randomWeapon = weapons.get(random.nextInt(weapons.size()));

                // 🚀 Kreiraj novu instancu za Firestore
                droppedEquipment = new Weapon(
                        randomWeapon.getId(),
                        randomWeapon.getName(),
                        randomWeapon.getCost(),
                        randomWeapon.getPpBoostPercent(),
                        randomWeapon.getCoinBoostPercent(),
                        randomWeapon.getImageResId()
                );

                result.setEquipmentId(randomWeapon.getId());
                result.setEquipmentName(randomWeapon.getName());
                result.setEquipmentImageResId(randomWeapon.getImageResId());
                Log.d("BattleService", "🗡️ Weapon dropped: " + randomWeapon.getName());
            } else {
                List<Clothing> clothes = com.example.teamgame28.staticData.ClothingStore.getClothes();
                Clothing randomClothing = clothes.get(random.nextInt(clothes.size()));

                // 🚀 Kreiraj novu instancu za Firestore
                droppedEquipment = new Clothing(
                        randomClothing.getId(),
                        randomClothing.getName(),
                        randomClothing.getCost(),
                        randomClothing.getPpBoostPercent(),
                        randomClothing.getSuccessChanceBoost(),
                        randomClothing.getExtraAttackChance(),
                        randomClothing.getImageResId()
                );

                result.setEquipmentId(randomClothing.getId());
                result.setEquipmentName(randomClothing.getName());
                result.setEquipmentImageResId(randomClothing.getImageResId());
                Log.d("BattleService", "🛡️ Clothing dropped: " + randomClothing.getName());
            }


        }

        android.util.Log.d("BattleService", "💰 Total rewards: " + result.getCoinsEarned() + " coins, equipment: " + result.isEquipmentDropped());

        // 🔹 ODMAH vrati rezultat, a Firestore update uradi u POZADINI
        android.util.Log.d("BattleService", "🎁 Calling success callback IMMEDIATELY with result: " + result.getCoinsEarned() + " coins");
        callback.onSuccess(result);

        // 🔹 U POZADINI dodaj coinse i equipment u Firestore (ne blokiraj UI)
        if (userId != null && result.getCoinsEarned() > 0) {
            android.util.Log.d("BattleService", "💾 [BACKGROUND] Adding " + result.getCoinsEarned() + " coins to user " + userId);
            userRepository.addCoinsToUser(userId, result.getCoinsEarned(), new UserRepository.CoinsCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("BattleService", "✅ [BACKGROUND] Coins added to Firestore");
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("BattleService", "❌ [BACKGROUND] Failed to add coins: " + e.getMessage());
                }
            });
        }

        if (result.isEquipmentDropped() && userId != null && droppedEquipment != null) {
            android.util.Log.d("BattleService", "🎒 [BACKGROUND] Adding equipment to user: " + droppedEquipment.getName());
            // Dodaj PRAVI equipment objekat u bazu (drop iz borbe - ne menjaj coinse!)
            equipmentRepository.addEquipmentAtomically(userId, droppedEquipment);

        } else {
            android.util.Log.w("BattleService", "⚠️ Equipment nije dodat:");
            android.util.Log.w("BattleService", "  - equipmentDropped: " + result.isEquipmentDropped());
            android.util.Log.w("BattleService", "  - userId: " + (userId != null ? userId : "NULL"));
            android.util.Log.w("BattleService", "  - droppedEquipment: " + (droppedEquipment != null ? droppedEquipment.getName() : "NULL"));
        }

        battleResultService.saveBattleResult(result);
    }

    /**
     * Određuje da li će pasti oprema
     */
    private boolean rollForEquipment(double chance) {
        if (chance <= 0.0) return false;
        return random.nextDouble() < chance;
    }

    /**
     * Priprema sve podatke potrebne za pokretanje borbe sa bosom.
     * Učitava UserProfile, izračunava boostove od opreme, i vraća BattleData.
     *
     * @param userId ID korisnika
     * @param callback rezultat sa BattleData objektom
     */
    public void prepareBattleData(String userId, PrepareBattleCallback callback) {
        // Učitaj UserProfile iz Firestore
        userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (userProfile == null) {
                    callback.onFailure("UserProfile nije pronađen");
                    return;
                }

                // VAŽNO: Preračunaj nivo iz XP-a jer Firestore možda ima zastareli nivo
                int userLevel = LevelingService.calculateLevelFromXp(userProfile.getXp());

                // VAŽNO: Preračunaj PP iz nivoa
                int levelBasedPP = LevelingService.getTotalPpForLevel(userLevel);

                // VAŽNO: Dodaj trajne boostove od PERMANENT potions
                int permanentBoost = userProfile.getPowerPoints(); // Trajno povećan PP
                int basePP = levelBasedPP + permanentBoost;

                List<Equipment> activeEquipment = userProfile.getActiveEquipment();

                android.util.Log.d("BattleService", "📊 UserProfile loaded:");
                android.util.Log.d("BattleService", "  - XP: " + userProfile.getXp());
                android.util.Log.d("BattleService", "  - Calculated Level: " + userLevel);
                android.util.Log.d("BattleService", "  - Level-based PP: " + levelBasedPP);
                android.util.Log.d("BattleService", "  - Permanent PP Boost: " + permanentBoost);
                android.util.Log.d("BattleService", "  - Total Base PP: " + basePP);
                android.util.Log.d("BattleService", "  - Active Equipment count: " +
                    (activeEquipment != null ? activeEquipment.size() : 0));

                if (activeEquipment == null) {
                    activeEquipment = new ArrayList<>();
                }

                // Izračunaj boostove od opreme
                EquipmentBoosts boosts = equipmentService.calculateEquipmentBoosts(basePP, activeEquipment);

                // Ukupan PP = bazni PP + boost
                int totalPP = basePP + boosts.getPpBoost();

                android.util.Log.d("BattleService", "  - PP Boost: " + boosts.getPpBoost());
                android.util.Log.d("BattleService", "  - Total PP (before assignment): " + totalPP);

                // 🔍 ADDITIONAL DEBUG - proveravamo šta se šalje dalje
                if (totalPP == 0) {
                    android.util.Log.e("BattleService", "⚠️ WARNING: Total PP is 0!");
                    android.util.Log.e("BattleService", "  - Base PP was: " + basePP);
                    android.util.Log.e("BattleService", "  - PP Boost was: " + boosts.getPpBoost());
                }

                // Extra attacks od čizama
                final int extraAttacks = equipmentService.calculateExtraAttacks(activeEquipment);
                final int totalAttacks = 5 + extraAttacks;

                // Imena aktivne opreme za UI
                final String activeEquipmentNames = equipmentService.getActiveEquipmentNames(activeEquipment);

                // Success rate = stopa uspešnosti iz trenutne etape + boost od štita
                final double successBoost = boosts.getSuccessBoost() * 100; // 0.1 → 10%
                final int finalTotalPP = totalPP;
                final int finalUserLevel = userLevel;

                // Dohvati success rate iz trenutne etape (koristi postojeću logiku iz TaskRepository)
                long stageStartTimestamp = userProfile.getCurrentLevelStartTimestamp();
                taskRepository.getSuccessRateForCurrentStage(userId, stageStartTimestamp, new TaskRepository.SuccessRateCallback() {
                    @Override
                    public void onSuccess(double baseSuccessRate) {
                        android.util.Log.d("BattleService", "📊 Stage success rate: " + baseSuccessRate + "%");

                        double totalSuccessRate = baseSuccessRate + successBoost;
                        android.util.Log.d("BattleService", "📊 Total success rate (with equipment): " + totalSuccessRate + "%");

                        // 🔥 LOGIKA NEPOBEĐENIH BOSOVA - Dohvati bosa sa kojim treba da se boriš
                        bossService.getBossForBattle(userId, finalUserLevel, new BossService.BossCallback() {
                            @Override
                            public void onSuccess(Boss boss, boolean isExistingBoss) {
                                android.util.Log.d("BattleService", isExistingBoss ?
                                    "🔴 Fighting UNDEFEATED boss (Level " + boss.getBossLevel() + ")" :
                                    "✅ Fighting NEW boss (Level " + boss.getBossLevel() + ")");

                                // Kreiraj BattleData objekat sa Boss podacima
                                BattleData battleData = new BattleData(
                                    boss.getBossLevel(),
                                    finalTotalPP,
                                    totalSuccessRate,
                                    totalAttacks,
                                    activeEquipmentNames,
                                    boss.getId(),  // Boss ID iz Firestore (null ako je novi boss)
                                    boss.getHp(),
                                    boss.getCurrentHP(),
                                    boss.getCoinsReward(),
                                    isExistingBoss
                                );

                                android.util.Log.d("BattleService", "✅ BattleData created: " + battleData.toString());
                                callback.onSuccess(battleData);
                            }

                            @Override
                            public void onFailure(String error) {
                                android.util.Log.e("BattleService", "❌ Failed to get boss: " + error);
                                callback.onFailure("Failed to load boss: " + error);
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("BattleService", "❌ Failed to load UserProfile: " + e.getMessage());
                callback.onFailure("Greška pri učitavanju profila: " + e.getMessage());
            }
        });
    }

    /**
     * Dohvata trenutno prijavljenog korisnika
     */
    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    // ========== CALLBACK INTERFACES ==========

    public interface PrepareBattleCallback {
        void onSuccess(BattleData battleData);
        void onFailure(String error);
    }

    public interface RewardsCallback {
        void onSuccess(BattleResult result);
        void onFailure(Exception e);
    }
}
