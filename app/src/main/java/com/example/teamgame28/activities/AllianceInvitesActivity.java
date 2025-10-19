package com.example.teamgame28.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.AllianceInviteAdapter;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.AllianceRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.AllianceService;
import com.example.teamgame28.util.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        allianceService.acceptInvite(alliance.getId(), currentUserId, new AllianceService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AllianceInvitesActivity.this, message, Toast.LENGTH_SHORT).show();

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

    @Override
    public void onDeclineClick(Alliance alliance) {
        allianceService.declineInvite(alliance.getId(), currentUserId, new AllianceService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AllianceInvitesActivity.this, message, Toast.LENGTH_SHORT).show();

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
}
