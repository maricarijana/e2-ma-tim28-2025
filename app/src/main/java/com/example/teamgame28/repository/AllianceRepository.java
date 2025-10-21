package com.example.teamgame28.repository;

import android.util.Log;

import com.example.teamgame28.model.Alliance;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AllianceRepository {

    private static final String TAG = "AllianceRepository";
    private static final String COLLECTION_ALLIANCES = "alliances";
    private static final String COLLECTION_USERS = "app_users";
    private static final String COLLECTION_ALLIANCE_MISSIONS = "alliance_missions";

    private final FirebaseFirestore db;

    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Kreiraj novi savez sa nazivom i postavi vođu.
     */
    public void createAlliance(String leaderId, String name, RepoCallback callback) {
        DocumentReference newAllianceRef = db.collection(COLLECTION_ALLIANCES).document();
        String allianceId = newAllianceRef.getId();

        Alliance alliance = new Alliance(
                allianceId,
                name,
                leaderId,
                System.currentTimeMillis()
        );

        newAllianceRef.set(alliance)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Savez kreiran: " + allianceId);

                    // Update user profila – setuj currentAllianceId
                    updateUserAlliance(leaderId, allianceId);

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greska kod kreiranja saveza", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Pošalji poziv prijatelju da se pridruži savezu.
     * Dodaj userId u "pendingInvites".
     */
    public void inviteToAlliance(String allianceId, String fromUserId, String toUserId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("pendingInvites", FieldValue.arrayUnion(toUserId));

        allianceRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "📨 Poziv poslat korisniku " + toUserId + " u savez " + allianceId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greska kod slanja poziva", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Prihvati poziv → dodaj korisnika u members listu.
     * Ako korisnik već ima savez (oldAllianceId != null), ukloni ga iz starog saveza.
     */
    public void acceptInvite(String allianceId, String userId, String oldAllianceId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

        // KORAK 0: Ako ima stari savez, prvo ga ukloni iz members liste starog saveza
        if (oldAllianceId != null && !oldAllianceId.isEmpty()) {
            DocumentReference oldAllianceRef = db.collection(COLLECTION_ALLIANCES).document(oldAllianceId);
            oldAllianceRef.update("members", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "🚪 " + userId + " napustio stari savez " + oldAllianceId);
                        // Nastavi sa pridruživanjem novom savezu
                        joinNewAlliance(allianceRef, userId, allianceId, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Greška kod napuštanja starog saveza", e);
                        callback.onFailure(e);
                    });
        } else {
            // Nema starog saveza, direktno se pridruži novom
            joinNewAlliance(allianceRef, userId, allianceId, callback);
        }
    }

    /**
     * Helper metoda za pridruživanje novom savezu.
     */
    private void joinNewAlliance(DocumentReference allianceRef, String userId, String allianceId, RepoCallback callback) {
        // 1. Dodaj u members
        allianceRef.update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "👥 " + userId + " pridružen savezu " + allianceId);

                    // 2. Ukloni iz pendingInvites
                    allianceRef.update("pendingInvites", FieldValue.arrayRemove(userId));

                    // 3. Update user profila
                    updateUserAlliance(userId, allianceId);

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod prihvatanja poziva", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Odbij poziv → samo ukloni iz pendingInvites.
     */
    public void declineInvite(String allianceId, String userId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

        allianceRef.update("pendingInvites", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "🚫 " + userId + " odbio poziv u savez " + allianceId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod odbijanja poziva", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Vođa može da ukine ceo savez.
     * Uklanja currentAllianceId svim članovima i briše savez.
     */
    public void disbandAlliance(String allianceId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

        // Prvo pročitaj Alliance da dobiješ listu članova
        allianceRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure(new Exception("Savez ne postoji"));
                        return;
                    }

                    Alliance alliance = documentSnapshot.toObject(Alliance.class);
                    if (alliance == null) {
                        callback.onFailure(new Exception("Greška pri čitanju saveza"));
                        return;
                    }

                    // Dobij sve članove (uključujući vođu)
                    java.util.List<String> allMembers = new java.util.ArrayList<>();
                    if (alliance.getMembers() != null) {
                        allMembers.addAll(alliance.getMembers());
                    }
                    // Dodaj vođu ako nije već u listi members
                    if (alliance.getLeaderId() != null && !allMembers.contains(alliance.getLeaderId())) {
                        allMembers.add(alliance.getLeaderId());
                    }

                    // Ukloni currentAllianceId svim članovima
                    for (String memberId : allMembers) {
                        removeUserAlliance(memberId);
                    }

                    // Na kraju obriši Alliance dokument
                    allianceRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "💥 Savez ukinut: " + allianceId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Greška kod brisanja saveza", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod čitanja saveza", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Helper: update user profila da zna u kom je savezu.
     */
    private void updateUserAlliance(String userId, String allianceId) {
        // currentAllianceId je u app_users/{userId}/profile/{userId}, NE u app_users/{userId}!
        DocumentReference profileRef = db.collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentAllianceId", allianceId);

        profileRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ currentAllianceId postavljen za usera " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Greska kod update-a user profila", e));
    }

    /**
     * Helper: ukloni currentAllianceId iz user profila.
     */
    private void removeUserAlliance(String userId) {
        DocumentReference profileRef = db.collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentAllianceId", null);

        profileRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ currentAllianceId uklonjen za usera " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Greška kod uklanjanja currentAllianceId", e));
    }

    /**
     * Pronađi savez po ID-u korisnika.
     */
    public void getAllianceByUserId(String userId, AllianceCallback callback) {
        Log.d(TAG, "🔍 Tražim savez za userId: " + userId);

        db.collection(COLLECTION_ALLIANCES)
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "📊 Query rezultat - broj dokumenata: " + query.size());

                    if (query.isEmpty()) {
                        Log.w(TAG, "⚠️ Nema saveza za korisnika: " + userId);
                        callback.onSuccess(null);
                    } else {
                        Alliance alliance = query.getDocuments().get(0).toObject(Alliance.class);
                        Log.d(TAG, "✅ Savez pronađen: " + (alliance != null ? alliance.getName() : "null"));
                        callback.onSuccess(alliance);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška pri traženju saveza: " + e.getMessage(), e);
                    callback.onFailure(e);
                });
    }

    /**
     * Dohvati napredak svih članova u misiji.
     */
    public void getMissionProgressForAllMembers(String missionId, ProgressListCallback callback) {
        db.collection("alliance_mission_progress")
                .whereEqualTo("missionId", missionId)
                .get()
                .addOnSuccessListener(query -> {
                    java.util.List<com.example.teamgame28.model.AllianceMissionProgress> progressList = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        com.example.teamgame28.model.AllianceMissionProgress progress = doc.toObject(com.example.teamgame28.model.AllianceMissionProgress.class);
                        if (progress != null) {
                            progressList.add(progress);
                        }
                    }
                    callback.onSuccess(progressList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Dohvati sve saveze gde je korisnik u pendingInvites listi.
     */
    public void getPendingInvitesForUser(String userId, AllianceListCallback callback) {
        db.collection(COLLECTION_ALLIANCES)
                .whereArrayContains("pendingInvites", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<Alliance> alliances = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Alliance alliance = doc.toObject(Alliance.class);
                        if (alliance != null) {
                            alliances.add(alliance);
                        }
                    }
                    Log.d(TAG, "✅ Dohvaćeno " + alliances.size() + " pending poziva");
                    callback.onSuccess(alliances);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod dohvatanja poziva", e);
                    callback.onFailure(e);
                });
    }

    // Callback interfejs za repo metode
    public interface RepoCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Callback interfejs za listu saveza
    public interface AllianceListCallback {
        void onSuccess(java.util.List<Alliance> alliances);
        void onFailure(Exception e);
    }

    /**
     * Proveri da li savez ima aktivnu misiju.
     */
    public void hasActiveMission(String allianceId, ActiveMissionCallback callback) {
        db.collection(COLLECTION_ALLIANCE_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasActive = !querySnapshot.isEmpty();
                    Log.d(TAG, "✅ Provera aktivne misije za savez " + allianceId + ": " + hasActive);
                    callback.onResult(hasActive);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod provere aktivne misije", e);
                    callback.onFailure(e);
                });
    }

    // Callback interfejs za proveru aktivne misije
    public interface ActiveMissionCallback {
        void onResult(boolean hasActiveMission);
        void onFailure(Exception e);
    }

    public interface AllianceCallback {
        void onSuccess(Alliance alliance);
        void onFailure(Exception e);
    }

    public interface ProgressListCallback {
        void onSuccess(java.util.List<com.example.teamgame28.model.AllianceMissionProgress> progressList);
        void onFailure(Exception e);
    }
}
