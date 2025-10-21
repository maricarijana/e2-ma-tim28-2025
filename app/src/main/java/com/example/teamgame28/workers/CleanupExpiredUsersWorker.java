package com.example.teamgame28.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.teamgame28.repository.UserRepository;

public class CleanupExpiredUsersWorker extends Worker {

    private static final String TAG = "CleanupWorker";

    public CleanupExpiredUsersWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "====================================");
        Log.d(TAG, "🔄 CleanupExpiredUsersWorker STARTED!");
        Log.d(TAG, "🕒 Vreme izvršavanja: " + new java.util.Date());
        Log.d(TAG, "====================================");

        try {
            UserRepository userRepository = new UserRepository();
            userRepository.deleteUnverifiedOldAccounts();

            Log.d(TAG, "✅ CleanupExpiredUsersWorker finished successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "❌ CleanupExpiredUsersWorker FAILED!", e);
            return Result.failure();
        }
    }
}
