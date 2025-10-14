package com.example.teamgame28.service;

import androidx.lifecycle.LiveData;

import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.repository.TaskCategoryRepository;

import java.util.List;

public class TaskCategoryService {

    private final TaskCategoryRepository repository;

    public TaskCategoryService() {
        this.repository = new TaskCategoryRepository();
    }

    // 🔹 Vrati sve kategorije korisnika
    public LiveData<List<TaskCategory>> getCategoriesByUser(String userId) {
        return repository.getCategoriesByUser(userId);
    }

    // 🔹 Dodaj novu kategoriju
    public void addCategory(TaskCategory category) {
        // Možeš dodati neku logiku – npr. ako je naziv prazan, ne dozvoli upis
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            repository.addCategory(category);
        }
    }

    // 🔹 Ažuriraj postojeću kategoriju
    public void updateCategory(TaskCategory category) {
        repository.updateCategory(category);
    }

    // 🔹 Obriši kategoriju
    public void deleteCategory(String categoryId) {
        repository.deleteCategory(categoryId);
    }
}
