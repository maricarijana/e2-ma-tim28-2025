package com.example.teamgame28.repository;

import android.util.Log;
import com.example.teamgame28.model.BattleResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class BattleResultRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void saveBattleResult(BattleResult result) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            Log.e("BattleResultRepo", "User not logged in");
            return;
        }

        Map<String, Object> battleData = new HashMap<>();
        battleData.put("bossDefeated", result.isBossDefeated());
        battleData.put("coinsEarned", result.getCoinsEarned());
        battleData.put("equipmentDropped", result.isEquipmentDropped());
        battleData.put("isWeapon", result.isWeapon());
        battleData.put("timestamp", System.currentTimeMillis());

        db.collection("app_users")
                .document(userId)
                .collection("battles")
                .add(battleData)
                .addOnSuccessListener(ref ->
                        Log.d("BattleResultRepo", "Battle result saved with id: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.e("BattleResultRepo", "Failed to save battle result", e));
    }
}
