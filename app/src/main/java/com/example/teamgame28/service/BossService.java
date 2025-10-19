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

    /**
     * Vraća bosa sa kojim korisnik treba da se bori.
     * LOGIKA "ČEKAONICA":
     * - Ako postoji nepobeđeni boss, on se prikazuje PRE nego što se kreira novi boss.
     * - Nema biranja, nema preskakanja.
     * - Ne možeš dalje dok ne rešiš starog neprijatelja.
     * - Korisnik može da se bori sa bossom JEDNOM PO NIVOU.
     *
     * @param userId ID korisnika
     * @param currentLevel Trenutni nivo korisnika
     * @param callback Callback sa Boss objektom ili null ako nema
     */
    public void getBossForBattle(String userId, int currentLevel, BossCallback callback) {
        // Prvo proveri da li ima nepobeđenih bosova
        bossRepository.getCurrentUndefeatedBossCallback(userId, new BossRepository.BossCallback() {
            @Override
            public void onSuccess(Boss undefeatedBoss) {
                if (undefeatedBoss != null) {
                    // ✅ Postoji nepobeđeni boss
                    android.util.Log.d("BossService", "🔴 Nepobeđeni boss pronađen: Level " + undefeatedBoss.getBossLevel() + ", HP: " + undefeatedBoss.getCurrentHP() + "/" + undefeatedBoss.getHp());
                    android.util.Log.d("BossService", "   lastAttemptedUserLevel: " + undefeatedBoss.getLastAttemptedUserLevel());
                    android.util.Log.d("BossService", "   Boss level: " + undefeatedBoss.getBossLevel());
                    android.util.Log.d("BossService", "   User level: " + currentLevel);

                    // 🔥 KLJUČNA PROVERA: Da li je korisnik već pokušao da se bori sa ovim bossom NA TRENUTNOM NIVOU?
                    if (undefeatedBoss.getLastAttemptedUserLevel() == currentLevel) {
                        // ❌ Korisnik je već pokušao da se bori sa ovim bossom na trenutnom nivou
                        android.util.Log.w("BossService", "⚠️ Korisnik je već pokušao da se bori sa ovim bossom na nivou " + currentLevel);
                        callback.onFailure("Već ste se borili sa bossom na ovom nivou! Pređite na sledeći nivo da biste pokušali ponovo.");
                        return;
                    }

                    // ✅ Korisnik može da se bori - označi pokušaj na trenutnom nivou
                    android.util.Log.d("BossService", "✅ Korisnik može da se bori sa bossom (pokušaj na nivou " + currentLevel + ")");
                    undefeatedBoss.setLastAttemptedUserLevel(currentLevel);

                    // Ažuriraj bossa u Firestore
                    bossRepository.updateBossCallback(undefeatedBoss.getId(), undefeatedBoss, new BossRepository.UpdateBossCallback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("BossService", "✅ Boss ažuriran: lastAttemptedUserLevel = " + currentLevel);
                            callback.onSuccess(undefeatedBoss, true); // true = existing boss
                        }

                        @Override
                        public void onFailure(String error) {
                            android.util.Log.e("BossService", "❌ Greška pri ažuriranju lastAttemptedUserLevel: " + error);
                            // I dalje vrati bossa, ali loguj grešku
                            callback.onSuccess(undefeatedBoss, true);
                        }
                    });
                } else {
                    // ❌ Nema nepobeđenih bosova - kreiraj novog za trenutni nivo
                    android.util.Log.d("BossService", "✅ Nema nepobeđenih bosova, kreiram novog za nivo " + currentLevel);

                    // Dohvati poslednjeg pobeđenog bosa da izračunaš HP/coins
                    bossRepository.getLastDefeatedBossCallback(userId, new BossRepository.BossCallback() {
                        @Override
                        public void onSuccess(Boss lastDefeatedBoss) {
                            Boss newBoss = getOrCreateBossForLevel(userId, currentLevel, lastDefeatedBoss);
                            newBoss.setLastAttemptedUserLevel(currentLevel); // Označi da je pokušano na ovom nivou

                            // 🔥 VAŽNO: Sačuvaj novog bossa u Firestore PRE nego što ga vratiš
                            bossRepository.insertBossCallback(newBoss, new BossRepository.InsertBossCallback() {
                                @Override
                                public void onSuccess(String bossId) {
                                    android.util.Log.d("BossService", "✅ Novi boss sačuvan u Firestore sa ID: " + bossId);
                                    newBoss.setId(bossId); // Postavi ID u Boss objekat
                                    callback.onSuccess(newBoss, false); // false = new boss
                                }

                                @Override
                                public void onFailure(String error) {
                                    android.util.Log.e("BossService", "❌ Greška pri čuvanju bossa: " + error);
                                    callback.onFailure("Greška pri kreiranju bossa: " + error);
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            android.util.Log.e("BossService", "❌ Greška pri dohvatanju poslednjeg bossa: " + error);
                            callback.onFailure("Greška pri dohvatanju podataka: " + error);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("BossService", "❌ Greška pri proveri nepobeđenih bosova: " + error);
                callback.onFailure("Greška pri proveri bosova: " + error);
            }
        });
    }

    /**
     * Ažurira bossa nakon borbe (čuva promene u Firestore).
     *
     * @param boss Boss objekat sa ažuriranim currentHP i isDefeated
     * @param callback Callback za potvrdu uspešnog čuvanja
     */
    public void updateBossAfterBattle(Boss boss, UpdateBossCallback callback) {
        if (boss.getId() == null) {
            android.util.Log.e("BossService", "❌ Boss nema ID, ne mogu da ga ažuriram!");
            callback.onFailure("Boss ID je null");
            return;
        }

        android.util.Log.d("BossService", "💾 Čuvam bossa u Firestore: " + boss.getId());
        android.util.Log.d("BossService", "  - HP: " + boss.getCurrentHP() + "/" + boss.getHp());
        android.util.Log.d("BossService", "  - Defeated: " + boss.getDefeated());

        bossRepository.updateBossCallback(boss.getId(), boss, new BossRepository.UpdateBossCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("BossService", "✅ Boss uspešno ažuriran u Firestore");
                callback.onSuccess();
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("BossService", "❌ Greška pri ažuriranju bossa: " + error);
                callback.onFailure(error);
            }
        });
    }

    /**
     * Callback interface za dohvatanje bosa
     */
    public interface BossCallback {
        void onSuccess(Boss boss, boolean isExistingBoss);
        void onFailure(String error);
    }

    /**
     * Callback interface za ažuriranje bosa
     */
    public interface UpdateBossCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
