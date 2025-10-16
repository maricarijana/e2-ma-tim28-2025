package com.example.teamgame28.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.teamgame28.R;
import com.example.teamgame28.fragments.CreateTaskFragment;
import com.example.teamgame28.fragments.ProfileFragment;
import com.example.teamgame28.fragments.TaskCalendarFragment;
import com.example.teamgame28.fragments.TaskListFragment;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.UserService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Toolbar (gornji meni)
        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // 🔹 Firebase autentifikacija
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // 🔹 Navigation Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        userRepository = new UserRepository();

        // 🔹 Postavite toggle dugme za otvaranje/zatvaranje drawer-a
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Učitajte podatke korisnika u drawer header
        loadUserDataIntoDrawer(currentUser.getUid());

        // 🔹 Drawer meni navigacija
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_settings) {
                Toast.makeText(this, "Podešavanja - uskoro!", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // 🔹 Bottom Navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        FloatingActionButton fab = findViewById(R.id.fab_add_task);

        // 🔹 Početni prikaz (lista zadataka)
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        // 🔹 Navigacija između tabova
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

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

        // 🔹 FAB za dodavanje novog zadatka
        fab.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateTaskFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // 🔹 Učitavanje korisničkih podataka u navigation drawer header
    private void loadUserDataIntoDrawer(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null && navigationView.getHeaderView(0) != null) {
                    ImageView avatarImageView = navigationView.getHeaderView(0).findViewById(R.id.drawer_avatar);
                    TextView usernameTextView = navigationView.getHeaderView(0).findViewById(R.id.drawer_username);
                    TextView emailTextView = navigationView.getHeaderView(0).findViewById(R.id.drawer_email);

                    // Postavite avatar
                    String avatarName = user.getAvatar();
                    if (avatarName != null && !avatarName.isEmpty()) {
                        // Konvertuj u lowercase jer su drawable resursi u lowercase formatu
                        String resourceName = avatarName.toLowerCase();
                        int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                        if (resId != 0) {
                            avatarImageView.setImageResource(resId);
                        }
                    }

                    // Postavite korisničko ime i email
                    usernameTextView.setText(user.getUsername());
                    emailTextView.setText(user.getEmail());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Greška pri učitavanju korisnika", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Toolbar meni (Profil ikona)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 🔹 Toolbar meni akcije (klik na profil ikonu)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            // Navigacija ka profilu korisnika
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 🔹 Logout funkcija
    private void logoutUser() {
        UserService userService = new UserService();
        userService.logout();

        Toast.makeText(this, "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // 🔹 Rukovanje back dugmetom kada je drawer otvoren
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
