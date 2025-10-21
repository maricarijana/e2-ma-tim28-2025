package com.example.teamgame28.service;

import com.example.teamgame28.model.AllianceMessage;
import com.example.teamgame28.repository.AllianceMessageRepository;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QuerySnapshot;

public class AllianceMessageService {

    private final AllianceMessageRepository repo = new AllianceMessageRepository();

    public interface ServiceCallback { void onSuccess(); void onFailure(String error); }

    /** Pošalji poruku (timestamp klijentski; repo već radi set(message)). */
    public void sendMessage(String allianceId,
                            String senderId,
                            String senderUsername,
                            String text,
                            ServiceCallback cb) {

        long now = System.currentTimeMillis();
        AllianceMessage msg = new AllianceMessage(
                null,              // id popunjava repo
                allianceId,
                senderId,
                senderUsername,
                text,
                now
        );

        repo.sendMessage(allianceId, msg, new AllianceMessageRepository.RepoCallback() {
            @Override public void onSuccess() { cb.onSuccess(); }
            @Override public void onFailure(Exception e) { cb.onFailure(e.getMessage()); }
        });
    }

    /** Realtime slušanje poruka za UI (RecyclerView, itd.). */
    public com.google.firebase.firestore.ListenerRegistration listenForMessages(String allianceId, EventListener<QuerySnapshot> listener) {
        return repo.listenForMessages(allianceId, listener);
    }
}
