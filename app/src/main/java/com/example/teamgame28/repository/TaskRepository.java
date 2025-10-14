package com.example.teamgame28.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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



    // ✅ Brisanje zadatka
    public void deleteTask(String taskId) {
        db.collection(COLLECTION_NAME)
                .document(taskId)
                .delete();
    }

}
