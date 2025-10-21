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
import com.example.teamgame28.service.AllianceService;
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
    private android.widget.Button btnOpenChat;
    private android.widget.Button btnDisbandAlliance;

    private AllianceMembersAdapter membersAdapter;
    private UserRepository userRepository;
    private AllianceService allianceService;
    private FirebaseFirestore db;
    private String currentUserId;
    private String allianceId;
    private Alliance currentAlliance;

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
        btnOpenChat = view.findViewById(R.id.btn_open_chat);
        btnDisbandAlliance = view.findViewById(R.id.btn_disband_alliance);

        // Firebase i Repository
        userRepository = new UserRepository();
        allianceService = new AllianceService();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Postavi RecyclerView
        membersAdapter = new AllianceMembersAdapter();
        recyclerMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMembers.setAdapter(membersAdapter);

        // Postavi listener za dugme Chat
        btnOpenChat.setOnClickListener(v -> openChat());

        // Postavi listener za dugme Ukini savez
        btnDisbandAlliance.setOnClickListener(v -> disbandAlliance());

        // Učitaj podatke o savezu
        loadAllianceDetails();

        return view;
    }

    private void openChat() {
        // Otvori AllianceChatFragment
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AllianceChatFragment())
                .addToBackStack(null)
                .commit();
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
                            currentAlliance = alliance;
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

        // Prikaži dugme za ukidanje saveza samo ako je trenutni korisnik vođa
        if (currentUserId != null && currentUserId.equals(alliance.getLeaderId())) {
            btnDisbandAlliance.setVisibility(View.VISIBLE);
        } else {
            btnDisbandAlliance.setVisibility(View.GONE);
        }
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

    private void disbandAlliance() {
        if (currentAlliance == null || allianceId == null) {
            Toast.makeText(getContext(), "Greška: savez nije učitan", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prvo proveri da li je misija pokrenuta
        allianceService.hasActiveMission(allianceId, new AllianceService.ActiveMissionCallback() {
            @Override
            public void onResult(boolean hasActiveMission) {
                if (hasActiveMission) {
                    // Misija je pokrenuta, ne dozvoli ukidanje
                    Toast.makeText(getContext(), "Misija pokrenuta! Ne možete ukinuti savez.", Toast.LENGTH_LONG).show();
                } else {
                    // Nema aktivne misije, možemo da ukinemo savez
                    confirmAndDisbandAlliance();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmAndDisbandAlliance() {
        // Potvrdi sa korisnikom
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Ukini savez")
                .setMessage("Da li ste sigurni da želite da ukinete savez? Svi članovi će biti uklonjeni iz saveza.")
                .setPositiveButton("Da", (dialog, which) -> {
                    // Pozovi service da ukine savez
                    allianceService.disbandAlliance(allianceId, new AllianceService.ServiceCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            // Vrati se na glavni ekran ili osvezi
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Ne", null)
                .show();
    }
}
