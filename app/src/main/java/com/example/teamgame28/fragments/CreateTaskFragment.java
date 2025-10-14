package com.example.teamgame28.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.viewmodels.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateTaskFragment extends Fragment {

    private EditText inputTitle, inputDescription, inputStartDate, inputEndDate, inputTime;
    private Spinner spinnerCategory, spinnerFrequency, spinnerDifficulty, spinnerImportance;
    private Button buttonSave;
    private TaskViewModel taskViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_task, container, false);

        inputTitle = view.findViewById(R.id.inputTitle);
        inputDescription = view.findViewById(R.id.inputDescription);
        inputStartDate = view.findViewById(R.id.inputStartDate);
        inputEndDate = view.findViewById(R.id.inputEndDate);
        inputTime = view.findViewById(R.id.inputTime);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerFrequency = view.findViewById(R.id.spinnerFrequency);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        spinnerImportance = view.findViewById(R.id.spinnerImportance);
        buttonSave = view.findViewById(R.id.buttonSaveTask);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        setupSpinners();
        setupPickers();

        buttonSave.setOnClickListener(v -> saveTask());

        return view;
    }

    private void setupSpinners() {
        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Zdravlje", "Učenje", "Zabava", "Sređivanje"}));

        spinnerFrequency.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Jednokratni", "Ponavljajući"}));

        spinnerDifficulty.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Veoma lak (1 XP)", "Lak (3 XP)", "Težak (7 XP)", "Ekstremno težak (20 XP)"}));

        spinnerImportance.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Normalan (1 XP)", "Važan (3 XP)", "Ekstremno važan (10 XP)", "Specijalan (100 XP)"}));
    }

    private void setupPickers() {
        inputStartDate.setOnClickListener(v -> showDatePicker(inputStartDate));
        inputEndDate.setOnClickListener(v -> showDatePicker(inputEndDate));
        inputTime.setOnClickListener(v -> showTimePicker(inputTime));
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            target.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hour, minute) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Unesi naziv zadatka", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(inputDescription.getText().toString());
        task.setCategoryName(spinnerCategory.getSelectedItem().toString());
        task.setFrequency(spinnerFrequency.getSelectedItem().toString());
        task.setRecurring(spinnerFrequency.getSelectedItem().toString().equals("Ponavljajući"));
        task.setTime(inputTime.getText().toString());
        task.setStatus(TaskStatus.PAUSED);
        task.setUserId("12345");
        task.setCreationTimestamp(System.currentTimeMillis());

        // 🎨 BOJA kategorije
        switch (task.getCategoryName()) {
            case "Zdravlje": task.setCategoryColor("#4CAF50"); break; // zelena
            case "Učenje": task.setCategoryColor("#2196F3"); break;   // plava
            case "Zabava": task.setCategoryColor("#FFC107"); break;   // žuta
            case "Sređivanje": task.setCategoryColor("#9C27B0"); break; // ljubičasta
            default: task.setCategoryColor("#9E9E9E"); // siva
        }

        // 📅 Datum početka / kraja (ako su uneti)
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (!inputStartDate.getText().toString().isEmpty())
                task.setStartDate(sdf.parse(inputStartDate.getText().toString()));
            if (!inputEndDate.getText().toString().isEmpty())
                task.setEndDate(sdf.parse(inputEndDate.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ⏰ Ako je jednokratni zadatak — postavi dueDate
        if (task.getFrequency().equals("Jednokratni")) {
            task.setDueDate(task.getStartDate());
        }

        // 🎯 XP vrednosti
        int[] difficultyXp = {1, 3, 7, 20};
        int[] importanceXp = {1, 3, 10, 100};
        task.setDifficultyXp(difficultyXp[spinnerDifficulty.getSelectedItemPosition()]);
        task.setImportanceXp(importanceXp[spinnerImportance.getSelectedItemPosition()]);
        task.calculateTotalXp();

        // ✅ Sačuvaj
        taskViewModel.addTask(task);
        Toast.makeText(requireContext(), "Zadatak uspešno sačuvan!", Toast.LENGTH_SHORT).show();

        clearInputs();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void clearInputs() {
        inputTitle.setText("");
        inputDescription.setText("");
        inputTime.setText("");
        inputStartDate.setText("");
        inputEndDate.setText("");
    }

}
