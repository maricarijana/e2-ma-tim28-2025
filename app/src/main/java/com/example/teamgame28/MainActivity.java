package com.example.teamgame28;
import com.example.teamgame28.R;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.teamgame28.fragments.CreateTaskFragment;
import com.example.teamgame28.fragments.TaskCalendarFragment;
import com.example.teamgame28.fragments.TaskListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        FloatingActionButton fab = findViewById(R.id.fab_add_task);

        // 🔹 Početni ekran — lista zadataka
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        // 🔹 Navigacija između prikaza

        bottomNavigation.setOnItemSelectedListener(item -> {
            // Uzimamo ID stavke
            int itemId = item.getItemId();

            // Koristimo if-else if umesto switch
            if (itemId == R.id.nav_tasks) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskCalendarFragment())
                        .commit();
                return true;
            }

            return false;
        });


        // 🔹 FAB otvara formu za kreiranje zadatka
        fab.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateTaskFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}


//package com.example.teamgame28;
//
//import android.os.Bundle;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.teamgame28.fragments.CreateTaskFragment;
//import com.example.teamgame28.fragments.TaskCalendarFragment;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//
//public class MainActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // 🔹 Prikaži kalendar pri pokretanju aplikacije
//        if (savedInstanceState == null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new TaskCalendarFragment())
//                    .commit();
//        }
//
//        // 🔹 Na klik FAB-a otvori formu za kreiranje zadatka
//        FloatingActionButton fab = findViewById(R.id.fab_add_task);
//        fab.setOnClickListener(v -> {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new CreateTaskFragment())
//                    .addToBackStack(null)
//                    .commit();
//        });
//    }
//}
