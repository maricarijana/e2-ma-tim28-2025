package com.example.teamgame28.service;

import android.util.Log;

import com.example.teamgame28.dto.StatisticsDto;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsService {

    private static final String TAG = "StatisticsService";
    private static final String TASKS_COLLECTION = "tasks";
    private static final String USERS_COLLECTION = "app_users";
    private static final String PROFILE_SUBCOLLECTION = "profile";
    private final FirebaseFirestore firestore;

    public StatisticsService() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Vraća statistiku taskova za korisnika - računa direktno iz Firestore-a
    public void getTaskStatusCounts(String userId, TaskStatusCallback callback) {
        firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int total = 0, finished = 0, unfinished = 0, canceled = 0, active = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        total++;

                        if (task.getStatus() == TaskStatus.FINISHED) finished++;
                        else if (task.getStatus() == TaskStatus.UNFINISHED) unfinished++;
                        else if (task.getStatus() == TaskStatus.CANCELLED) canceled++;
                        else if (task.getStatus() == TaskStatus.ACTIVE) active++;
                    }

                    StatisticsDto.TaskStatusStats stats =
                            new StatisticsDto.TaskStatusStats(total, finished, unfinished, canceled, active);

                    callback.onSuccess(stats);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Vraća broj završenih zadataka po kategoriji
    public void getFinishedTasksByCategory(String userId, CategoryStatsCallback callback) {
        firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.FINISHED)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StatisticsDto.CategoryStats> list = new ArrayList<>();

                    Map<String, Integer> temp = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        String categoryName = task.getCategoryName();
                        if (categoryName != null && !categoryName.isEmpty()) {
                            temp.put(categoryName, temp.getOrDefault(categoryName, 0) + 1);
                        }
                    }

                    for (Map.Entry<String, Integer> entry : temp.entrySet()) {
                        list.add(new StatisticsDto.CategoryStats(entry.getKey(), entry.getValue()));
                    }

                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Callback interface
    public interface TaskStatusCallback {
        void onSuccess(StatisticsDto.TaskStatusStats stats);
        void onFailure(Exception e);
    }

    public interface CategoryStatsCallback {
        void onSuccess(List<StatisticsDto.CategoryStats> categoryStats);
        void onFailure(Exception e);
    }

    // Vraća XP podatke za poslednjih 7 dana
    public void getXpLast7Days(String userId, XpHistoryCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);

                        if (userProfile != null) {
                            Map<String, Integer> xpHistory = userProfile.getXpHistory();
                            if (xpHistory == null) {
                                xpHistory = new HashMap<>();
                            }

                            // Generiši listu za poslednjih 7 dana
                            List<XpDataPoint> xpData = new ArrayList<>();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Calendar calendar = Calendar.getInstance();

                            for (int i = 6; i >= 0; i--) {
                                calendar.setTimeInMillis(System.currentTimeMillis());
                                calendar.add(Calendar.DAY_OF_YEAR, -i);
                                String date = sdf.format(calendar.getTime());

                                int xp = xpHistory.getOrDefault(date, 0);
                                xpData.add(new XpDataPoint(date, xp));
                            }

                            Log.d(TAG, "XP history za poslednjih 7 dana: " + xpData.toString());
                            callback.onSuccess(xpData);
                        } else {
                            callback.onFailure(new Exception("UserProfile ne postoji"));
                        }
                    } else {
                        callback.onFailure(new Exception("Korisnik ne postoji"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri učitavanju XP istorije", e);
                    callback.onFailure(e);
                });
    }

    // Pomoćna klasa za XP podatke
    public static class XpDataPoint {
        private String date;
        private int xp;

        public XpDataPoint(String date, int xp) {
            this.date = date;
            this.xp = xp;
        }

        public String getDate() { return date; }
        public int getXp() { return xp; }

        @Override
        public String toString() {
            return "XpDataPoint{date='" + date + "', xp=" + xp + "}";
        }
    }

    public interface XpHistoryCallback {
        void onSuccess(List<XpDataPoint> xpData);
        void onFailure(Exception e);
    }

    // Vraća prosečnu težinu završenih zadataka za poslednjih 7 dana
    public void getAverageDifficultyLast7Days(String userId, AverageDifficultyCallback callback) {
        firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.FINISHED)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Mapa: datum -> lista težina (difficultyXp)
                    Map<String, List<Integer>> difficultyByDate = new HashMap<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    // Grupiši završene zadatke po datumu njihovog završetka
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);

                        // lastActionTimestamp je vreme kada je zadatak završen
                        long finishedTimestamp = task.getLastActionTimestamp();
                        if (finishedTimestamp > 0) {
                            String date = sdf.format(new Date(finishedTimestamp));

                            if (!difficultyByDate.containsKey(date)) {
                                difficultyByDate.put(date, new ArrayList<>());
                            }
                            difficultyByDate.get(date).add(task.getDifficultyXp());
                        }
                    }

                    // Generiši listu za poslednjih 7 dana sa prosečnom težinom
                    List<AverageDifficultyPoint> avgData = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance();

                    for (int i = 6; i >= 0; i--) {
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.add(Calendar.DAY_OF_YEAR, -i);
                        String date = sdf.format(calendar.getTime());

                        double avgDifficulty = 0.0;
                        if (difficultyByDate.containsKey(date)) {
                            List<Integer> difficulties = difficultyByDate.get(date);
                            int sum = 0;
                            for (int diff : difficulties) {
                                sum += diff;
                            }
                            avgDifficulty = (double) sum / difficulties.size();
                        }

                        avgData.add(new AverageDifficultyPoint(date, avgDifficulty));
                    }

                    Log.d(TAG, "Prosečna težina za poslednjih 7 dana: " + avgData.toString());
                    callback.onSuccess(avgData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri računanju prosečne težine", e);
                    callback.onFailure(e);
                });
    }

    // Pomoćna klasa za prosečnu težinu po danu
    public static class AverageDifficultyPoint {
        private String date;
        private double avgDifficulty;

        public AverageDifficultyPoint(String date, double avgDifficulty) {
            this.date = date;
            this.avgDifficulty = avgDifficulty;
        }

        public String getDate() { return date; }
        public double getAvgDifficulty() { return avgDifficulty; }

        // Pomoćna metoda za dobijanje opisa težine
        public String getDifficultyLabel() {
            if (avgDifficulty == 0) return "Bez zadataka";
            else if (avgDifficulty <= 2) return "Veoma lak";
            else if (avgDifficulty <= 5) return "Lak";
            else if (avgDifficulty <= 13) return "Težak";
            else return "Ekstremno težak";
        }

        @Override
        public String toString() {
            return "AverageDifficultyPoint{date='" + date + "', avgDifficulty=" + avgDifficulty +
                   ", label='" + getDifficultyLabel() + "'}";
        }
    }

    public interface AverageDifficultyCallback {
        void onSuccess(List<AverageDifficultyPoint> avgData);
        void onFailure(Exception e);
    }

    // Vraća ukupan prosek težine svih završenih zadataka
    // Rezultat: Map sa ključevima "avgDifficulty" (double), "totalTasks" (int), "category" (String), "description" (String)
    public void getOverallAverageDifficulty(String userId, OverallAvgCallback callback) {
        firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.FINISHED)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess(new StatisticsDto.OverallAvgDifficulty(
                                0.0, 0, "Bez zadataka", "Završite zadatke da vidite statistiku"
                        ));
                        return;
                    }

                    int totalDifficulty = 0;
                    int taskCount = querySnapshot.size();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        totalDifficulty += task.getDifficultyXp();
                    }

                    double avgDifficulty = (double) totalDifficulty / taskCount;
                    String category;
                    if (avgDifficulty <= 2) category = "Veoma lak";
                    else if (avgDifficulty <= 5) category = "Lak";
                    else if (avgDifficulty <= 13) category = "Težak";
                    else category = "Ekstremno težak";

                    callback.onSuccess(new StatisticsDto.OverallAvgDifficulty(
                            avgDifficulty, taskCount, category,
                            "Uglavnom rešavate '" + category + "' zadatke"
                    ));
                })
                .addOnFailureListener(callback::onFailure);
    }


    public interface OverallAvgCallback {
        void onSuccess(StatisticsDto.OverallAvgDifficulty result);
        void onFailure(Exception e);
    }

    // Vraća najduži niz uspešno urađenih zadataka (u danima)
    // Niz se ne prekida ako nema zadataka, već samo ako postoje nezavršeni zadatci
    public void getLongestSuccessStreak(String userId, LongestStreakCallback callback) {
        firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, List<TaskStatus>> tasksByDate = new HashMap<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        long timestamp = task.getLastActionTimestamp();
                        if (timestamp > 0) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(timestamp);
                            // normalizacija na početak dana
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MILLISECOND, 0);

                            String date = sdf.format(cal.getTime());
                            tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task.getStatus());
                        }
                    }

                    int longestStreak = 0, currentStreak = 0;
                    Calendar calendar = Calendar.getInstance();

                    // idi unazad do 365 dana
                    for (int i = 365; i >= 0; i--) {
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.add(Calendar.DAY_OF_YEAR, -i);
                        String date = sdf.format(calendar.getTime());

                        if (tasksByDate.containsKey(date)) {
                            List<TaskStatus> statuses = tasksByDate.get(date);

                            boolean allFinished = statuses.stream().allMatch(s -> s == TaskStatus.FINISHED);
                            boolean hasFinished = statuses.stream().anyMatch(s -> s == TaskStatus.FINISHED);

                            if (hasFinished && allFinished) {
                                currentStreak++;
                                longestStreak = Math.max(longestStreak, currentStreak);
                            } else if (!allFinished) {
                                currentStreak = 0;
                            }
                        }
                        // ako nema zadataka tog dana, streak se ne prekida
                    }

                    callback.onSuccess(new StatisticsDto.LongestStreakResult(
                            longestStreak, longestStreak + " dana zaredom"
                    ));
                })
                .addOnFailureListener(callback::onFailure);
    }


    public interface LongestStreakCallback {
        void onSuccess(StatisticsDto.LongestStreakResult result);
        void onFailure(Exception e);
    }

    // Vraća broj započetih i završenih specijalnih misija
    public void getSpecialMissionsStats(String userId, SpecialMissionsCallback callback) {
        // 1. Prvo nađi savez korisnika
        firestore.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(allianceQuery -> {
                    if (allianceQuery.isEmpty()) {
                        // Korisnik nije u savezu
                        callback.onSuccess(new StatisticsDto.SpecialMissionsStats(0, 0));
                        return;
                    }

                    String allianceId = allianceQuery.getDocuments().get(0).getId();

                    // 2. Učitaj sve misije tog saveza
                    firestore.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .get()
                            .addOnSuccessListener(missionsQuery -> {
                                int totalStarted = missionsQuery.size();
                                int totalCompleted = 0;

                                for (QueryDocumentSnapshot doc : missionsQuery) {
                                    Long bossHp = doc.getLong("bossHp");
                                    Boolean active = doc.getBoolean("active");

                                    // Misija je završena ako je bossHp == 0 i active == false
                                    if (bossHp != null && bossHp == 0 &&
                                        active != null && !active) {
                                        totalCompleted++;
                                    }
                                }

                                Log.d(TAG, "Specijalne misije - Započeto: " + totalStarted + ", Završeno: " + totalCompleted);
                                callback.onSuccess(new StatisticsDto.SpecialMissionsStats(totalStarted, totalCompleted));
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface SpecialMissionsCallback {
        void onSuccess(StatisticsDto.SpecialMissionsStats stats);
        void onFailure(Exception e);
    }
}
