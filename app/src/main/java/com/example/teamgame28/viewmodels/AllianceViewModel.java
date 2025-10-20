package com.example.teamgame28.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.AllianceMission;
import com.example.teamgame28.model.AllianceMissionProgress;
import com.example.teamgame28.repository.AllianceRepository;
import com.example.teamgame28.repository.AllianceMissionRepository;
import com.example.teamgame28.service.AllianceMissionService;
import com.example.teamgame28.service.AllianceService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * ViewModel za upravljanje savezima i specijalnim misijama.
 */
public class AllianceViewModel extends ViewModel {

    private final AllianceService allianceService;
    private final AllianceMissionService missionService;
    private final AllianceRepository allianceRepository;
    private final AllianceMissionRepository missionRepository;

    private final MutableLiveData<Alliance> currentAlliance = new MutableLiveData<>();
    private final MutableLiveData<AllianceMission> activeMission = new MutableLiveData<>();
    private final MutableLiveData<List<AllianceMissionProgress>> missionProgress = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AllianceViewModel() {
        this.allianceService = new AllianceService();
        this.missionService = new AllianceMissionService();
        this.allianceRepository = new AllianceRepository();
        this.missionRepository = new AllianceMissionRepository();
    }

    // ==================== GETTERS ====================

    public LiveData<Alliance> getCurrentAlliance() {
        return currentAlliance;
    }

    public LiveData<AllianceMission> getActiveMission() {
        return activeMission;
    }

    public LiveData<List<AllianceMissionProgress>> getMissionProgress() {
        return missionProgress;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    // ==================== ALLIANCE OPERATIONS ====================

    /**
     * Učitava savez za korisnika.
     */
    public void loadUserAlliance(String userId) {
        loading.setValue(true);
        allianceRepository.getAllianceByUserId(userId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onSuccess(Alliance alliance) {
                loading.setValue(false);
                currentAlliance.setValue(alliance);

                if (alliance != null) {
                    // 🔥 Dodato — pokreće realtime slušanje misije
                    listenToActiveMission(alliance.getId());

                    // i dalje možeš da učitaš postojeće stanje jednom:
                    loadActiveMission(alliance.getId());
                }
            }

            @Override
            public void onFailure(Exception e) {
                loading.setValue(false);
                errorMessage.setValue("Greška pri učitavanju saveza: " + e.getMessage());
            }
        });
    }


    // Kreiranje saveza, napuštanje, ukidanje - to je tačka 7.1, ne 7.3
    // Za tačku 7.3 potreban je samo pregled postojećeg saveza

    // ==================== MISSION OPERATIONS ====================

    /**
     * Učitava aktivnu misiju za savez.
     */
    public void loadActiveMission(String allianceId) {
        loading.setValue(true);
        missionRepository.getActiveMission(allianceId, new AllianceMissionRepository.MissionCallback() {
            @Override
            public void onSuccess(AllianceMission mission) {
                loading.setValue(false);
                activeMission.setValue(mission);
            }

            @Override
            public void onFailure(Exception e) {
                loading.setValue(false);
                // Nije greška ako nema aktivne misije
                activeMission.setValue(null);
            }
        });
    }

    /**
     * Pokreće specijalnu misiju (samo vođa).
     */
    public void startSpecialMission(String allianceId, String leaderId) {
        loading.setValue(true);
        missionService.startSpecialMission(allianceId, leaderId, new AllianceMissionService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                loading.setValue(false);
                successMessage.setValue(message);
                loadActiveMission(allianceId);
            }

            @Override
            public void onFailure(String error) {
                loading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Učitava napredak svih članova u misiji.
     */
    public void loadMissionProgress(String missionId) {
        allianceRepository.getMissionProgressForAllMembers(missionId, new AllianceRepository.ProgressListCallback() {
            @Override
            public void onSuccess(List<AllianceMissionProgress> progressList) {
                missionProgress.setValue(progressList);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Greška pri učitavanju napretka: " + e.getMessage());
            }
        });
    }
    public void listenToActiveMission(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("alliance_missions")
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("active", true)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty()) return;
                    AllianceMission mission = snapshots.getDocuments().get(0).toObject(AllianceMission.class);
                    activeMission.postValue(mission);
                });
    }

    /**
     * Refresuje podatke (alliance, mission, progress).
     */
    public void refreshData(String userId) {
        loadUserAlliance(userId);
    }
}
