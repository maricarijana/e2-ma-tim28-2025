package com.example.teamgame28.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.teamgame28.repository.UserRepository; // Dodaj import
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.service.TaskService;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskService taskService;
    private final UserRepository userRepository; // Dodaj UserRepository
    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.taskService = new TaskService(application);
        this.userRepository = new UserRepository();

    }

    public LiveData<List<Task>> getTasksByUser(String userId) {
        return taskService.getTasksByUser(userId);
    }

    public void addTask(Task task) {
        taskService.addTask(task);
    }

    public void markTaskDone(Task task) {
        // Provera da se ne dodeli XP dvaput
        if (task.getStatus() == TaskStatus.FINISHED) {
            Log.w("TaskViewModel", "Zadatak je već završen, XP se ne dodaje ponovo.");
            return;
        }

        // 1. AŽURIRAJ STATUS ZADATKA
        task.setStatus(TaskStatus.FINISHED);
        taskService.updateTask(task);

        // 2. DODELI XP KORISNIKU
        String userId = task.getUserId();
        int xpToAdd = task.getTotalXp();

        if (userId != null && !userId.isEmpty() && xpToAdd > 0) {
            if (task.isXpCounted()) {
                Log.d("TaskViewModel", "✅ XP je obračunat, dodajem " + xpToAdd + " XP korisniku " + userId);
                userRepository.addXpToUser(userId, xpToAdd);
            } else {
                Log.w("TaskViewModel", "⚠️ XP nije obračunat (kvota ispunjena), ne dodajem XP korisniku " + userId);
            }
        } else {
            Log.e("TaskViewModel", "❌ Ne mogu dodati XP: userId je null ili xpToAdd je 0.");
        }
    }

    public void deleteTask(Task task) {
        taskService.deleteTask(task);
    }
}
