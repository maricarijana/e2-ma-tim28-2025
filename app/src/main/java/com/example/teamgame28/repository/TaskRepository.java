package com.example.teamgame28.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskRepository {
    private static TaskRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "tasks";
    // ✅ Singleton (da možeš zvati getInstance)
    private TaskRepository(Context context) { }

    public static TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ✅ Dohvatanje svih zadataka (za TaskListFragment)
    public LiveData<List<Task>> getAllTasks() {
        MutableLiveData<List<Task>> liveData = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Task task = document.toObject(Task.class);
                        tasks.add(task);
                    }
                    liveData.setValue(tasks);
                });

        return liveData;
    }

    // ✅ Ako ti treba filtrirano po korisniku
    public LiveData<List<Task>> getTasksByUser(String userId) {
        MutableLiveData<List<Task>> liveData = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Task task = document.toObject(Task.class);
                        tasks.add(task);
                    }
                    liveData.setValue(tasks);
                });

        return liveData;
    }

    // ✅ Dodavanje zadatka
    public void addTask(Task task) {
        String id = db.collection(COLLECTION_NAME).document().getId();
        task.setId(id);
        db.collection(COLLECTION_NAME)
                .document(id)
                .set(task);
    }
    public void updateTask(Task task) {
        db.collection(COLLECTION_NAME)
                .document(task.getId())
                .set(task);
    }

    public void addTaskWithXpLimit(Task task) {
        Log.d("XP_DEBUG", "➡️ Dodajem task: " + task.getTitle());

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        int maxCount = getXpLimit(task);

        // ⏰ Odredi vremenski period kvote
        if (isDaily(task)) {
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_MONTH, 1);
        } else if (isWeekly(task)) {
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (isMonthly(task)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
        }

        Log.d("XP_DEBUG", "🕒 Period: " + start.getTime() + " → " + end.getTime());
        Log.d("XP_DEBUG", "📊 Kvota dozvoljena: " + maxCount);

        // 🧮 Definiši "tip" zadatka po težini i bitnosti
        int diff = task.getDifficultyXp();
        int imp = task.getImportanceXp();

        // 🔍 Napravi query koji broji koliko takvih zadataka već ima u periodu
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", task.getUserId())
                .whereEqualTo("difficultyXp", diff)
                .whereEqualTo("importanceXp", imp)
                .whereGreaterThanOrEqualTo("creationTimestamp", start.getTimeInMillis())
                .whereLessThan("creationTimestamp", end.getTimeInMillis())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long count = querySnapshot.size();
                    Log.d("XP_DEBUG", "📦 Pronađeno " + count + " zadataka ove kategorije u periodu.");

                    if (count < maxCount) {
                        task.setXpCounted(true);
                        Log.d("XP_DEBUG", "✅ XP obračunat (pun) za: " + task.getTitle());
                    } else {
                        task.setXpCounted(false);
                        task.setTotalXp(task.getImportanceXp());
                        Log.d("XP_DEBUG", "⚠️ Kvota ispunjena — XP samo za bitnost (" + task.getTotalXp() + ")");
                    }

                    addTask(task);
                })
                .addOnFailureListener(e -> {
                    Log.e("XP_DEBUG", "❌ Firestore greška: " + e.getMessage());
                    // Fallback — upiši task i označi kao XP obračunat (da ne gubiš podatke)
                    task.setXpCounted(true);
                    addTask(task);
                });
    }


    private int getXpLimit(Task task) {
        int diff = task.getDifficultyXp();
        int imp = task.getImportanceXp();

        if (diff == 1 || imp == 1) return 5; // Veoma lak / Normalan
        if (diff == 3 || imp == 3) return 5; // Lak / Važan
        if (diff == 7 || imp == 10) return 2; // Težak / Ekstremno važan
        if (diff == 20) return 1; // Ekstremno težak
        if (imp == 100) return 1; // Specijalan
        return 5;
    }

    private boolean isDaily(Task task) {
        return task.getDifficultyXp() <= 7 && task.getImportanceXp() <= 10;
    }

    private boolean isWeekly(Task task) {
        return task.getDifficultyXp() == 20;
    }

    private boolean isMonthly(Task task) {
        return task.getImportanceXp() == 100;
    }


    // ✅ Brisanje zadatka
    public void deleteTask(String taskId) {
        db.collection(COLLECTION_NAME)
                .document(taskId)
                .delete();
    }

    // 🔹 Dohvatanje zadatka po ID-ju
    public LiveData<Task> getTaskById(String taskId) {
        MutableLiveData<Task> liveData = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .document(taskId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        liveData.setValue(null);
                        return;
                    }
                    Task task = snapshot.toObject(Task.class);
                    liveData.setValue(task);
                });

        return liveData;
    }

    // 🔹 Ažuriranje samo statusa zadatka
    public void updateTaskStatus(String taskId, TaskStatus newStatus) {
        db.collection(COLLECTION_NAME)
                .document(taskId)
                .update("status", newStatus, "lastActionTimestamp", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d("TaskRepository", "✅ Status ažuriran: " + newStatus))
                .addOnFailureListener(e -> Log.e("TaskRepository", "❌ Greška: " + e.getMessage()));
    }


}
