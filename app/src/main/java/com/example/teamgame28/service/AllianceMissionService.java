package com.example.teamgame28.service;

import com.example.teamgame28.repository.AllianceMissionRepository;

public class AllianceMissionService {

    private final AllianceMissionRepository missionRepository;

    public AllianceMissionService() {
        this.missionRepository = new AllianceMissionRepository();
    }

    /**
     * Vođa saveza pokreće specijalnu misiju.
     * HP bossa = 100 * broj članova saveza
     * Traje 2 nedelje, ne može se prekinuti, i svi članovi automatski učestvuju.
     */
    public void startSpecialMission(String allianceId, String leaderId, ServiceCallback callback) {
        missionRepository.startSpecialMission(allianceId, leaderId, new AllianceMissionRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Specijalna misija je uspešno započeta!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška pri pokretanju misije: " + e.getMessage());
            }
        });
    }

    public interface ServiceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
