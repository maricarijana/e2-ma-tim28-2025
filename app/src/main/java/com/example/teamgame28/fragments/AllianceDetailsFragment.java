package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.AllianceMembersAdapter;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AllianceDetailsFragment extends Fragment {

    private TextView tvAllianceName;
    private TextView tvLeaderName;
    private TextView tvMemberCount;
    private TextView tvCurrentMission;
    private RecyclerView recyclerMembers;
    private TextView tvEmptyMembers;

    private AllianceMembersAdapter membersAdapter;
    private UserRepository userRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    private String allianceId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alliance_details, container, false);

        // Inicijalizacija view-a
        tvAllianceName = view.findViewById(R.id.tv_alliance_name);
        tvLeaderName = view.findViewById(R.id.tv_leader_name);
        tvMemberCount = view.findViewById(R.id.tv_member_count);
        tvCurrentMission = view.findViewById(R.id.tv_current_mission);
        recyclerMembers = view.findViewById(R.id.recycler_members);
        tvEmptyMembers = view.findViewById(R.id.tv_empty_members);

        // Firebase i Repository
        userRepository = new UserRepository();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Postavi RecyclerView
        membersAdapter = new AllianceMembersAdapter();
        recyclerMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMembers.setAdapter(membersAdapter);

        // Učitaj podatke o savezu
        loadAllianceDetails();

        return view;
    }

    private void loadAllianceDetails() {
        // Dohvati allianceId iz UserProfile
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(com.example.teamgame28.model.UserProfile userProfile) {
                if (userProfile != null && userProfile.getCurrentAllianceId() != null) {
                    allianceId = userProfile.getCurrentAllianceId();
                    loadAllianceData(allianceId);
                } else {
                    Toast.makeText(getContext(), "Nisi u savezu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllianceData(String allianceId) {
        db.collection("alliances")
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Alliance alliance = documentSnapshot.toObject(Alliance.class);
                        if (alliance != null) {
                            displayAllianceInfo(alliance);
                            loadMembers(alliance);
                            loadActiveMission(allianceId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Greška pri učitavanju saveza: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayAllianceInfo(Alliance alliance) {
        tvAllianceName.setText(alliance.getName());

        // Učitaj ime vođe
        userRepository.getUserById(alliance.getLeaderId(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User leader) {
                if (leader != null) {
                    tvLeaderName.setText("Vođa: " + leader.getUsername());
                }
            }

            @Override
            public void onFailure(Exception e) {
                tvLeaderName.setText("Vođa: Nepoznat");
            }
        });

        // Broj članova (leaderId + members lista)
        int totalMembers = 1 + (alliance.getMembers() != null ? alliance.getMembers().size() : 0);
        tvMemberCount.setText("Članovi: " + totalMembers);
    }

    private void loadMembers(Alliance alliance) {
        List<String> memberIds = new ArrayList<>();
        memberIds.add(alliance.getLeaderId()); // Dodaj vođu

        if (alliance.getMembers() != null) {
            memberIds.addAll(alliance.getMembers()); // Dodaj ostale članove
        }

        // Dohvati User objekte
        userRepository.getUsersByIds(memberIds, new UserRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    membersAdapter.setMembers(users, alliance.getLeaderId());
                    recyclerMembers.setVisibility(View.VISIBLE);
                    tvEmptyMembers.setVisibility(View.GONE);
                } else {
                    recyclerMembers.setVisibility(View.GONE);
                    tvEmptyMembers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju članova: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadActiveMission(String allianceId) {
        db.collection("alliance_missions")
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        com.example.teamgame28.model.AllianceMission mission =
                                querySnapshot.getDocuments().get(0).toObject(com.example.teamgame28.model.AllianceMission.class);
                        if (mission != null) {
                            tvCurrentMission.setText("Boss HP: " + mission.getBossHp());
                        }
                    } else {
                        tvCurrentMission.setText("Nema aktivne misije");
                    }
                })
                .addOnFailureListener(e -> {
                    tvCurrentMission.setText("Nema aktivne misije");
                });
    }
}
