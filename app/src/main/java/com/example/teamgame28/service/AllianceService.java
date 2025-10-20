package com.example.teamgame28.service;

import com.example.teamgame28.repository.AllianceRepository;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AllianceService {

    private final AllianceRepository allianceRepository;
    private final FirebaseFirestore db;
    public AllianceService() {
        this.allianceRepository = new AllianceRepository();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Kreiraj novi savez.
     */
    public void createAlliance(String leaderId, String name, ServiceCallback callback) {
        allianceRepository.createAlliance(leaderId, name, new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Savez uspešno kreiran!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri kreiranju saveza: " + e.getMessage());
            }
        });
    }

    /**
     * Pozovi prijatelja u savez.
     */
    public void inviteToAlliance(String allianceId, String fromUserId, String toUserId, ServiceCallback callback) {
        allianceRepository.inviteToAlliance(allianceId, fromUserId, toUserId, new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                // --- 2) upis Firestore invite-a kod PRIJATELJA ---
                String inviteId = db.collection("tmp").document().getId(); // generiši ID

                Map<String, Object> invite = new HashMap<>();
                invite.put("inviteId", inviteId);
                invite.put("allianceId", allianceId);
                invite.put("fromUserId", fromUserId);   // vođa (pošiljalac)
                invite.put("toUserId", toUserId);       // primalac (ovaj user)
                invite.put("status", "pending");        // pending | accepted | declined
                invite.put("createdAt", FieldValue.serverTimestamp());
                invite.put("notified", false);          // prijatelj još nije prikazao lokalnu notifikaciju

                db.collection("app_users")
                        .document(toUserId)
                        .collection("invites")
                        .document(inviteId)
                        .set(invite)
                        .addOnSuccessListener(unused -> callback.onSuccess("Poziv uspešno poslat!"))
                        .addOnFailureListener(e -> callback.onFailure("Poziv poslat, ali upis obaveštenja nije uspeo: " + e.getMessage()));
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri slanju poziva: " + e.getMessage());
            }
        });
    }

    /**
     * Prihvati poziv za savez.
     * Ako korisnik već ima savez (oldAllianceId), automatski ga napušta i pridružuje se novom.
     */
    public void acceptInvite(String allianceId, String userId, String oldAllianceId, ServiceCallback callback) {
        allianceRepository.acceptInvite(allianceId, userId, oldAllianceId, new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Uspešno si se pridružio savezu!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri pridruživanju savezu: " + e.getMessage());
            }
        });
    }

    /**
     * Odbij poziv.
     */
    public void declineInvite(String allianceId, String userId, ServiceCallback callback) {
        allianceRepository.declineInvite(allianceId, userId, new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Poziv odbijen.");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška: " + e.getMessage());
            }
        });
    }

    /**
     * Ukini ceo savez (samo vođa).
     */
    public void disbandAlliance(String allianceId, ServiceCallback callback) {
        allianceRepository.disbandAlliance(allianceId, new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Savez uspešno ukinut!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri ukidanju saveza: " + e.getMessage());
            }
        });
    }

    /**
     * Proveri da li savez ima aktivnu misiju.
     */
    public void hasActiveMission(String allianceId, ActiveMissionCallback callback) {
        allianceRepository.hasActiveMission(allianceId, new AllianceRepository.ActiveMissionCallback() {
            @Override
            public void onResult(boolean hasActiveMission) {
                callback.onResult(hasActiveMission);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri proveri misije: " + e.getMessage());
            }
        });
    }

    // Generic service callback
    public interface ServiceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    // Callback za proveru aktivne misije
    public interface ActiveMissionCallback {
        void onResult(boolean hasActiveMission);
        void onFailure(String error);
    }
}
