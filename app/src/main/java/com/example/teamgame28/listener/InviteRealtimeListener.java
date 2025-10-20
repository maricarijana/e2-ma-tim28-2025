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

                            String type = dc.getDocument().getString("type");
                            String allianceId = dc.getDocument().getString("allianceId");
                            String fromUserId = dc.getDocument().getString("fromUserId");
                            String allianceName = dc.getDocument().getString("allianceName");

                            int nid = (currentUserId + "_" + dc.getDocument().getId()).hashCode();

                            if ("acceptance".equals(type)) {
                                // Ovo je notifikacija da je neko prihvatio poziv
                                String fromUserName = dc.getDocument().getString("fromUserName");

                                NotificationHelper.sendAllianceAcceptanceNotification(
                                        appContext,
                                        fromUserName != null ? fromUserName : "Korisnik",
                                        allianceName != null ? allianceName : "Savez",
                                        nid
                                );
                            } else {
                                // Ovo je poziv u savez (default)
                                String leaderName = dc.getDocument().getString("leaderName");

                                NotificationHelper.sendAllianceInviteNotification(
                                        appContext,
                                        allianceName != null ? allianceName : "Savez",
                                        leaderName != null ? leaderName : "Vođa",
                                        nid
                                );
                            }

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
