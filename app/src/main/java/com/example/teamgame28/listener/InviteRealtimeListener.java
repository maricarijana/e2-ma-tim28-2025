package com.example.teamgame28.listener;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.teamgame28.util.NotificationHelper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class InviteRealtimeListener {

    private static final String TAG = "InviteRealtimeListener";
    private static ListenerRegistration reg;

    public static void start(Context appContext, String currentUserId) {
        stop();
        reg = FirebaseFirestore.getInstance()
                .collection("app_users").document(currentUserId)
                .collection("invites")
                .whereEqualTo("status", "pending")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override public void onEvent(@Nullable QuerySnapshot snaps, @Nullable FirebaseFirestoreException e) {
                        if (e != null || snaps == null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        for (DocumentChange dc : snaps.getDocumentChanges()) {
                            if (dc.getType() != DocumentChange.Type.ADDED) continue;

                            String allianceId = dc.getDocument().getString("allianceId");
                            String fromUserId = dc.getDocument().getString("fromUserId");

                            // (Opcionalno) možeš dovući i imena, ali i bez toga radi – prikažemo generic poruku
                            String allianceName = dc.getDocument().getString("allianceName"); // ako dodaš u invite mapu
                            String leaderName   = dc.getDocument().getString("leaderName");   // ako dodaš u invite mapu

                            int nid = (currentUserId + "_" + dc.getDocument().getId()).hashCode();

                            NotificationHelper.sendAllianceInviteNotification(
                                    appContext,
                                    allianceName != null ? allianceName : "Savez",
                                    leaderName   != null ? leaderName   : "Vođa",
                                    nid
                            );

                            // markiraj kao 'notified' da se ne duplira
                            dc.getDocument().getReference().update("notified", true);
                        }
                    }
                });
    }

    public static void stop() {
        if (reg != null) { reg.remove(); reg = null; }
    }
}
