package com.example.teamgame28.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.service.TaskCategoryService;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final TaskCategoryService categoryService;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.categoryService = new TaskCategoryService();
    }

    // 🔹 Dohvati sve kategorije korisnika
    public LiveData<List<TaskCategory>> getCategoriesByUser(String userId) {
        return categoryService.getCategoriesByUser(userId);
    }

    // 🔹 Dodaj novu kategoriju
    public void addCategory(TaskCategory category) {
        categoryService.addCategory(category);
    }

    // 🔹 Ažuriraj kategoriju
    public void updateCategory(TaskCategory category) {
        categoryService.updateCategory(category);
    }

    // 🔹 Obriši kategoriju
    public void deleteCategory(String categoryId) {
        categoryService.deleteCategory(categoryId);
    }
}
