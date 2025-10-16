package com.example.teamgame28.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;

import java.util.Date;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";

    private TextView titleText, descText, categoryText, dateText, xpText, statusText;
    private Button btnDone, btnCancel, btnPause, btnEdit, btnDelete,btnResume;

    private String taskId;
    private TaskRepository repo;
    private Task task; // 🔹 trenutno učitani zadatak

    public static TaskDetailFragment newInstance(String taskId) {
        Bundle args = new Bundle();
        args.putString(ARG_TASK_ID, taskId);
        TaskDetailFragment fragment = new TaskDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_detail, container, false);

        titleText = v.findViewById(R.id.textTitle);
        descText = v.findViewById(R.id.textDescription);
        categoryText = v.findViewById(R.id.textCategory);
        dateText = v.findViewById(R.id.textDates);
        xpText = v.findViewById(R.id.textXp);
        statusText = v.findViewById(R.id.textStatus);

        btnDone = v.findViewById(R.id.btnDone);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnPause = v.findViewById(R.id.btnPause);
        btnEdit = v.findViewById(R.id.btnEdit);
        btnDelete = v.findViewById(R.id.btnDelete);
        btnResume = v.findViewById(R.id.btnResume);

        repo = TaskRepository.getInstance(requireContext());
        taskId = getArguments().getString(ARG_TASK_ID);

        loadTask();

        btnDone.setOnClickListener(vw -> updateStatus(TaskStatus.FINISHED));
        btnCancel.setOnClickListener(vw -> updateStatus(TaskStatus.CANCELLED));
        btnPause.setOnClickListener(vw -> updateStatus(TaskStatus.PAUSED));
        btnResume.setOnClickListener(vw -> updateStatus(TaskStatus.ACTIVE)); // 🔹 novo

        btnEdit.setOnClickListener(vw -> openEditScreen());
        btnDelete.setOnClickListener(vw -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Brisanje zadatka")
                    .setMessage("Da li želiš da obrišeš ovaj zadatak?" +
                            (task.isRecurring() ? "\n\nNapomena: Biće obrisana i sva buduća ponavljanja." : ""))
                    .setPositiveButton("Obriši", (dialog, which) -> {
                        if (task.getStatus() == TaskStatus.FINISHED) {
                            Toast.makeText(requireContext(),
                                    "Završeni zadaci se ne mogu obrisati.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (task.isRecurring()) {
                            repo.deleteFutureRecurringTasks(task);
                        } else {
                            repo.deleteTask(task);
                        }

                        Toast.makeText(requireContext(), "Zadatak obrisan.", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .setNegativeButton("Otkaži", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        return v;
    }

    private void loadTask() {
        repo.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task == null) return;
            this.task = task;
            if (task.getStatus() == TaskStatus.UNFINISHED) {
                disableAllActions();
                Toast.makeText(requireContext(),
                        "Neurađeni zadaci se ne mogu menjati, brisati ni označavati.",
                        Toast.LENGTH_LONG).show();
            }

            if (task.getStatus() == TaskStatus.PAUSED) {
                btnResume.setVisibility(View.VISIBLE); // može da aktivira
                btnPause.setVisibility(View.GONE);     // ne može ponovo da pauzira
            } else {
                btnResume.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
            }
            titleText.setText(task.getTitle());
            descText.setText(task.getDescription());
            categoryText.setText(task.getCategoryName() + "  (" + task.getCategoryColor() + ")");
            xpText.setText("XP: " + task.getTotalXp());
            statusText.setText("Status: " + task.getStatus().name());
            dateText.setText("Kreiran: " + new Date(task.getCreationTimestamp()).toString());
        });
    }

    private void updateStatus(TaskStatus newStatus) {
        if (task == null) return;

        // 🔸 1. Ako zadatak nije ACTIVE — ne može se menjati
        if (task.getStatus() != TaskStatus.ACTIVE && !(task.getStatus() == TaskStatus.PAUSED && newStatus == TaskStatus.ACTIVE)) {
            Toast.makeText(getContext(), "Samo aktivan ili pauziran zadatak može menjati status.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔸 2. Ako je stariji od 3 dana — postaje NEURAĐEN i zaključava se
        if (task.getStartDate() != null) {
            long diff = System.currentTimeMillis() - task.getStartDate().getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if (days > 3) {
                repo.updateTaskStatus(taskId, TaskStatus.UNFINISHED);
                Toast.makeText(getContext(),
                        "Zadatak je istekao i automatski označen kao neurađen.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 🔸 3. Inače — normalna promena statusa
        repo.updateTaskStatus(taskId, newStatus);
        Toast.makeText(getContext(), "Status promenjen na: " + newStatus.name(), Toast.LENGTH_SHORT).show();
    }


    private void openEditScreen() {
        Fragment editFragment = CreateTaskFragment.newInstance(taskId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void disableAllActions() {
        btnDone.setEnabled(false);
        btnCancel.setEnabled(false);
        btnPause.setEnabled(false);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
    }

}
