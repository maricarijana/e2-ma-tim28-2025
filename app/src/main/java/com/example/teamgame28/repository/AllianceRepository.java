package com.example.teamgame28.repository;

import android.util.Log;

import com.example.teamgame28.model.Alliance;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AllianceRepository {

    private static final String TAG = "AllianceRepository";
    private static final String COLLECTION_ALLIANCES = "alliances";
    private static final String COLLECTION_USERS = "users";

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
     */
    public void acceptInvite(String allianceId, String userId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

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
     */
    public void disbandAlliance(String allianceId, RepoCallback callback) {
        DocumentReference allianceRef = db.collection(COLLECTION_ALLIANCES).document(allianceId);

        allianceRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "💥 Savez ukinut: " + allianceId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greska kod brisanja saveza", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Helper: update user profila da zna u kom je savezu.
     * VAŽNO: currentAllianceId mora biti u profile podkolekciji!
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
}
