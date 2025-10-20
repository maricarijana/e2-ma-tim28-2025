package com.example.teamgame28.repository;

import android.util.Log;

import com.example.teamgame28.model.AllianceMessage;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class AllianceMessageRepository {

    private static final String TAG = "AllianceMessageRepo";
    private static final String COLLECTION_ALLIANCES = "alliances";
    private static final String COLLECTION_MESSAGES = "messages";

    private final FirebaseFirestore db;

    public AllianceMessageRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Pošalji poruku u savez (doda se u subkolekciju "messages").
     */
    public void sendMessage(String allianceId, AllianceMessage message, RepoCallback callback) {
        CollectionReference messagesRef = db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .collection(COLLECTION_MESSAGES);

        // generišemo ID
        String msgId = messagesRef.document().getId();
        message.setId(msgId);

        messagesRef.document(msgId).set(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Poruka poslata: " + msgId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Greška kod slanja poruke", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Real-time slušanje poruka u savezu (redosled po timestampu).
     */
    public com.google.firebase.firestore.ListenerRegistration listenForMessages(String allianceId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    // Callback interfejs
    public interface RepoCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
