package com.example.teamgame28.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.teamgame28.R;
import com.example.teamgame28.fragments.CategoryManagementFragment;
import com.example.teamgame28.fragments.ProfileFragment;
import com.example.teamgame28.fragments.TaskCalendarFragment;
import com.example.teamgame28.fragments.TaskListFragment;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.UserService;
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

        // 🔹 Toolbar setup
        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // 🔹 Firebase Auth
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // 🔹 Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        userRepository = new UserRepository();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Učitavanje korisničkih podataka u header
        loadUserDataIntoDrawer(currentUser.getUid());

        // 🔹 Navigation Drawer — glavna navigacija
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_tasks) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
            } else if (itemId == R.id.nav_calendar) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskCalendarFragment())
                        .commit();
            } else if (itemId == R.id.nav_categories) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CategoryManagementFragment())
                        .commit();
            } else if (itemId == R.id.nav_profile) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            } else if (itemId == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // 🔹 Početni fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        // 🔹 Nova OnBackPressedDispatcher logika (umesto onBackPressed)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Omogući da se normalno obradi "back" kad drawer nije otvoren
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // 🔹 Učitavanje korisnika u drawer header
    private void loadUserDataIntoDrawer(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null && navigationView.getHeaderView(0) != null) {
                    ImageView avatarImageView = navigationView.getHeaderView(0).findViewById(R.id.drawer_avatar);
                    TextView usernameTextView = navigationView.getHeaderView(0).findViewById(R.id.drawer_username);
                    TextView emailTextView = navigationView.getHeaderView(0).findViewById(R.id.drawer_email);

                    String avatarName = user.getAvatar();
                    if (avatarName != null && !avatarName.isEmpty()) {
                        int resId = getResources().getIdentifier(avatarName.toLowerCase(), "drawable", getPackageName());
                        if (resId != 0) {
                            avatarImageView.setImageResource(resId);
                        }
                    }

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

    // 🔹 Logout funkcija
    private void logoutUser() {
        new UserService().logout();
        Toast.makeText(this, "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
