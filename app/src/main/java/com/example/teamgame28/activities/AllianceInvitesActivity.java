package com.example.teamgame28.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.example.teamgame28.R;
import com.example.teamgame28.activities.MainActivity;
import com.example.teamgame28.adapters.AllianceInviteAdapter;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.AllianceRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.AllianceService;
import com.example.teamgame28.util.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceInvitesActivity extends AppCompatActivity implements AllianceInviteAdapter.OnInviteActionListener {

    private RecyclerView recyclerView;
    private TextView emptyMessage;

    private AllianceInviteAdapter adapter;
    private AllianceRepository allianceRepository;
    private AllianceService allianceService;
    private UserRepository userRepository;
    private FirebaseAuth auth;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_invites);

        // View komponente
        recyclerView = findViewById(R.id.recycler_view_invites);
        emptyMessage = findViewById(R.id.empty_message);

        // Firebase i servisi
        auth = FirebaseAuth.getInstance();
        allianceRepository = new AllianceRepository();
        allianceService = new AllianceService();
        userRepository = new UserRepository();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadPendingInvites();
        }

        // RecyclerView setup
        adapter = new AllianceInviteAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            markAcceptanceNotificationsAsSeen();
        }
    }

    /**
     * Markiraj acceptance notifikacije kao viđene kada se otvori ekran.
     */
    private void markAcceptanceNotificationsAsSeen() {
        FirebaseFirestore.getInstance()
                .collection("app_users")
                .document(currentUserId)
                .collection("invites")
                .whereEqualTo("type", "acceptance")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Markiraj kao viđeno
                        doc.getReference().update("seen", true);

                        // Otkaži notifikaciju
                        int notificationId = (currentUserId + "_" + doc.getId()).hashCode();
                        NotificationHelper.cancelNotification(this, notificationId);
                    }
                    android.util.Log.d("ALLIANCE_INVITES", "✅ Acceptance notifications marked as seen");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ALLIANCE_INVITES", "❌ Failed to mark notifications as seen", e);
                });
    }

    private void loadPendingInvites() {
        allianceRepository.getPendingInvitesForUser(currentUserId, new AllianceRepository.AllianceListCallback() {
            @Override
            public void onSuccess(List<Alliance> alliances) {
                if (alliances == null || alliances.isEmpty()) {
                    // Nema poziva
                    emptyMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Dohvati User podatke za sve vođe
                    loadLeadersForAlliances(alliances);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AllianceInvitesActivity.this, "Greška pri učitavanju poziva: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLeadersForAlliances(List<Alliance> alliances) {
        // Izvuci unique leaderIds
        List<String> leaderIds = new ArrayList<>();
        for (Alliance alliance : alliances) {
            if (!leaderIds.contains(alliance.getLeaderId())) {
                leaderIds.add(alliance.getLeaderId());
            }
        }

        // Dohvati User objekte
        userRepository.getUsersByIds(leaderIds, new UserRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                // Kreiraj mapu leaderId -> User
                Map<String, User> leadersById = new HashMap<>();
                for (User user : users) {
                    leadersById.put(user.getUid(), user);
                }

                // Postavi podatke u adapter
                adapter.setData(alliances, leadersById);
                emptyMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AllianceInvitesActivity.this, "Greška pri učitavanju vođa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAcceptClick(Alliance alliance) {
        // KORAK 1: Dohvati UserProfile da vidiš da li je korisnik već u nekom savezu
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                String currentAllianceId = userProfile != null ? userProfile.getCurrentAllianceId() : null;

                if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
                    // Korisnik je već u savezu - proveri da li ima aktivnu misiju
                    checkActiveMissionAndProceed(alliance, currentAllianceId);
                } else {
                    // Korisnik nema savez - direktno prihvati poziv
                    acceptInviteDirectly(alliance, null);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AllianceInvitesActivity.this, "Greška pri proveri profila: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Proveri da li trenutni savez ima aktivnu misiju.
     */
    private void checkActiveMissionAndProceed(Alliance newAlliance, String currentAllianceId) {
        allianceService.hasActiveMission(currentAllianceId, new AllianceService.ActiveMissionCallback() {
            @Override
            public void onResult(boolean hasActiveMission) {
                if (hasActiveMission) {
                    // Savez ima aktivnu misiju - ne može napustiti
                    Toast.makeText(AllianceInvitesActivity.this,
                            "❌ Ne možeš napustiti trenutni savez jer je misija u toku!",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Nema aktivnu misiju - prikaži dijalog za potvrdu
                    showConfirmationDialog(newAlliance, currentAllianceId);
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AllianceInvitesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Prikaži dijalog za potvrdu napuštanja trenutnog saveza.
     */
    private void showConfirmationDialog(Alliance newAlliance, String currentAllianceId) {
        new AlertDialog.Builder(this)
                .setTitle("Napusti savez?")
                .setMessage("Već si u savezu. Da li želiš da napustiš trenutni savez i pridružiš se savezu \"" + newAlliance.getName() + "\"?")
                .setPositiveButton("Prihvati", (dialog, which) -> {
                    // Korisnik potvrdio - napusti stari savez i pridruži se novom
                    acceptInviteDirectly(newAlliance, currentAllianceId);
                })
                .setNegativeButton("Ostani", (dialog, which) -> {
                    // Korisnik odbio - ne radi ništa
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Prihvati poziv (sa ili bez napuštanja starog saveza).
     */
    private void acceptInviteDirectly(Alliance alliance, String oldAllianceId) {
        allianceService.acceptInvite(alliance.getId(), currentUserId, oldAllianceId, new AllianceService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AllianceInvitesActivity.this, "✅ " + message, Toast.LENGTH_SHORT).show();

                // Updateuj status originalnog invite dokumenta na "accepted"
                updateInviteStatus(alliance.getId(), "accepted");

                // Ukloni notifikaciju
                int notificationId = (alliance.getName() + currentUserId).hashCode();
                NotificationHelper.cancelNotification(AllianceInvitesActivity.this, notificationId);

                // Pošalji notifikaciju vođi da je poziv prihvaćen
                sendAcceptanceNotificationToLeader(alliance);

                // Otvori MainActivity i prikaži AllianceDetailsFragment
                Intent intent = new Intent(AllianceInvitesActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("open_alliance_details", true);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AllianceInvitesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Pošalji notifikaciju vođi da je korisnik prihvatio poziv.
     */
    private void sendAcceptanceNotificationToLeader(Alliance alliance) {
        // Dohvati ime trenutnog korisnika
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User currentUser) {
                if (currentUser != null) {
                    // Upiši dokument u invites kolekciju vođe (za notifikaciju)
                    String leaderUserId = alliance.getLeaderId();
                    String notificationId = FirebaseFirestore.getInstance().collection("tmp").document().getId();

                    java.util.Map<String, Object> notification = new java.util.HashMap<>();
                    notification.put("inviteId", notificationId);
                    notification.put("allianceId", alliance.getId());
                    notification.put("allianceName", alliance.getName());
                    notification.put("fromUserId", currentUserId);
                    notification.put("fromUserName", currentUser.getUsername());
                    notification.put("type", "acceptance"); // Označava da je ovo prihvaćanje poziva
                    notification.put("status", "pending"); // Pending da bi listener uhvatio
                    notification.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    notification.put("notified", false);

                    FirebaseFirestore.getInstance()
                            .collection("app_users")
                            .document(leaderUserId)
                            .collection("invites")
                            .document(notificationId)
                            .set(notification)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("ALLIANCE_ACCEPT", "✅ Notifikacija poslata vođi " + leaderUserId);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("ALLIANCE_ACCEPT", "❌ Greška pri slanju notifikacije vođi", e);
                            });
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("ALLIANCE_ACCEPT", "❌ Greška pri dohvatanju trenutnog korisnika", e);
            }
        });
    }

    @Override
    public void onDeclineClick(Alliance alliance) {
        allianceService.declineInvite(alliance.getId(), currentUserId, new AllianceService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AllianceInvitesActivity.this, message, Toast.LENGTH_SHORT).show();

                // Updateuj status originalnog invite dokumenta na "declined"
                updateInviteStatus(alliance.getId(), "declined");

                // Ukloni notifikaciju
                int notificationId = (alliance.getName() + currentUserId).hashCode();
                NotificationHelper.cancelNotification(AllianceInvitesActivity.this, notificationId);

                // Osvježi listu
                loadPendingInvites();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AllianceInvitesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updateuj status invite dokumenta u app_users/{currentUserId}/invites.
     */
    private void updateInviteStatus(String allianceId, String newStatus) {
        FirebaseFirestore.getInstance()
                .collection("app_users")
                .document(currentUserId)
                .collection("invites")
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("status", "pending")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().get(0).getReference()
                                .update("status", newStatus)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("INVITE_UPDATE", "✅ Invite status updated to: " + newStatus);
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("INVITE_UPDATE", "❌ Failed to update invite status", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("INVITE_UPDATE", "❌ Failed to find invite document", e);
                });
    }
}
