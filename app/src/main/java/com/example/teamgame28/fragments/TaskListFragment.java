package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        listView = view.findViewById(R.id.listViewTasks);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        adapter = new TaskAdapter(requireContext(), tasks, (task, v) -> {
            Toast.makeText(requireContext(),
                    "Kliknuo si na: " + task.getTitle(),
                    Toast.LENGTH_SHORT).show();
        }, viewModel);

        listView.setAdapter(adapter);

        // 🔹 Posmatraj podatke iz ViewModel-a
        viewModel.getTasksByUser("12345") // ili loggedUserId
                .observe(getViewLifecycleOwner(), taskList -> {
                    tasks.clear();
                    if (taskList != null) tasks.addAll(taskList);
                    adapter.setTasks(tasks);
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
