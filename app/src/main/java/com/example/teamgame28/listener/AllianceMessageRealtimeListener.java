package com.example.teamgame28.listener;

import android.content.Context;

import com.example.teamgame28.util.NotificationHelper;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceMessageRealtimeListener {

    private static final Map<String, ListenerRegistration> regs = new HashMap<>();

    public static void startForAlliance(Context appCtx,
                                        String currentUserId,
                                        String allianceId,
                                        String allianceName) {
        stopForAlliance(allianceId);

        ListenerRegistration reg = FirebaseFirestore.getInstance()
                .collection("alliances").document(allianceId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snaps, e) -> {
                    if (e != null || snaps == null) return;

                    for (DocumentChange dc : snaps.getDocumentChanges()) {
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        String senderId       = dc.getDocument().getString("senderId");
                        String senderUsername = dc.getDocument().getString("senderUsername");
                        String text           = dc.getDocument().getString("text");
                        String msgId          = dc.getDocument().getId();

                        // Ne obaveštavaj pošiljaoca
                        if (senderId != null && senderId.equals(currentUserId)) continue;

                        int nid = (allianceId + "_" + msgId).hashCode();
                        String title = "Nova poruka u " + (allianceName != null ? allianceName : "savezu");
                        String body  = (senderUsername != null ? senderUsername : "Član") + ": " + (text != null ? text : "");

                        // notifikacija za chat: dismisable
                        NotificationHelper.sendAllianceChatNotification(appCtx, title, body, nid, allianceId);
                    }
                });

        regs.put(allianceId, reg);
    }

    public static void startForAll(Context appCtx,
                                   String currentUserId,
                                   List<String> allianceIds,
                                   Map<String, String> allianceNames) {
        stopAll();
        if (allianceIds == null) return;
        for (String aid : allianceIds) {
            String name = allianceNames != null ? allianceNames.get(aid) : null;
            startForAlliance(appCtx, currentUserId, aid, name);
        }
    }

    public static void stopForAlliance(String allianceId) {
        ListenerRegistration reg = regs.remove(allianceId);
        if (reg != null) reg.remove();
    }

    public static void stopAll() {
        for (ListenerRegistration r : regs.values()) if (r != null) r.remove();
        regs.clear();
    }
}
