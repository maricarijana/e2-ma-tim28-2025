package com.example.teamgame28.repository;

import android.util.Log;

import com.example.teamgame28.model.FriendRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FriendRepository {

    private static final String TAG = "FriendRepository";
    private static final String COLLECTION_REQUESTS = "friendRequests";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;

    public FriendRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Pošalji friend request (status = pending).
     */
    public void sendFriendRequest(String fromUserId, String toUserId, RepoCallback callback) {
        if (fromUserId.equals(toUserId)) {
            callback.onFailure(new Exception("Ne možeš poslati zahtev sam sebi!"));
            return;
        }

        // Kreiraj prazan doc da dobijemo ID
        DocumentReference newReqRef = db.collection(COLLECTION_REQUESTS).document();
        String requestId = newReqRef.getId();

        FriendRequest request = new FriendRequest(
                requestId,
                fromUserId,
                toUserId,
                System.currentTimeMillis(),
                "pending"
        );

        newReqRef.set(request)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Friend request poslat: " + requestId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greska kod slanja friend requesta", e);
                    callback.onFailure(e);
                });
    }


    /**
     * Prihvati friend request → dodaj u oba profila listu friends.
     */
    public void acceptFriendRequest(String requestId, String fromUserId, String toUserId, RepoCallback callback) {
        DocumentReference requestRef = db.collection(COLLECTION_REQUESTS).document(requestId);

        // 1. update status
        requestRef.update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Friend request prihvaćen");

                    // 2. dodaj oba korisnika jedan drugom u friends
                    addFriendship(fromUserId, toUserId);
                    addFriendship(toUserId, fromUserId);

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod prihvatanja zahteva", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Odbij friend request (status = declined).
     */
    public void declineFriendRequest(String requestId, RepoCallback callback) {
        DocumentReference requestRef = db.collection(COLLECTION_REQUESTS).document(requestId);

        requestRef.update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Friend request odbijen");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod odbijanja friend requesta", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Dohvati pending friend requests za korisnika.
     */
    public void getPendingFriendRequests(String userId, FriendRequestListCallback callback) {
        db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<FriendRequest> requests = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        FriendRequest request = doc.toObject(FriendRequest.class);
                        if (request != null) {
                            requests.add(request);
                        }
                    }
                    Log.d(TAG, "✅ Dohvaćeno " + requests.size() + " pending friend requests");
                    callback.onSuccess(requests);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod dohvatanja friend requests", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Helper – dodaj prijatelja u polje friends[] u UserProfile dokumentu (podkolekcija).
     */
    private void addFriendship(String userId, String friendId) {
        // VAŽNO: friends je u profile podkolekciji, ne u glavnom user dokumentu!
        DocumentReference profileRef = db.collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("friends", com.google.firebase.firestore.FieldValue.arrayUnion(friendId));

        profileRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "👥 " + friendId + " dodat u friends listu korisnika " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Greska kod dodavanja prijatelja: " + e.getMessage()));
    }

    // Callback interfejs za repo metode
    public interface RepoCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Callback interfejs za listu friend requests
    public interface FriendRequestListCallback {
        void onSuccess(java.util.List<FriendRequest> requests);
        void onFailure(Exception e);
    }
}
