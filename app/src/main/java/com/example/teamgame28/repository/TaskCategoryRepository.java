package com.example.teamgame28.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.TaskCategory;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskCategoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "categories";

    // 🔹 Dodavanje nove kategorije
    public void addCategory(TaskCategory category) {
        String id = db.collection(COLLECTION_NAME).document().getId();
        category.setId(id);

        db.collection(COLLECTION_NAME)
                .document(id)
                .set(category);
    }

    // 🔹 Dohvatanje svih kategorija za korisnika (kao LiveData)
    public MutableLiveData<List<TaskCategory>> getCategoriesByUser(String userId) {
        MutableLiveData<List<TaskCategory>> liveData = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<TaskCategory> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        TaskCategory category = document.toObject(TaskCategory.class);
                        categories.add(category);
                    }
                    liveData.setValue(categories);
                });

        return liveData;
    }

    // 🔹 Ažuriranje postojeće kategorije
    public void updateCategory(TaskCategory category) {
        db.collection(COLLECTION_NAME)
                .document(category.getId())
                .set(category);
    }

    // 🔹 Brisanje kategorije
    public void deleteCategory(String categoryId) {
        db.collection(COLLECTION_NAME)
                .document(categoryId)
                .delete();
    }
}
