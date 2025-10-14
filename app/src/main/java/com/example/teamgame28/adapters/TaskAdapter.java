package com.example.teamgame28.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.viewmodels.TaskViewModel;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    private final Context context;
    private final List<Task> tasks;
    private final OnItemClickListener listener;
    private final TaskViewModel viewModel;

    public interface OnItemClickListener {
        void onItemClick(Task task, View view);
    }

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks,
                       OnItemClickListener listener, TaskViewModel viewModel) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = new ArrayList<>(tasks);
        this.listener = listener;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            // ✅ koristiš layout za jedan red u listi, ne ceo fragment
            listItem = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_task, parent, false);

        }

        Task currentTask = tasks.get(position);

        // 🔹 Povezivanje sa elementima iz list_item_task.xml
        TextView taskTitle = listItem.findViewById(R.id.textViewTaskTitle);
        TextView taskCategory = listItem.findViewById(R.id.textViewTaskCategory);
        TextView taskStatus = listItem.findViewById(R.id.textViewTaskStatus);
        Button actionButton = listItem.findViewById(R.id.buttonTaskAction);

        // 🔹 Naziv zadatka
        if (currentTask.isRecurring()) {
            taskTitle.setText(currentTask.getTitle() + " (ponavljajući)");
        } else {
            taskTitle.setText(currentTask.getTitle());
        }

        // 🔹 Kategorija i boja
        taskCategory.setText(currentTask.getCategoryName());
        String color = currentTask.getCategoryColor();
        if (color != null && !color.isEmpty()) {
            try {
                taskCategory.setBackgroundColor(Color.parseColor(color));
            } catch (IllegalArgumentException e) {
                taskCategory.setBackgroundColor(Color.GRAY);
            }
        } else {
            taskCategory.setBackgroundColor(Color.GRAY);
        }

        // 🔹 Status (TaskStatus → string)
        taskStatus.setText(currentTask.getStatus().name());

        // 🔹 Ako je urađen, sakrij dugme
        if (currentTask.getStatus() == TaskStatus.FINISHED) {
            actionButton.setVisibility(View.GONE);
        } else {
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setText("Mark Done");

            actionButton.setOnClickListener(v -> {
                viewModel.markTaskDone(currentTask);
                Toast.makeText(context, "Zadatak označen kao urađen", Toast.LENGTH_SHORT).show();
            });
        }

        // 🔹 Klik na ceo item
        listItem.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(currentTask, v);
        });

        return listItem;
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Nullable
    @Override
    public Task getItem(int position) {
        return tasks.get(position);
    }

    public void setTasks(List<Task> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);
        notifyDataSetChanged();
    }
}
