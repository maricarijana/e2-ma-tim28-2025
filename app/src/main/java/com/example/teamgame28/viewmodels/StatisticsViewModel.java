package com.example.teamgame28.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.dto.StatisticsDto;
import com.example.teamgame28.service.StatisticsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class StatisticsViewModel extends AndroidViewModel {

    private final StatisticsService statisticsService;

    private final MutableLiveData<StatisticsDto.TaskStatusStats> taskStatusCounts = new MutableLiveData<>();
    private final MutableLiveData<List<StatisticsDto.CategoryStats>> categoryStats = new MutableLiveData<>();
    private final MutableLiveData<List<StatisticsService.XpDataPoint>> xpData = new MutableLiveData<>();
    private final MutableLiveData<List<StatisticsService.AverageDifficultyPoint>> avgDifficultyData = new MutableLiveData<>();
    private final MutableLiveData<StatisticsDto.OverallAvgDifficulty> overallAvgDifficulty = new MutableLiveData<>();
    private final MutableLiveData<StatisticsDto.LongestStreakResult> longestStreak = new MutableLiveData<>();
    private final MutableLiveData<StatisticsDto.SpecialMissionsStats> specialMissionsStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        this.statisticsService = new StatisticsService();
        this.loading.setValue(false);
    }

    public LiveData<StatisticsDto.TaskStatusStats> getTaskStatusCounts() {
        return taskStatusCounts;
    }

    public LiveData<List<StatisticsDto.CategoryStats>> getCategoryStats() {
        return categoryStats;
    }

    public LiveData<List<StatisticsService.XpDataPoint>> getXpData() {
        return xpData;
    }

    public LiveData<List<StatisticsService.AverageDifficultyPoint>> getAvgDifficultyData() {
        return avgDifficultyData;
    }

    public LiveData<StatisticsDto.OverallAvgDifficulty> getOverallAvgDifficulty() {
        return overallAvgDifficulty;
    }

    public LiveData<StatisticsDto.LongestStreakResult> getLongestStreak() {
        return longestStreak;
    }

    public LiveData<StatisticsDto.SpecialMissionsStats> getSpecialMissionsStats() {
        return specialMissionsStats;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    // Učitaj statistiku za trenutno ulogovanog korisnika
    public void loadTaskStatistics() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            error.setValue("Korisnik nije prijavljen");
            return;
        }

        loading.setValue(true);
        String userId = currentUser.getUid();

        // Task status counts
        statisticsService.getTaskStatusCounts(userId, new StatisticsService.TaskStatusCallback() {
            @Override
            public void onSuccess(StatisticsDto.TaskStatusStats stats) {
                taskStatusCounts.setValue(stats);
                loading.setValue(false);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                loading.setValue(false);
            }
        });

        // Statistika po kategorijama
        statisticsService.getFinishedTasksByCategory(userId, new StatisticsService.CategoryStatsCallback() {
            @Override
            public void onSuccess(List<StatisticsDto.CategoryStats> categoryList) {
                categoryStats.setValue(categoryList);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // XP za poslednjih 7 dana
        statisticsService.getXpLast7Days(userId, new StatisticsService.XpHistoryCallback() {
            @Override
            public void onSuccess(List<StatisticsService.XpDataPoint> xpDataPoints) {
                xpData.setValue(xpDataPoints);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // Prosečna težina zadataka za poslednjih 7 dana
        statisticsService.getAverageDifficultyLast7Days(userId, new StatisticsService.AverageDifficultyCallback() {
            @Override
            public void onSuccess(List<StatisticsService.AverageDifficultyPoint> avgData) {
                avgDifficultyData.setValue(avgData);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // Ukupan prosek težine
        statisticsService.getOverallAverageDifficulty(userId, new StatisticsService.OverallAvgCallback() {
            @Override
            public void onSuccess(StatisticsDto.OverallAvgDifficulty result) {
                overallAvgDifficulty.setValue(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // Najduži streak
        statisticsService.getLongestSuccessStreak(userId, new StatisticsService.LongestStreakCallback() {
            @Override
            public void onSuccess(StatisticsDto.LongestStreakResult result) {
                longestStreak.setValue(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // Specijalne misije
        statisticsService.getSpecialMissionsStats(userId, new StatisticsService.SpecialMissionsCallback() {
            @Override
            public void onSuccess(StatisticsDto.SpecialMissionsStats stats) {
                specialMissionsStats.setValue(stats);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }
}
