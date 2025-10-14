package com.example.teamgame28;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.teamgame28.fragments.CreateTaskFragment;
import com.example.teamgame28.fragments.TaskListFragment;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskCategoryRepository;
import com.example.teamgame28.repository.TaskRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Prikaži listu zadataka pri pokretanju
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        // 🔹 Na klik FAB-a otvori formu za kreiranje zadatka
        FloatingActionButton fab = findViewById(R.id.fab_add_task);
        fab.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateTaskFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}