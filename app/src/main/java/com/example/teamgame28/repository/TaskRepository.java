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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private static TaskRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "tasks";

    // Callback za XP dodelu (poziva ga TaskService)
    public interface OnTaskFinishedListener {
        void onTaskFinished(String userId, int xpAmount);
    }

    private OnTaskFinishedListener taskFinishedListener;

    // ✅ Singleton (da možeš zvati getInstance)
    private TaskRepository(Context context) { }

    public static TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void setOnTaskFinishedListener(OnTaskFinishedListener listener) {
        this.taskFinishedListener = listener;
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
                        task.setTotalXp(0); // ❗ XP se više ne dodeljuje, kvota je ispunjena
                        Log.d("XP_DEBUG", "⚠️ Kvota ispunjena — XP neće biti dodeljen");
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

        if (diff == 1 && imp == 1) return 5;   // Veoma lak + Normalan
        if (diff == 3 && imp == 3) return 5;   // Lak + Važan
        if (diff == 7 && imp == 10) return 2;  // Težak + Ekstremno važan
        if (diff == 20) return 1;              // Ekstremno težak
        if (imp == 100) return 1;              // Specijalan
        return 5;                              // Default fallback
    }

    private boolean isDaily(Task task) {
        int diff = task.getDifficultyXp();
        int imp = task.getImportanceXp();
        return (diff == 1 && imp == 1) || (diff == 3 && imp == 3) || (diff == 7 && imp == 10);
    }

    private boolean isWeekly(Task task) {
        return task.getDifficultyXp() == 20; // ekstremno težak
    }

    private boolean isMonthly(Task task) {
        return task.getImportanceXp() == 100; // specijalan
    }


    public void deleteTask(Task task) {
        // 🔹 1. Ne dozvoli brisanje završenih zadataka
        if (task.getStatus() == TaskStatus.FINISHED || task.getStatus() == TaskStatus.CANCELLED) {
            Log.w("Firestore", "❌ Završen zadatak ne može biti obrisan: " + task.getTitle());
            return;
        }

        // 🔹 2. Ako zadatak nije ponavljajući — samo obriši dokument
        if (!task.isRecurring()) {
            db.collection(COLLECTION_NAME)
                    .document(task.getId())
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            Log.d("Firestore", "✅ Zadatak obrisan: " + task.getTitle()))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "❌ Greška pri brisanju zadatka", e));
            return;
        }

        // 🔹 3. Ako jeste ponavljajući — obriši sva buduća ponavljanja
        long now = System.currentTimeMillis();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("recurringGroupId", task.getRecurringGroupId())
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        Task t = doc.toObject(Task.class);
                        if (t.getStartDate() != null && t.getStartDate().getTime() >= now) {
                            db.collection(COLLECTION_NAME)
                                    .document(t.getId())
                                    .delete();
                        }
                    }
                    Log.d("Firestore", "✅ Obrisana buduća ponavljanja za grupu: " + task.getRecurringGroupId());
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Greška pri brisanju ponavljajućih zadataka", e));
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
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Task task = snapshot.toObject(Task.class);
                        if (task == null) return;

                        task.setStatus(newStatus);
                        task.setLastActionTimestamp(System.currentTimeMillis());

                        // 🔹 1️⃣ XP se ne obračunava za pauzirane i otkazane zadatke
                        if (newStatus == TaskStatus.PAUSED || newStatus == TaskStatus.CANCELLED) {
                            task.setXpCounted(false);
                            task.setTotalXp(0);
                        }

                        // 🔹 2️⃣ Ako je zadatak označen kao urađen
                        else if (newStatus == TaskStatus.FINISHED) {
                            task.setXpCounted(true);

                            // ➕ Obavesti TaskService da dodeli XP kroz callback
                            if (taskFinishedListener != null) {
                                taskFinishedListener.onTaskFinished(task.getUserId(), task.getTotalXp());
                            }

                            Log.d("XP_SYSTEM", "✅ Task završen: " + task.getTitle() +
                                    " — " + task.getTotalXp() + " XP za korisnika " + task.getUserId());
                        }

                        // 🔹 3️⃣ Ažuriraj u bazi
                        db.collection(COLLECTION_NAME)
                                .document(taskId)
                                .set(task)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("TaskRepository", "✅ Status ažuriran: " + newStatus))
                                .addOnFailureListener(e ->
                                        Log.e("TaskRepository", "❌ Greška pri ažuriranju statusa", e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("TaskRepository", "❌ Greška pri preuzimanju zadatka", e));
    }


    /**
     * Računa success rate za zadatke u trenutnoj etapi (od currentLevelStartTimestamp).
     * Automatski isključuje pauzirane, otkazane i zadatke preko kvote (xpCounted = false).
     */
    public void getSuccessRateForCurrentStage(String userId, long currentLevelStartTimestamp, SuccessRateCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("creationTimestamp", currentLevelStartTimestamp)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> validTasks = new ArrayList<>();
                    int finishedCount = 0;

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Task task = document.toObject(Task.class);

                        // Isključi zadatke preko kvote, pauzirane i otkazane (xpCounted = false)
                        if (!task.isXpCounted()) {
                            continue;
                        }

                        validTasks.add(task);

                        if (task.getStatus() == TaskStatus.FINISHED) {
                            finishedCount++;
                        }
                    }

                    // Izračunaj success rate
                    double successRate;
                    if (validTasks.isEmpty()) {
                        successRate = 67.0; // Default ako nema zadataka u etapi
                    } else {
                        successRate = ((double) finishedCount / validTasks.size()) * 100.0;
                    }

                    Log.d("TaskRepository", "📊 Stage success rate: " + finishedCount + "/" + validTasks.size() + " = " + successRate + "%");
                    callback.onSuccess(successRate);
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskRepository", "❌ Error calculating success rate", e);
                    callback.onSuccess(67.0); // Default u slučaju greške
                });
    }

    public interface SuccessRateCallback {
        void onSuccess(double successRate);
    }

    public void deleteFutureRecurringTasks(Task task) {
        if (task.getRecurringGroupId() == null) {
            // Ako zadatak nema groupId (tj. nije deo grupe ponavljanja) — briši samo njega
            deleteTask(task);
            return;
        }

        long now = System.currentTimeMillis();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("recurringGroupId", task.getRecurringGroupId())
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        Task t = doc.toObject(Task.class);

                        // Ako je startDate u budućnosti — obriši
                        if (t.getStartDate() != null && t.getStartDate().getTime() >= now) {
                            db.collection(COLLECTION_NAME)
                                    .document(t.getId())
                                    .delete();
                        }
                    }

                    Log.d("Firestore", "✅ Obrisana buduća ponavljanja za grupu: " + task.getRecurringGroupId());
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Greška pri brisanju ponavljajućih zadataka", e));
    }

    // 🔁 Dodaj sve instance ponavljajućeg zadatka (uz provjeru baze)
    public void addRecurringTaskInstances(Task baseTask) {
        if (baseTask.getRecurringDates() == null || baseTask.getRecurringDates().isEmpty()) {
            addTaskWithXpLimit(baseTask);
            return;
        }

        if (baseTask.getRecurringGroupId() == null || baseTask.getRecurringGroupId().isEmpty()) {
            baseTask.setRecurringGroupId(java.util.UUID.randomUUID().toString());
        }

        int diff = baseTask.getDifficultyXp();
        int imp = baseTask.getImportanceXp();
        int maxCount = getXpLimit(baseTask);

        // 🔹 Odredi vremenski period (isti kao u addTaskWithXpLimit)
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        if (isDaily(baseTask)) {
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_MONTH, 1);
        } else if (isWeekly(baseTask)) {
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (isMonthly(baseTask)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
        }

        // 🔍 1️⃣ Prebroj postojeće zadatke u bazi za taj period
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", baseTask.getUserId())
                .whereEqualTo("difficultyXp", diff)
                .whereEqualTo("importanceXp", imp)
                .whereGreaterThanOrEqualTo("creationTimestamp", start.getTimeInMillis())
                .whereLessThan("creationTimestamp", end.getTimeInMillis())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long existingCount = querySnapshot.size();
                    int remaining = (int) (maxCount - existingCount);
                    if (remaining < 0) remaining = 0;

                    Log.d("XP_DEBUG", "📦 Već postoji " + existingCount + " zadataka ove vrste. Preostalo: " + remaining);

                    // 🔁 2️⃣ Kreiraj instance i dodijeli XP prema remaining broju
                    int instanceIndex = 0;
                    for (Long timestamp : baseTask.getRecurringDates()) {
                        Task copy = createTaskCopy(baseTask, timestamp);
                        copy.setCreationTimestamp(System.currentTimeMillis() + instanceIndex);
                        instanceIndex++;

                        if (remaining > 0) {
                            copy.setXpCounted(true);
                            copy.setTotalXp(copy.getDifficultyXp() + copy.getImportanceXp());
                            remaining--;
                            Log.d("XP_DEBUG", "✅ XP obračunat za " + copy.getTitle());
                        } else {
                            copy.setXpCounted(false);
                            copy.setTotalXp(0);
                            Log.d("XP_DEBUG", "⚠️ Kvota ispunjena — XP neće biti dodeljen");
                        }

                        addTask(copy);
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                })
                .addOnFailureListener(e -> Log.e("XP_DEBUG", "❌ Firestore greška: " + e.getMessage()));
    }

    private Task createTaskCopy(Task baseTask, long timestamp) {
        Task copy = new Task();
        copy.setUserId(baseTask.getUserId());
        copy.setTitle(baseTask.getTitle());
        copy.setDescription(baseTask.getDescription());
        copy.setCategoryId(baseTask.getCategoryId());
        copy.setCategoryName(baseTask.getCategoryName());
        copy.setCategoryColor(baseTask.getCategoryColor());
        copy.setFrequency(baseTask.getFrequency());
        copy.setRecurring(true);
        copy.setRecurringGroupId(baseTask.getRecurringGroupId());
        copy.setInterval(baseTask.getInterval());
        copy.setIntervalUnit(baseTask.getIntervalUnit());
        copy.setStartDate(new Date(timestamp));
        copy.setEndDate(baseTask.getEndDate());
        copy.setTime(baseTask.getTime());
        copy.setStatus(TaskStatus.ACTIVE);
        copy.setDifficultyXp(baseTask.getDifficultyXp());
        copy.setImportanceXp(baseTask.getImportanceXp());
        copy.calculateTotalXp();
        copy.setCreationTimestamp(System.currentTimeMillis());
        return copy;
    }

    private String getPeriodKey(Date date, int diff, int imp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        if (isDaily(new Task(diff, imp))) {
            // npr. "2025-10-21"
            return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
        } else if (isWeekly(new Task(diff, imp))) {
            // npr. "2025-W42"
            return cal.get(Calendar.YEAR) + "-W" + cal.get(Calendar.WEEK_OF_YEAR);
        } else if (isMonthly(new Task(diff, imp))) {
            // npr. "2025-M10"
            return cal.get(Calendar.YEAR) + "-M" + (cal.get(Calendar.MONTH) + 1);
        }
        return "default";
    }


}
