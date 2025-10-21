package com.example.teamgame28.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.CategoryAdapter;
import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.viewmodels.CategoryViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementFragment extends Fragment implements CategoryAdapter.OnCategoryActionListener {

    private CategoryViewModel categoryViewModel;
    private CategoryAdapter categoryAdapter;
    private RecyclerView recyclerView;
    private TextView textNoCategories;
    private Button btnAddCategory;

    private List<TaskCategory> allCategories = new ArrayList<>();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 Inicijalizacija komponenti
        recyclerView = view.findViewById(R.id.recyclerViewCategories);
        textNoCategories = view.findViewById(R.id.textNoCategories);
        btnAddCategory = view.findViewById(R.id.btnAddCategory);

        // 🔹 Firebase korisnik
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "12345"; // fallback

        // 🔹 ViewModel
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // 🔹 Adapter
        categoryAdapter = new CategoryAdapter(this);
        recyclerView.setAdapter(categoryAdapter);

        // 🔹 Observe kategorija
        observeCategories();

        // 🔹 Dugme za dodavanje
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void observeCategories() {
        categoryViewModel.getCategoriesByUser(currentUserId).observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories != null ? categories : new ArrayList<>();
            categoryAdapter.setCategories(allCategories);

            if (allCategories.isEmpty()) {
                textNoCategories.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoCategories.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showAddCategoryDialog() {
        showCategoryDialog(null);
    }

    private void showCategoryDialog(@Nullable TaskCategory existingCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 🔹 UI elementi
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        EditText editCategoryName = dialogView.findViewById(R.id.editCategoryName);
        TextView textSelectedColor = dialogView.findViewById(R.id.textSelectedColor);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // 🔹 Boje
        int[] colorViewIds = {R.id.color1, R.id.color2, R.id.color3, R.id.color4, R.id.color5,
                R.id.color6, R.id.color7, R.id.color8, R.id.color9, R.id.color10};

        final String[] selectedColor = {null};

        // 🔹 Ako je edit mode
        boolean isEditMode = existingCategory != null;
        if (isEditMode) {
            dialogTitle.setText("Izmeni Kategoriju");
            editCategoryName.setText(existingCategory.getName());
            selectedColor[0] = existingCategory.getColor();
            textSelectedColor.setText("Izabrana boja: " + existingCategory.getColor());
        }

        // 🔹 Klik na boje
        for (int colorId : colorViewIds) {
            View colorView = dialogView.findViewById(colorId);
            colorView.setOnClickListener(v -> {
                String color = (String) v.getTag();

                // 🔴 Validacija: proveri da li je boja već zauzeta (osim ako je to trenutna kategorija)
                if (isColorTaken(color, existingCategory)) {
                    Toast.makeText(requireContext(),
                            "Ova boja je već zauzeta! Izaberite drugu boju.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedColor[0] = color;
                textSelectedColor.setText("Izabrana boja: " + color);

                // Označi izabranu boju (border ili alpha)
                for (int id : colorViewIds) {
                    dialogView.findViewById(id).setAlpha(0.5f);
                }
                v.setAlpha(1.0f);
            });
        }

        // 🔹 Sačuvaj
        btnSave.setOnClickListener(v -> {
            String name = editCategoryName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Unesite naziv kategorije!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.length() > 20) {
                Toast.makeText(requireContext(), "Naziv može imati maksimalno 20 karaktera!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isCategoryNameTaken(name, existingCategory)) {
                Toast.makeText(requireContext(), "Kategorija sa ovim nazivom već postoji!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedColor[0] == null) {
                Toast.makeText(requireContext(), "Izaberite boju!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                // 🔄 Ažuriraj postojeću
                existingCategory.setName(name);
                existingCategory.setColor(selectedColor[0]);
                categoryViewModel.updateCategory(existingCategory);
                Toast.makeText(requireContext(), "Kategorija ažurirana!", Toast.LENGTH_SHORT).show();
            } else {
                // ➕ Dodaj novu
                TaskCategory newCategory = new TaskCategory(null, currentUserId, name, selectedColor[0]);
                categoryViewModel.addCategory(newCategory);
                Toast.makeText(requireContext(), "Kategorija dodata!", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        // 🔹 Otkaži
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Proverava da li je boja već zauzeta od strane druge kategorije.
     * @param color Boja koja se proverava
     * @param currentCategory Trenutna kategorija (null ako je nova)
     * @return true ako je boja zauzeta, false ako nije
     */
    private boolean isColorTaken(String color, TaskCategory currentCategory) {
        for (TaskCategory category : allCategories) {
            // Ako editujemo postojeću kategoriju, preskočimo je
            if (currentCategory != null && category.getId().equals(currentCategory.getId())) {
                continue;
            }
            // Ako neka druga kategorija već ima tu boju
            if (category.getColor().equalsIgnoreCase(color)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEditCategory(TaskCategory category) {
        showCategoryDialog(category);
    }

    @Override
    public void onDeleteCategory(TaskCategory category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Potvrda brisanja")
                .setMessage("Da li ste sigurni da želite da obrišete kategoriju \"" + category.getName() + "\"?")
                .setPositiveButton("Da", (dialog, which) -> {
                    categoryViewModel.deleteCategory(category.getId());
                    Toast.makeText(requireContext(), "Kategorija obrisana!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Ne", null)
                .show();
    }

    private boolean isCategoryNameTaken(String name, TaskCategory currentCategory) {
        for (TaskCategory category : allCategories) {
            if (currentCategory != null && category.getId().equals(currentCategory.getId())) {
                continue;
            }
            if (category.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

}
