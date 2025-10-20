package com.example.teamgame28.repository;

import android.util.Log;

import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.AllianceMission;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceMissionRepository {

    private static final String TAG = "AllianceMissionRepository";
    private static final String COLLECTION_MISSIONS = "alliance_missions";
    private static final String COLLECTION_ALLIANCES = "alliances";

    private final FirebaseFirestore db;

    public AllianceMissionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Kreiraj novu specijalnu misiju ako savez nema aktivnu.
     */
    public void startSpecialMission(String allianceId, String leaderId, RepoCallback callback) {
        // 1️⃣ Provera da li postoji aktivna misija
        db.collection(COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        callback.onFailure(new Exception("Savez već ima aktivnu specijalnu misiju."));
                        return;
                    }

                    // 2️⃣ Učitaj savez da dobiješ broj članova
                    db.collection(COLLECTION_ALLIANCES)
                            .document(allianceId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                Alliance alliance = snapshot.toObject(Alliance.class);
                                if (alliance == null) {
                                    callback.onFailure(new Exception("Savez nije pronađen."));
                                    return;
                                }

                                // Hardkodovano: vodja = leaderId (ako nemamo login)
                                if (!leaderId.equals(alliance.getLeaderId())) {
                                    callback.onFailure(new Exception("Samo vođa saveza može da pokrene misiju."));
                                    return;
                                }

                                int memberCount = alliance.getMembers().size();
                                if (memberCount == 0) memberCount = 1; // fallback

                                int bossHp = 100 * memberCount;

                                Timestamp startTime = Timestamp.now();
                                Calendar cal = Calendar.getInstance();
                                cal.add(Calendar.DAY_OF_MONTH, 14); // traje 2 nedelje
                                Timestamp endTime = new Timestamp(cal.getTime());

                                // 3️⃣ Kreiraj misiju
                                DocumentReference missionRef = db.collection(COLLECTION_MISSIONS).document();
                                AllianceMission mission = new AllianceMission(
                                        missionRef.getId(),
                                        allianceId,
                                        bossHp,
                                        true,
                                        Timestamp.now(),
                                        endTime
                                );

                                missionRef.set(mission)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ Specijalna misija pokrenuta: " + missionRef.getId());

                                            // 🔔 Poziv metode koja šalje notifikacije svim članovima saveza
                                            notifyAllianceMembers(allianceId, missionRef.getId());

                                            // 🔁 Vrati rezultat servisu
                                            callback.onSuccess();
                                        })
                                        .addOnFailureListener(e -> callback.onFailure(e));


                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 🔔 Obavesti sve članove saveza da je misija počela
     */
    private void notifyAllianceMembers(String allianceId, String missionId) {
        db.collection(COLLECTION_ALLIANCES).document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) return;

                    List<String> members = (List<String>) allianceDoc.get("members");
                    if (members == null || members.isEmpty()) return;

                    for (String memberId : members) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", memberId);
                        notification.put("title", "🛡️ Nova specijalna misija!");
                        notification.put("message", "Vođa saveza je pokrenuo novu misiju. Pridruži se i napadni bossa!");
                        notification.put("missionId", missionId);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(doc ->
                                        Log.d(TAG, "📩 Notifikacija poslata za člana: " + memberId))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "❌ Greška pri slanju notifikacije", e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Greška pri učitavanju saveza za notifikacije", e));
    }


    /**
     * Dohvati aktivnu misiju za savez.
     */
    public void getActiveMission(String allianceId, MissionCallback callback) {
        db.collection(COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onSuccess(null);
                    } else {
                        AllianceMission mission = query.getDocuments().get(0).toObject(AllianceMission.class);
                        callback.onSuccess(mission);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface RepoCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface MissionCallback {
        void onSuccess(AllianceMission mission);
        void onFailure(Exception e);
    }
}
