package com.example.teamgame28.service;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;

import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    public TaskService(Context context) {
        this.repository = TaskRepository.getInstance(context);
    }
    // 🔹 Vrati sve taskove korisnika
    public LiveData<List<Task>> getTasksByUser(String userId) {
        return repository.getTasksByUser(userId);
    }

    // 🔹 Dodaj novi task
    public void addTask(Task task) {
        task.setTotalXp(task.getDifficultyXp() + task.getImportanceXp());
        task.setCreationTimestamp(System.currentTimeMillis());

        if (task.isRecurring()) {
            repository.addRecurringTaskInstances(task);
        } else {
            repository.addTaskWithXpLimit(task);
        }
    }


    // 🔹 Ažuriraj postojeći task
    public void updateTask(Task task) {
        task.setLastActionTimestamp(System.currentTimeMillis());
        repository.updateTask(task);
    }

    // 🔹 Obriši task
    public void deleteTask(Task task) {
        repository.deleteTask(task);
    }


    // 🔹 (opciono) Izračunaj procenat urađenih
    public LiveData<Double> getSuccessRate(String userId) {
        MutableLiveData<Double> successRate = new MutableLiveData<>();

        repository.getTasksByUser(userId).observeForever(tasks -> {
            if (tasks == null || tasks.isEmpty()) {
                successRate.setValue(0.0);
            } else {
                long completed = tasks.stream()
                        .filter(t -> t.getStatus() == TaskStatus.FINISHED)
                        .count();
                successRate.setValue((completed * 100.0) / tasks.size());
            }
        });

        return successRate;
    }
}
