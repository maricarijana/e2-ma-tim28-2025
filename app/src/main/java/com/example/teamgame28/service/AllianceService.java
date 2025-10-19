package com.example.teamgame28.service;

import com.example.teamgame28.repository.AllianceRepository;

public class AllianceService {

    private final AllianceRepository allianceRepository;

    public AllianceService() {
        this.allianceRepository = new AllianceRepository();
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
                callback.onSuccess("Poziv uspešno poslat!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri slanju poziva: " + e.getMessage());
            }
        });
    }

    /**
     * Prihvati poziv za savez.
     */
    public void acceptInvite(String allianceId, String userId, ServiceCallback callback) {
        allianceRepository.acceptInvite(allianceId, userId, new AllianceRepository.RepoCallback() {
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

    // Generic service callback
    public interface ServiceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
