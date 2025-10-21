package com.example.teamgame28.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.teamgame28.model.AllianceMissionProgress;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Servis za ažuriranje napretka korisnika tokom specijalne misije.
 * Ažurira damage i napredak u bazi i smanjuje HP bossa.
 */
public class AllianceMissionProgressService {

    private final FirebaseFirestore db;

    public AllianceMissionProgressService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Beleži akciju korisnika i smanjuje HP bossa.
     */
    public void recordUserAction(AllianceMissionProgress progress, ServiceCallback callback) {
        if (progress.getMissionId() == null || progress.getUserId() == null) {
            callback.onFailure("Nedostaje missionId ili userId");
            return;
        }

        String missionId = progress.getMissionId();
        String userId = progress.getUserId();

        // 🔹 Ažuriraj dokument korisnika u kolekciji napretka
        db.collection("alliance_mission_progress")
                .document(userId + "_" + missionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    AllianceMissionProgress current;
                    if (snapshot.exists()) {
                        current = snapshot.toObject(AllianceMissionProgress.class);
                        if (current == null) current = new AllianceMissionProgress();
                    } else {
                        current = new AllianceMissionProgress();
                        current.setMissionId(missionId);
                        current.setUserId(userId);
                    }

                    // Saberi prethodni i novi napredak
                    current.setDamageDealt(current.getDamageDealt() + progress.getDamageDealt());
                    current.setTasksCompleted(current.getTasksCompleted() + progress.getTasksCompleted());
                    current.setShopPurchases(current.getShopPurchases() + progress.getShopPurchases());
                    current.setMessagesSent(current.getMessagesSent() + progress.getMessagesSent());
                    current.setNoUnfinishedTasks(progress.isNoUnfinishedTasks());

                    // Snimi nazad u Firestore
                    db.collection("alliance_mission_progress")
                            .document(userId + "_" + missionId)
                            .set(current)
                            .addOnSuccessListener(unused -> {
                                // 🔹 Smanji boss HP u kolekciji misije
                                db.collection("alliance_missions")
                                        .document(missionId)
                                        .update("bossHp", com.google.firebase.firestore.FieldValue.increment(-progress.getDamageDealt()))
                                        .addOnSuccessListener(unused2 -> {
                                            Log.d("AllianceProgress", "✅ Boss HP smanjen za " + progress.getDamageDealt());
                                            callback.onSuccess("Boss HP smanjen!");
                                        })
                                        .addOnFailureListener(e -> callback.onFailure("Greška pri ažuriranju boss HP-a: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onFailure("Greška pri snimanju napretka: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Greška pri čitanju napretka: " + e.getMessage()));
    }

    // 🔹 Callback interfejs
    public interface ServiceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public void rewardAllianceMembers(String missionId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1️⃣ Nađi misiju
        db.collection("alliance_missions").document(missionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    int bossHp = doc.getLong("bossHp").intValue();
                    String allianceId = doc.getString("allianceId");

                    if (bossHp > 0) {
                        Log.d("SpecialMission", "⛔ Boss još nije poražen.");
                        return;
                    }

                    // 2️⃣ Nađi članove saveza
                    db.collection("alliances").document(allianceId)
                            .get()
                            .addOnSuccessListener(allianceDoc -> {
                                List<String> members = (List<String>) allianceDoc.get("members");

                                if (members == null || members.isEmpty()) return;

                                for (String userId : members) {
                                    SpecialTaskMissionService.grantMissionRewardsToUser(userId);

                                }

                                Log.d("SpecialMission", "✅ Dodeljene nagrade svim članovima saveza!");
                            });
                });
    }

}
