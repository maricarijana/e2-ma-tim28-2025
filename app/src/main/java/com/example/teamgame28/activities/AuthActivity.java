package com.example.teamgame28.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.teamgame28.R;
import com.example.teamgame28.fragments.LoginFragment;
import com.example.teamgame28.workers.CleanupExpiredUsersWorker;

import java.util.concurrent.TimeUnit;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Scheduluj Worker da se izvršava nakon 2 minuta (za testiranje)
        // Za produkciju: koristiti PeriodicWorkRequest sa 24h
        scheduleCleanupWorker();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.auth_fragment_container, new LoginFragment())
                    .commit();
        }
    }

    private void scheduleCleanupWorker() {
        // ZA TESTIRANJE: OneTimeWorkRequest koji se izvršava nakon 2 minuta
        OneTimeWorkRequest cleanupWorkRequest = new OneTimeWorkRequest.Builder(CleanupExpiredUsersWorker.class)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .addTag("cleanup_unverified_users")
                .build();

        WorkManager.getInstance(this).enqueue(cleanupWorkRequest);

        android.util.Log.d("AuthActivity", "====================================");
        android.util.Log.d("AuthActivity", "⏰ CleanupWorker SCHEDULOVAN!");
        android.util.Log.d("AuthActivity", "🕒 Vreme schedulovanja: " + new java.util.Date());
        android.util.Log.d("AuthActivity", "⏱️ Izvršiće se za: 2 minuta");
        android.util.Log.d("AuthActivity", "🆔 Work ID: " + cleanupWorkRequest.getId());
        android.util.Log.d("AuthActivity", "====================================");
    }
}
