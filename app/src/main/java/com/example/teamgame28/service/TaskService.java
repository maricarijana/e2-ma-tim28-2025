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
    private final UserService userService;

    public TaskService(Context context) {
        this.repository = TaskRepository.getInstance(context);
        this.userService = new UserService();

        // Postavi callback za XP dodelu kada se task završi
        repository.setOnTaskFinishedListener((userId, xpAmount) -> {
            userService.addXpToUserWithLevelUp(userId, xpAmount);
        });
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

        // ===== ⬇️ IZMENA OVDE (Tačka 2) ⬇️ =====
        // ✅ Ako je zadatak završen — proveri da li je deo specijalne misije
        if (task.getStatus() == TaskStatus.FINISHED) {
            SpecialTaskMissionService specialService = new SpecialTaskMissionService();

            int diff = task.getDifficultyXp();
            int imp = task.getImportanceXp();

            // Definišemo šta su "Ostali zadaci" prema specifikaciji (7.3)
            // To su: Težak (7 XP), Ekstremno težak (20 XP), Ekstremno važan (10 XP), Specijalan (100 XP)
            // [cite: 74, 75, 79, 80]
            boolean isOtherTask = (diff == 7 || diff == 20 || imp == 10 || imp == 100);

            if (isOtherTask) {
                // Poziv za "Ostale zadatke" (max 6) - 4 HP
                specialService.recordOtherTask(task);
            } else {
                // Poziv za "Veoma lak, lak, normalan ili važan" (max 10) - 1/2 HP
                // (Ovo 'else' hvata sve što nije 'isOtherTask',
                // što su po specifikaciji lakši zadaci)
                specialService.recordTaskCompletion(task);
            }
        }
        // ===== ⬆️ KRAJ IZMENE ⬆️ =====
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