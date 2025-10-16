package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.TaskAdapter;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.viewmodels.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment  extends Fragment {

    private ListView listView;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private TaskViewModel viewModel;
    private Button btnJednokratni, btnPonavljajuci;
    private List<Task> allTasks = new ArrayList<>();
    private FloatingActionButton fabAddTask;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        listView = view.findViewById(R.id.listViewTasks);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        adapter = new TaskAdapter(requireContext(), tasks, (task, v) -> {

            // 👉 Klik na zadatak otvara TaskDetailFragment
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task.getId());

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();

        }, viewModel);

        listView.setAdapter(adapter);
        btnJednokratni = view.findViewById(R.id.btnJednokratni);
        btnPonavljajuci = view.findViewById(R.id.btnPonavljajuci);
        fabAddTask = view.findViewById(R.id.fabAddTask);

        // 🔹 Posmatraj podatke iz ViewModel-a
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "12345";

        viewModel.getTasksByUser(currentUserId)
                .observe(getViewLifecycleOwner(), taskList -> {
                    tasks.clear();
                    allTasks.clear();

                    if (taskList != null) {
                        long now = System.currentTimeMillis();

                        for (Task t : taskList) {
                            // 🔹 Uzimamo samo zadatke koji još traju ili su u budućnosti
                            if (t.getStartDate() != null && t.getStartDate().getTime() + 86400000 >= now) {
                                allTasks.add(t);
                            }
                            // Ako zadatak nema startDate, ali ima dueDate — koristi to
                            else if (t.getDueDate() != null && t.getDueDate().getTime() >= now) {
                                allTasks.add(t);
                            }
                        }

                        tasks.addAll(allTasks);
                    }

                    adapter.setTasks(tasks);
                });

        btnJednokratni.setOnClickListener(v -> {
            List<Task> filtered = new ArrayList<>();
            for (Task t : allTasks) {
                if (!t.isRecurring()) filtered.add(t);
            }
            adapter.setTasks(filtered);

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "Nema jednokratnih zadataka.", Toast.LENGTH_SHORT).show();
            }
        });

        btnPonavljajuci.setOnClickListener(v -> {
            List<Task> filtered = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.isRecurring()) filtered.add(t);
            }
            adapter.setTasks(filtered);

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "Nema ponavljajućih zadataka.", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔹 FAB onClick - otvara CreateTaskFragment
        fabAddTask.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateTaskFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadTasks() {
        TaskRepository.getInstance(getContext())
                .getAllTasks()
                .observe(getViewLifecycleOwner(), taskList -> {
                    if (taskList != null) {
                        tasks.clear();
                        tasks.addAll(taskList);
                        adapter.setTasks(tasks);
                    }
                });
    }
}
