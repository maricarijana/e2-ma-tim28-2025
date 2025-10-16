package com.example.teamgame28.service;

import android.util.Log;

import com.example.teamgame28.model.BattleResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Servis koji čuva rezultat borbe u Firestore bazi.
 * Kreira novi dokument u kolekciji app_users/{userId}/battles.
 */
public class BattleResultService {

    private final FirebaseFirestore db;

    public BattleResultService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Snima rezultat borbe za trenutnog korisnika.
     * @param result BattleResult objekat sa svim informacijama o borbi
     */
    public void saveBattleResult(BattleResult result) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Log.e("BattleResultService", "User not logged in");
            return;
        }

        Map<String, Object> battleData = new HashMap<>();
        battleData.put("bossDefeated", result.isBossDefeated());
        battleData.put("coinsEarned", result.getCoinsEarned());
        battleData.put("equipmentDropped", result.isEquipmentDropped());
        battleData.put("isWeapon", result.isWeapon());
        battleData.put("equipmentChance", result.getEquipmentChance());
        battleData.put("timestamp", System.currentTimeMillis());

        db.collection("app_users")
                .document(userId)
                .collection("battles")
                .add(battleData)
                .addOnSuccessListener(ref ->
                        Log.d("BattleResultService", "Battle result saved with id: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.e("BattleResultService", "Failed to save battle result", e));
    }
}
