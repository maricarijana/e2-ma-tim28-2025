package com.example.teamgame28.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.viewmodels.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateTaskFragment extends Fragment {

    private EditText inputTitle, inputDescription, inputStartDate, inputEndDate, inputTime;
    private Spinner spinnerCategory, spinnerFrequency, spinnerDifficulty, spinnerImportance;
    private Button buttonSave;
    private TaskViewModel taskViewModel;
    private String taskId = null;
    private EditText inputInterval;
    private Spinner spinnerIntervalUnit;
    private LinearLayout recurringOptionsLayout;
    public static CreateTaskFragment newInstance(String taskId) {
        CreateTaskFragment fragment = new CreateTaskFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_task, container, false);
        Bundle args = getArguments();
        if (args != null && args.containsKey("taskId")) {
            String taskId = args.getString("taskId");
            loadTaskForEditing(taskId);
        }

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
        inputInterval = view.findViewById(R.id.inputInterval);
        spinnerIntervalUnit = view.findViewById(R.id.spinnerIntervalUnit);
        recurringOptionsLayout = view.findViewById(R.id.recurringOptionsLayout);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        setupSpinners();
        setupPickers();

        if (getArguments() != null && getArguments().containsKey("taskId")) {
            taskId = getArguments().getString("taskId");
            loadTaskForEditing(taskId);
        }

        // 🟢 Klik na “Sačuvaj”
        buttonSave.setOnClickListener(v -> {
            if (taskId != null) {
                updateExistingTask(taskId); // ako postoji taskId — ažurira
            } else {
                saveTask(); // ako nema taskId — kreira novi
            }
        });


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

        spinnerIntervalUnit.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Dan", "Nedelja"}));

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                recurringOptionsLayout.setVisibility(selected.equals("Ponavljajući") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPickers() {
        inputStartDate.setOnClickListener(v -> showDatePicker(inputStartDate));
        inputEndDate.setOnClickListener(v -> showDatePicker(inputEndDate));
        inputTime.setOnClickListener(v -> showTimePicker(inputTime));
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) ->
                target.setText(day + "/" + (month + 1) + "/" + year),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hour, minute) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

// ---------------- CREATE / UPDATE -----------------

    private void saveNewTask() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Unesi naziv zadatka", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = createTaskFromInputs();
        taskViewModel.addTask(task);
        Toast.makeText(requireContext(), "Zadatak uspešno sačuvan!", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void updateExistingTask(String taskId) {
        TaskRepository.getInstance(requireContext())
                .getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task == null) return;

                    if (task.getStatus() == TaskStatus.FINISHED) {
                        Toast.makeText(requireContext(), "Ne možeš menjati završeni zadatak.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    task.setTitle(inputTitle.getText().toString());
                    task.setDescription(inputDescription.getText().toString());
                    task.setTime(inputTime.getText().toString());
                    task.setDifficultyXp(getSelectedDifficultyXp());
                    task.setImportanceXp(getSelectedImportanceXp());
                    task.calculateTotalXp();
                    task.setLastActionTimestamp(System.currentTimeMillis());

                    TaskRepository.getInstance(requireContext()).updateTask(task);
                    Toast.makeText(requireContext(), "Zadatak uspešno ažuriran!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }

    private Task createTaskFromInputs() {
        Task task = new Task();
        task.setUserId("12345");
        task.setTitle(inputTitle.getText().toString());
        task.setDescription(inputDescription.getText().toString());
        task.setTime(inputTime.getText().toString());
        task.setStatus(TaskStatus.ACTIVE);
        task.setCreationTimestamp(System.currentTimeMillis());
        task.setDifficultyXp(getSelectedDifficultyXp());
        task.setImportanceXp(getSelectedImportanceXp());
        task.calculateTotalXp();
        return task;
    }

// ---------------- LOAD EXISTING -----------------

    private void loadTaskForEditing(String taskId) {
        TaskRepository.getInstance(requireContext())
                .getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task == null) return;

                    inputTitle.setText(task.getTitle());
                    inputDescription.setText(task.getDescription());
                    inputTime.setText(task.getTime());
                    spinnerDifficulty.setSelection(getDifficultyIndex(task.getDifficultyXp()));
                    spinnerImportance.setSelection(getImportanceIndex(task.getImportanceXp()));

                    Toast.makeText(requireContext(), "Uređuješ zadatak: " + task.getTitle(), Toast.LENGTH_SHORT).show();

                    if (task.getStatus() == TaskStatus.FINISHED) {
                        disableEditing();
                        Toast.makeText(requireContext(), "Završene zadatke nije moguće menjati.", Toast.LENGTH_LONG).show();
                    }
                });
    }

// ---------------- HELPERS -----------------

    private int getSelectedDifficultyXp() {
        int[] difficultyXp = {1, 3, 7, 20};
        return difficultyXp[spinnerDifficulty.getSelectedItemPosition()];
    }

    private int getSelectedImportanceXp() {
        int[] importanceXp = {1, 3, 10, 100};
        return importanceXp[spinnerImportance.getSelectedItemPosition()];
    }

    private int getDifficultyIndex(int value) {
        int[] options = {1, 3, 7, 20};
        for (int i = 0; i < options.length; i++) if (options[i] == value) return i;
        return 0;
    }

    private int getImportanceIndex(int value) {
        int[] options = {1, 3, 10, 100};
        for (int i = 0; i < options.length; i++) if (options[i] == value) return i;
        return 0;
    }

    private void disableEditing() {
        inputTitle.setEnabled(false);
        inputDescription.setEnabled(false);
        inputTime.setEnabled(false);
        spinnerDifficulty.setEnabled(false);
        spinnerImportance.setEnabled(false);
        buttonSave.setEnabled(false);
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
        task.setStatus(TaskStatus.ACTIVE);
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

        // 🔁 Ako je zadatak ponavljajući
        if (task.isRecurring()) {
            String intervalStr = inputInterval.getText().toString().trim();
            if (!intervalStr.isEmpty()) {
                task.setInterval(Integer.parseInt(intervalStr));
            }
            task.setIntervalUnit(spinnerIntervalUnit.getSelectedItem().toString());

            // ✅ Generiši sve datume ponavljanja
            List<Date> recurringDates = generateRecurringDates(
                    task.getStartDate(),
                    task.getEndDate(),
                    task.getInterval(),
                    task.getIntervalUnit()
            );
            if (task.isRecurring()) {
                if (task.getRecurringGroupId() == null || task.getRecurringGroupId().isEmpty()) {
                    task.setRecurringGroupId(java.util.UUID.randomUUID().toString());
                }
            } else {
                task.setRecurringGroupId(null);
            }
            // ✅ Pretvori u timestamp listu za Firestore
            List<Long> timestamps = new ArrayList<>();
            for (Date d : recurringDates) {
                timestamps.add(d.getTime());
            }
            task.setRecurringDates(timestamps);
        }

        // 💾 Sačuvaj u bazu
        taskViewModel.addTask(task);
        Toast.makeText(requireContext(), "Zadatak uspešno sačuvan!", Toast.LENGTH_SHORT).show();

        clearInputs();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private List<Date> generateRecurringDates(Date start, Date end, int interval, String unit) {
        List<Date> dates = new ArrayList<>();
        if (start == null || end == null || interval <= 0) return dates;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while (!calendar.getTime().after(end)) {
            dates.add(calendar.getTime());
            if (unit.equalsIgnoreCase("Dan")) {
                calendar.add(Calendar.DAY_OF_MONTH, interval);
            } else if (unit.equalsIgnoreCase("Nedelja")) {
                calendar.add(Calendar.WEEK_OF_YEAR, interval);
            }
        }
        return dates;
    }


    private void clearInputs() {
        inputTitle.setText("");
        inputDescription.setText("");
        inputTime.setText("");
        inputStartDate.setText("");
        inputEndDate.setText("");
    }


}
