package com.example.teamgame28.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.teamgame28.R;
import com.example.teamgame28.activities.AllianceInvitesActivity;

public class NotificationHelper {

    private static final String CHANNEL_ID = "alliance_invites_channel";
    private static final String CHANNEL_NAME = "Pozivi u savez";
    private static final String CHANNEL_DESC = "Notifikacije za pozive u savez";

    /** Kreira notification channel (Android 8.0+). Bezbedno je pozvati uvek. */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Pošalji SISTEMSKU (status bar) notifikaciju – NEDISMISSABLE.
     * Potpis ostaje IDENTIČAN tvom.
     */
    public static void sendAllianceInviteNotification(Context context,
                                                      String allianceName,
                                                      String leaderName,
                                                      int notificationId) {
        // Koristi app context radi sigurnosti iz fragmenta/servisa
        Context appCtx = context.getApplicationContext();

        // Kanal uvek pre notify (no-op na <26)
        createNotificationChannel(appCtx);

        // Intent ka ekranu sa pozivima
        Intent intent = new Intent(appCtx, AllianceInvitesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                appCtx,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notifikacija (heads-up na <26 preko PRIORITY/DEFAULTS)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appCtx, CHANNEL_ID)
                // Sistemaska ikonica (da ne dodaješ drawable):
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Poziv u savez")
                .setContentText("Pozvani ste u savez \"" + allianceName + "\" od " + leaderName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)   // za <26
                .setDefaults(NotificationCompat.DEFAULT_ALL)     // ton/vibracija
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)                            // ne skida se tapom
                .setOngoing(true);                               // nedismissable

        // SISTEMSKI NotificationManager (kao u tvom kodu)
        NotificationManager notificationManager =
                (NotificationManager) appCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    /**
     * Pošalji notifikaciju vođi da je neko prihvatio poziv.
     */
    public static void sendAllianceAcceptanceNotification(Context context,
                                                          String memberName,
                                                          String allianceName,
                                                          int notificationId) {
        Context appCtx = context.getApplicationContext();

        // Kanal uvek pre notify
        createNotificationChannel(appCtx);

        // Intent ka ekranu sa detaljima saveza (možeš kasnije dodati)
        Intent intent = new Intent(appCtx, AllianceInvitesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                appCtx,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notifikacija
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appCtx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Novi član u savezu")
                .setContentText(memberName + " se pridružio savezu \"" + allianceName + "\"")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Može se dismissovati tapom

        NotificationManager notificationManager =
                (NotificationManager) appCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    /** Ruši notifikaciju iz KODA (jer je nedismissable). */
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }
}
