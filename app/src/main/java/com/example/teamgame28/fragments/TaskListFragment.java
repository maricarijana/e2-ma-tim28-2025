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

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment  extends Fragment {

    private ListView listView;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private TaskViewModel viewModel;
    private Button btnJednokratni, btnPonavljajuci;
    private List<Task> allTasks = new ArrayList<>();


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

        // 🔹 Posmatraj podatke iz ViewModel-a
        viewModel.getTasksByUser("12345") // ili loggedUserId
                .observe(getViewLifecycleOwner(), taskList -> {
                    tasks.clear();
                    allTasks.clear();
                    allTasks.addAll(taskList);
                    adapter.setTasks(tasks);
                });

        btnJednokratni.setOnClickListener(v -> {
            List<Task> filtered = new ArrayList<>();
            for (Task t : allTasks) {
                if (!t.isRecurring()) filtered.add(t);
            }
            adapter.setTasks(filtered);
        });

        btnPonavljajuci.setOnClickListener(v -> {
            List<Task> filtered = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.isRecurring()) filtered.add(t);
            }
            adapter.setTasks(filtered);
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
