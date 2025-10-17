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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.LevelingService;
import com.example.teamgame28.viewmodels.CategoryViewModel;
import com.example.teamgame28.viewmodels.TaskViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateTaskFragment extends Fragment {

    // UI Components
    private TextView textHeader;
    private EditText inputTitle, inputDescription;
    private EditText inputSingleDate, inputTime;
    private EditText inputStartDate, inputEndDate, inputRecurringTime;
    private EditText inputInterval;
    private Spinner spinnerCategory, spinnerFrequency, spinnerDifficulty, spinnerImportance, spinnerIntervalUnit;
    private LinearLayout singleTaskLayout, recurringTaskLayout;
    private Button buttonSave;

    // ViewModels
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;

    // Data
    private String taskId = null;
    private List<TaskCategory> allCategories = new ArrayList<>();
    private Map<String, TaskCategory> categoryMap = new HashMap<>(); // Za brzo pronalaženje po imenu
    private int userLevel = 0; // Trenutni nivo korisnika

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

        // Initialize UI components
        initializeViews(view);

        // Initialize ViewModels
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Load categories from Firestore
        loadCategories();

        // Load user level and setup spinners
        loadUserLevelAndSetupSpinners();

        // Setup date/time pickers
        setupPickers();

        // Check if editing existing task
        if (getArguments() != null && getArguments().containsKey("taskId")) {
            taskId = getArguments().getString("taskId");
            textHeader.setText("Uredi Zadatak");
            loadTaskForEditing(taskId);
        }

        // Save button click
        buttonSave.setOnClickListener(v -> {
            if (taskId != null) {
                updateExistingTask(taskId);
            } else {
                saveTask();
            }
        });

        return view;
    }

    private void initializeViews(View view) {
        textHeader = view.findViewById(R.id.textHeader);
        inputTitle = view.findViewById(R.id.inputTitle);
        inputDescription = view.findViewById(R.id.inputDescription);
        inputSingleDate = view.findViewById(R.id.inputSingleDate);
        inputTime = view.findViewById(R.id.inputTime);
        inputStartDate = view.findViewById(R.id.inputStartDate);
        inputEndDate = view.findViewById(R.id.inputEndDate);
        inputRecurringTime = view.findViewById(R.id.inputRecurringTime);
        inputInterval = view.findViewById(R.id.inputInterval);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerFrequency = view.findViewById(R.id.spinnerFrequency);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        spinnerImportance = view.findViewById(R.id.spinnerImportance);
        spinnerIntervalUnit = view.findViewById(R.id.spinnerIntervalUnit);
        singleTaskLayout = view.findViewById(R.id.singleTaskLayout);
        recurringTaskLayout = view.findViewById(R.id.recurringTaskLayout);
        buttonSave = view.findViewById(R.id.buttonSaveTask);
    }

    private void loadCategories() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "12345";

        categoryViewModel.getCategoriesByUser(currentUserId).observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                allCategories = categories;
                categoryMap.clear();

                // Kreirati listu imena za spinner
                List<String> categoryNames = new ArrayList<>();
                for (TaskCategory category : categories) {
                    categoryNames.add(category.getName());
                    categoryMap.put(category.getName(), category);
                }

                // Postavi adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        categoryNames
                );
                spinnerCategory.setAdapter(adapter);
            } else {
                Toast.makeText(requireContext(),
                        "Nemaš ni jednu kategoriju! Prvo kreiraj kategoriju u \"Kategorije\" tabu.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUserLevelAndSetupSpinners() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "12345";

        UserRepository userRepository = new UserRepository();
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(com.example.teamgame28.model.UserProfile userProfile) {
                userLevel = userProfile.getLevel();
                setupSpinners();
            }

            @Override
            public void onFailure(Exception e) {
                userLevel = 0; // Fallback na level 0
                setupSpinners();
            }
        });
    }

    private void setupSpinners() {
        // Frequency spinner
        spinnerFrequency.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Jednokratni", "Ponavljajući"}));

        // Difficulty spinner - dinamički na osnovu user levela
        spinnerDifficulty.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                LevelingService.getDifficultyLabels(userLevel)));

        // Importance spinner - dinamički na osnovu user levela
        spinnerImportance.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                LevelingService.getImportanceLabels(userLevel)));

        // Interval unit spinner
        spinnerIntervalUnit.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Dan", "Nedelja"}));

        // Frequency change listener - toggle layouts
        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isRecurring = position == 1; // "Ponavljajući"
                singleTaskLayout.setVisibility(isRecurring ? View.GONE : View.VISIBLE);
                recurringTaskLayout.setVisibility(isRecurring ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupPickers() {
        // Single task date picker
        inputSingleDate.setOnClickListener(v -> showDatePicker(inputSingleDate));

        // Recurring task date pickers
        inputStartDate.setOnClickListener(v -> showDatePicker(inputStartDate));
        inputEndDate.setOnClickListener(v -> showDatePicker(inputEndDate));

        // Time pickers
        inputTime.setOnClickListener(v -> showTimePicker(inputTime));
        inputRecurringTime.setOnClickListener(v -> showTimePicker(inputRecurringTime));
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

    private void saveTask() {
        // Validacija
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Unesi naziv zadatka!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Prvo kreiraj kategoriju!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Get current user
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "12345";

        // 🔹 Kreiraj task
        Task task = new Task();
        task.setUserId(currentUserId);
        task.setTitle(title);
        task.setDescription(inputDescription.getText().toString().trim());
        task.setStatus(TaskStatus.ACTIVE);
        task.setCreationTimestamp(System.currentTimeMillis());
        task.setLastActionTimestamp(System.currentTimeMillis());

        // 🔹 Kategorija
        String selectedCategoryName = spinnerCategory.getSelectedItem().toString();
        TaskCategory selectedCategory = categoryMap.get(selectedCategoryName);
        if (selectedCategory != null) {
            task.setCategoryId(selectedCategory.getId());
            task.setCategoryName(selectedCategory.getName());
            task.setCategoryColor(selectedCategory.getColor());
        }

        // 🔹 Frekvencija
        boolean isRecurring = spinnerFrequency.getSelectedItemPosition() == 1;
        task.setRecurring(isRecurring);
        task.setFrequency(isRecurring ? "Ponavljajući" : "Jednokratni");

        // 🔹 XP vrednosti
        task.setDifficultyXp(getSelectedDifficultyXp());
        task.setImportanceXp(getSelectedImportanceXp());
        task.calculateTotalXp();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (isRecurring) {
            // 🔁 Ponavljajući zadatak
            try {
                Date startDate = sdf.parse(inputStartDate.getText().toString().trim());
                Date endDate = sdf.parse(inputEndDate.getText().toString().trim());
                String time = inputRecurringTime.getText().toString().trim();
                String intervalStr = inputInterval.getText().toString().trim();

                if (startDate == null || endDate == null || time.isEmpty() || intervalStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Popuni sva polja za ponavljajući zadatak!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int interval = Integer.parseInt(intervalStr);
                String intervalUnit = spinnerIntervalUnit.getSelectedItem().toString();

                task.setStartDate(startDate);
                task.setEndDate(endDate);
                task.setTime(time);
                task.setInterval(interval);
                task.setIntervalUnit(intervalUnit);
                task.setRecurringGroupId(java.util.UUID.randomUUID().toString());

                // 🔹 Generiši sve datume ponavljanja
                List<Date> recurringDates = generateRecurringDates(startDate, endDate, interval, intervalUnit);
                List<Long> timestamps = new ArrayList<>();
                for (Date d : recurringDates) timestamps.add(d.getTime());
                task.setRecurringDates(timestamps);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Greška u formatiranju datuma!", Toast.LENGTH_SHORT).show();
                return;
            }

        } else {
            // 🟢 Jednokratni zadatak
            try {
                Date singleDate = sdf.parse(inputSingleDate.getText().toString().trim());
                String time = inputTime.getText().toString().trim();

                if (singleDate == null || time.isEmpty()) {
                    Toast.makeText(requireContext(), "Popuni datum i vreme za jednokratni zadatak!", Toast.LENGTH_SHORT).show();
                    return;
                }

                task.setStartDate(singleDate);
                task.setDueDate(singleDate);
                task.setTime(time);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Greška u formatiranju datuma!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 💾 Sačuvaj u bazu
        taskViewModel.addTask(task);
        Toast.makeText(requireContext(), "Zadatak uspešno sačuvan!", Toast.LENGTH_SHORT).show();

        // 🔙 Povratak nazad
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

    private void updateExistingTask(String taskId) {
        TaskRepository.getInstance(requireContext())
                .getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task == null) return;

                    if (task.getStatus() == TaskStatus.FINISHED) {
                        Toast.makeText(requireContext(), "Ne možeš menjati završeni zadatak.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (task.getStatus() == TaskStatus.UNFINISHED) {
                        Toast.makeText(requireContext(), "Ne možeš menjati neurađeni zadatak.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (task.getStatus() == TaskStatus.CANCELLED) {
                        Toast.makeText(requireContext(), "Ne možeš menjati otkazan zadatak.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (task.getDueDate() != null && task.getDueDate().getTime() < System.currentTimeMillis()
                            && task.getStatus() == TaskStatus.ACTIVE) {
                        Toast.makeText(requireContext(),
                                "Ne mogu se menjati vremenski završeni zadaci.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (task.isRecurring() && task.getStartDate() != null &&
                            task.getStartDate().getTime() < System.currentTimeMillis()) {
                        Toast.makeText(requireContext(),
                                "Prošle instance ponavljajućih zadataka se ne mogu menjati.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Update fields
                    task.setTitle(inputTitle.getText().toString().trim());
                    task.setDescription(inputDescription.getText().toString().trim());
                    task.setDifficultyXp(getSelectedDifficultyXp());
                    task.setImportanceXp(getSelectedImportanceXp());
                    task.calculateTotalXp();
                    task.setLastActionTimestamp(System.currentTimeMillis());

                    // Update time
                    if (task.isRecurring()) {
                        task.setTime(inputRecurringTime.getText().toString().trim());
                    } else {
                        task.setTime(inputTime.getText().toString().trim());
                    }

                    TaskRepository.getInstance(requireContext()).updateTask(task);
                    Toast.makeText(requireContext(), "Zadatak uspešno ažuriran!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }

    private void loadTaskForEditing(String taskId) {
        TaskRepository.getInstance(requireContext())
                .getTaskById(taskId)
                .observe(getViewLifecycleOwner(), task -> {
                    if (task == null) return;

                    inputTitle.setText(task.getTitle());
                    inputDescription.setText(task.getDescription());
                    spinnerDifficulty.setSelection(getDifficultyIndex(task.getDifficultyXp()));
                    spinnerImportance.setSelection(getImportanceIndex(task.getImportanceXp()));

                    if (task.getStatus() == TaskStatus.FINISHED || task.getStatus() == TaskStatus.UNFINISHED ||
                            task.getStatus() == TaskStatus.CANCELLED) {
                        disableEditing();
                        Toast.makeText(requireContext(), "Ovaj zadatak se ne može menjati.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int getSelectedDifficultyXp() {
        return LevelingService.getDifficultyXpForLevel(spinnerDifficulty.getSelectedItemPosition(), userLevel);
    }

    private int getSelectedImportanceXp() {
        return LevelingService.getImportanceXpForLevel(spinnerImportance.getSelectedItemPosition(), userLevel);
    }

    private int getDifficultyIndex(int value) {
        // Pronađi indeks za zadatu XP vrednost na trenutnom levelu
        for (int i = 0; i < 4; i++) {
            if (LevelingService.getDifficultyXpForLevel(i, userLevel) == value) return i;
        }
        return 0;
    }

    private int getImportanceIndex(int value) {
        // Pronađi indeks za zadatu XP vrednost na trenutnom levelu
        for (int i = 0; i < 4; i++) {
            if (LevelingService.getImportanceXpForLevel(i, userLevel) == value) return i;
        }
        return 0;
    }

    private void disableEditing() {
        inputTitle.setEnabled(false);
        inputDescription.setEnabled(false);
        inputSingleDate.setEnabled(false);
        inputTime.setEnabled(false);
        inputStartDate.setEnabled(false);
        inputEndDate.setEnabled(false);
        inputRecurringTime.setEnabled(false);
        inputInterval.setEnabled(false);
        spinnerCategory.setEnabled(false);
        spinnerFrequency.setEnabled(false);
        spinnerDifficulty.setEnabled(false);
        spinnerImportance.setEnabled(false);
        spinnerIntervalUnit.setEnabled(false);
        buttonSave.setEnabled(false);
    }
}
