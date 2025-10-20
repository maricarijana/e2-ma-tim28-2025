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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
            listItem = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        Task currentTask = tasks.get(position);

        TextView taskTitle = listItem.findViewById(R.id.textViewTaskTitle);
        TextView taskCategory = listItem.findViewById(R.id.textViewTaskCategory);
        TextView taskStatus = listItem.findViewById(R.id.textViewTaskStatus);
        TextView taskDates = listItem.findViewById(R.id.textViewTaskDates);
        TextView taskInterval = listItem.findViewById(R.id.textViewTaskInterval);
        TextView recurringDates = listItem.findViewById(R.id.textViewRecurringDates);
        TextView taskDifficulty = listItem.findViewById(R.id.textViewTaskDifficulty);
        TextView taskImportance = listItem.findViewById(R.id.textViewTaskImportance);
        TextView taskXp = listItem.findViewById(R.id.textViewTaskXp);
        TextView taskXpCounted = listItem.findViewById(R.id.textViewXpCounted);
        if (currentTask.isXpCounted()) {
            taskXpCounted.setText("✅ XP je obračunat");
            taskXpCounted.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            taskXpCounted.setText("⚠️ XP nije obračunat (kvota ispunjena)");
            taskXpCounted.setTextColor(Color.parseColor("#F44336"));
        }

        Button actionButton = listItem.findViewById(R.id.buttonTaskAction);

        // 🔹 Naziv
        taskTitle.setText(currentTask.isRecurring()
                ? currentTask.getTitle() + " (ponavljajući)"
                : currentTask.getTitle());

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

        // 📅 Datum početka i završetka
        SimpleDateFormat sdf = new SimpleDateFormat("d.M.yyyy", Locale.getDefault());
        if (currentTask.getStartDate() != null && currentTask.getEndDate() != null) {
            taskDates.setText("Datum: " + sdf.format(currentTask.getStartDate()) +
                    " - " + sdf.format(currentTask.getEndDate()));
        } else if (currentTask.getStartDate() != null) {
            taskDates.setText("Datum: " + sdf.format(currentTask.getStartDate()));
        } else {
            taskDates.setText("");
        }

        // 🔁 Interval i jedinica
        if (currentTask.isRecurring() && currentTask.getInterval() > 0) {
            taskInterval.setVisibility(View.VISIBLE);
            taskInterval.setText("Interval: " + currentTask.getInterval() + " " + currentTask.getIntervalUnit());
        } else {
            taskInterval.setVisibility(View.GONE);
        }

        // 🗓️ Lista ponavljanja (samo prvih 5 datuma)
        if (currentTask.isRecurring() && currentTask.getRecurringDates() != null && !currentTask.getRecurringDates().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            List<Long> dates = currentTask.getRecurringDates();
            int limit = Math.min(dates.size(), 5);
            for (int i = 0; i < limit; i++) {
                sb.append(new SimpleDateFormat("d.M.", Locale.getDefault()).format(new Date(dates.get(i)))).append(", ");
            }
            if (dates.size() > 5) sb.append("...");
            String list = sb.substring(0, sb.length() - 2);
            recurringDates.setText("📅 Zadatak se definiše za: " + list);
            recurringDates.setVisibility(View.VISIBLE);
        } else {
            recurringDates.setVisibility(View.GONE);
        }

        // 🎯 Težina i bitnost (tekstualni opis)
        String[] difficultyLabels = {"Veoma lak (1 XP)", "Lak (3 XP)", "Težak (7 XP)", "Ekstremno težak (20 XP)"};
        String[] importanceLabels = {"Normalan (1 XP)", "Važan (3 XP)", "Ekstremno važan (10 XP)", "Specijalan (100 XP)"};
        int[] difficultyXpValues = {1, 3, 7, 20};
        int[] importanceXpValues = {1, 3, 10, 100};

        int diffIndex = Arrays.binarySearch(difficultyXpValues, currentTask.getDifficultyXp());
        int impIndex = Arrays.binarySearch(importanceXpValues, currentTask.getImportanceXp());

        taskDifficulty.setText("Težina: " + (diffIndex >= 0 ? difficultyLabels[diffIndex] :
                currentTask.getDifficultyXp() + " XP"));
        taskImportance.setText("Bitnost: " + (impIndex >= 0 ? importanceLabels[impIndex] :
                currentTask.getImportanceXp() + " XP"));

        // 💪 Ukupna XP vrednost
        taskXp.setText("Ukupna vrednost zadatka: " + currentTask.getTotalXp() + " XP");

        // 🔹 Status
        taskStatus.setText("Status: " + currentTask.getStatus().name());

        // 🔹 Dugme za akciju
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

        // 🔹 Klik na ceo red
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
