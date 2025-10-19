package com.example.teamgame28.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Boss;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossRepository {

    private FirebaseFirestore db;
    private static final String COLLECTION_NAME = "bosses";

    public BossRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Dodaje novog bosa u Firebase
     */
    public LiveData<String> insertBoss(Boss boss) {
        MutableLiveData<String> result = new MutableLiveData<>();

        Map<String, Object> bossData = new HashMap<>();
        bossData.put("userId", boss.getUserId());
        bossData.put("bossLevel", boss.getBossLevel());
        bossData.put("hp", boss.getHp());
        bossData.put("currentHP", boss.getCurrentHP());
        bossData.put("isDefeated", boss.getDefeated());
        bossData.put("coinsReward", boss.getCoinsReward());
        bossData.put("attemptedThisLevel", boss.isAttemptedThisLevel());
        bossData.put("lastAttemptedUserLevel", boss.getLastAttemptedUserLevel());

        db.collection(COLLECTION_NAME)
                .add(bossData)
                .addOnSuccessListener(documentReference -> result.setValue(documentReference.getId()))
                .addOnFailureListener(e -> result.setValue(null));

        return result;
    }

    /**
     * Ažurira postojećeg bosa
     */
    public LiveData<Boolean> updateBoss(String bossId, Boss boss) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentHP", boss.getCurrentHP());
        updates.put("isDefeated", boss.getDefeated());
        updates.put("attemptedThisLevel", boss.isAttemptedThisLevel());
        updates.put("lastAttemptedUserLevel", boss.getLastAttemptedUserLevel());
        updates.put("coinsRewardPercent", boss.getCoinsRewardPercent());

        db.collection(COLLECTION_NAME)
                .document(bossId)
                .update(updates)
                .addOnSuccessListener(aVoid -> result.setValue(true))
                .addOnFailureListener(e -> result.setValue(false));

        return result;
    }

    /**
     * Pronalazi trenutnog neporaženog bosa za korisnika
     */
    public LiveData<Boss> getCurrentUndefeatedBoss(String userId) {
        MutableLiveData<Boss> result = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", false)
                .orderBy("bossLevel", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Boss boss = documentToBoss(document);
                        result.setValue(boss);
                    } else {
                        result.setValue(null);
                    }
                })
                .addOnFailureListener(e -> result.setValue(null));

        return result;
    }

    /**
     * Pronalazi poslednjeg poraženog bosa za korisnika
     */
    public LiveData<Boss> getLastDefeatedBoss(String userId) {
        MutableLiveData<Boss> result = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", true)
                .orderBy("bossLevel", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Boss boss = documentToBoss(document);
                        result.setValue(boss);
                    } else {
                        result.setValue(null);
                    }
                })
                .addOnFailureListener(e -> result.setValue(null));

        return result;
    }

    /**
     * Pronalazi sve bosove za korisnika
     */
    public LiveData<List<Boss>> getAllBossesByUserId(String userId) {
        MutableLiveData<List<Boss>> result = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("bossLevel", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Boss> bosses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        bosses.add(documentToBoss(document));
                    }
                    result.setValue(bosses);
                })
                .addOnFailureListener(e -> result.setValue(new ArrayList<>()));

        return result;
    }

    /**
     * Callback verzija - pronalazi trenutnog neporaženog bosa
     * Koristi se za battle logiku da izbegne observeForever memory leaks
     */
    public void getCurrentUndefeatedBossCallback(String userId, BossCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", false)
                .orderBy("bossLevel", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Boss boss = documentToBoss(document);
                        callback.onSuccess(boss);
                    } else {
                        callback.onSuccess(null); // Nema nepobeđenih bosova
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Callback verzija - pronalazi poslednjeg poraženog bosa
     * Koristi se za izračunavanje HP/coins novog bossa
     */
    public void getLastDefeatedBossCallback(String userId, BossCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", true)
                .orderBy("bossLevel", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Boss boss = documentToBoss(document);
                        callback.onSuccess(boss);
                    } else {
                        callback.onSuccess(null); // Nema pobeđenih bosova (prvi put)
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Čuva novog bossa u Firestore i vraća njegov ID
     */
    public void insertBossCallback(Boss boss, InsertBossCallback callback) {
        Map<String, Object> bossData = new HashMap<>();
        bossData.put("userId", boss.getUserId());
        bossData.put("bossLevel", boss.getBossLevel());
        bossData.put("hp", boss.getHp());
        bossData.put("currentHP", boss.getCurrentHP());
        bossData.put("isDefeated", boss.getDefeated());
        bossData.put("coinsReward", boss.getCoinsReward());
        bossData.put("attemptedThisLevel", boss.isAttemptedThisLevel());
        bossData.put("lastAttemptedUserLevel", boss.getLastAttemptedUserLevel());

        db.collection(COLLECTION_NAME)
                .add(bossData)
                .addOnSuccessListener(documentReference -> {
                    String bossId = documentReference.getId();
                    boss.setId(bossId); // Postavi ID u Boss objekat
                    android.util.Log.d("BossRepository", "✅ Boss saved with ID: " + bossId);
                    callback.onSuccess(bossId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BossRepository", "❌ Failed to save boss: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Ažurira bossa nakon borbe (currentHP, isDefeated)
     */
    public void updateBossCallback(String bossId, Boss boss, UpdateBossCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentHP", boss.getCurrentHP());
        updates.put("isDefeated", boss.getDefeated());
        updates.put("attemptedThisLevel", boss.isAttemptedThisLevel());
        updates.put("lastAttemptedUserLevel", boss.getLastAttemptedUserLevel());

        db.collection(COLLECTION_NAME)
                .document(bossId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("BossRepository", "✅ Boss updated: " + bossId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BossRepository", "❌ Failed to update boss: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Konvertuje Firebase dokument u Boss objekat
     */
    private Boss documentToBoss(QueryDocumentSnapshot document) {
        Boss boss = new Boss();
        boss.setId(document.getId());  // Postavi Firestore document ID
        boss.setUserId(document.getString("userId"));

        Long bossLevel = document.getLong("bossLevel");
        if (bossLevel != null) boss.setBossLevel(bossLevel.intValue());

        Long hp = document.getLong("hp");
        if (hp != null) boss.setHp(hp.intValue());

        Long currentHP = document.getLong("currentHP");
        if (currentHP != null) boss.setCurrentHP(currentHP.intValue());

        Boolean isDefeated = document.getBoolean("isDefeated");
        if (isDefeated != null) boss.setDefeated(isDefeated);

        Long coinsReward = document.getLong("coinsReward");
        if (coinsReward != null) boss.setCoinsReward(coinsReward.intValue());

        Boolean attemptedThisLevel = document.getBoolean("attemptedThisLevel");
        if (attemptedThisLevel != null) boss.setAttemptedThisLevel(attemptedThisLevel);

        Long lastAttemptedUserLevel = document.getLong("lastAttemptedUserLevel");
        if (lastAttemptedUserLevel != null) boss.setLastAttemptedUserLevel(lastAttemptedUserLevel.intValue());

        return boss;
    }

    // ========== CALLBACK INTERFACES ==========

    public interface BossCallback {
        void onSuccess(Boss boss); // boss može biti null
        void onFailure(String error);
    }

    public interface InsertBossCallback {
        void onSuccess(String bossId);
        void onFailure(String error);
    }

    public interface UpdateBossCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
