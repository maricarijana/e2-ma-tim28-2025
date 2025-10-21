package com.example.teamgame28.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.teamgame28.service.SpecialTaskMissionService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Periodični radnik koji proverava da li je neka specijalna misija istekla
 * (prošlo je više od 2 nedelje od startTime) i poziva centralnu logiku
 * za završetak misije u SpecialTaskMissionService.
 *
 * Ako je boss poražen → dodeljuju se nagrade.
 * Ako je misija istekla → proverava se "Bez nerešenih zadataka" (-10 HP).
 */
public class MissionExpiryWorker extends Worker {

    private final FirebaseFirestore db;

    public MissionExpiryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        long now = System.currentTimeMillis();
        SpecialTaskMissionService missionService = new SpecialTaskMissionService();

        Log.d("MissionExpiryWorker", "🚀 Pokrećem proveru isteka misija...");

        db.collection("alliance_missions")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.d("MissionExpiryWorker", "✅ Nema aktivnih misija za proveru.");
                        return;
                    }

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        // 🔹 Sigurno čitanje endTime polja (može biti Timestamp, Long ili String)
                        Object endTimeObj = doc.get("endTime");
                        long endTime = 0L;

                        if (endTimeObj instanceof Timestamp) {
                            endTime = ((Timestamp) endTimeObj).toDate().getTime();
                        } else if (endTimeObj instanceof Long) {
                            endTime = (Long) endTimeObj;
                        } else if (endTimeObj instanceof String) {
                            try {
                                endTime = Long.parseLong((String) endTimeObj);
                            } catch (NumberFormatException e) {
                                Log.e("MissionExpiryWorker", "⚠️ endTime nije validan broj: " + endTimeObj);
                            }
                        }

                        // 🔸 Provera da li je misija istekla
                        if (endTime > 0 && endTime < now) {
                            String missionId = doc.getId();
                            Log.d("MissionExpiryWorker", "🕒 Misija " + missionId + " je istekla — pokrećem proveru završetka...");

                            // ✅ Centralna logika — poziva sve (proveru zadataka, nagrade, deaktivaciju)
                            missionService.triggerMissionCheck(missionId);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("MissionExpiryWorker", "❌ Greška pri proveri isteka misija", e));

        // Worker se izvršava asinhrono → vraća success da ne blokira pozadinski rad
        return Result.success();
    }
}
