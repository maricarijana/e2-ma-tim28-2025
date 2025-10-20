package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.SelectableFriendAdapter;
import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.AllianceService;
import com.example.teamgame28.util.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAllianceFragment extends Fragment {

    private EditText inputAllianceName;
    private RecyclerView recyclerView;
    private MaterialButton btnCreateAlliance;

    private SelectableFriendAdapter adapter;
    private UserRepository userRepository;
    private AllianceService allianceService;
    private FirebaseAuth auth;

    private String currentUserId;
    private String currentUsername;
    private List<String> friendIds;
    private Map<String, User> usersById; // Za username lookup

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_alliance, container, false);

        // View komponente
        inputAllianceName = view.findViewById(R.id.input_alliance_name);
        recyclerView = view.findViewById(R.id.recycler_view_friends);
        btnCreateAlliance = view.findViewById(R.id.btn_create_alliance);

        // Firebase i servisi
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        allianceService = new AllianceService();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadCurrentUsername(); // Učitaj username za notifikacije
            loadFriends();
        }

        // RecyclerView setup
        adapter = new SelectableFriendAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Dugme za kreiranje
        btnCreateAlliance.setOnClickListener(v -> createAlliance());

        // Kreiraj notification channel
        NotificationHelper.createNotificationChannel(requireContext());

        return view;
    }

    private void loadCurrentUsername() {
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUsername = user.getUsername();
                }
            }

            @Override
            public void onFailure(Exception e) {
                currentUsername = "Nepoznat korisnik";
            }
        });
    }

    private void loadFriends() {
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (userProfile != null) {
                    friendIds = userProfile.getFriends();

                    if (friendIds == null || friendIds.isEmpty()) {
                        Toast.makeText(getContext(), "Nemate prijatelja za pozivanje", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Dohvati User objekte za prijatelje
                    userRepository.getUsersByIds(friendIds, new UserRepository.UserListCallback() {
                        @Override
                        public void onSuccess(List<User> users) {
                            // Filtriraj samog sebe iz liste (safety check)
                            users.removeIf(user -> user.getUid().equals(currentUserId));

                            // Kreiraj mapu za kasnije lookup
                            usersById = new HashMap<>();
                            for (User user : users) {
                                usersById.put(user.getUid(), user);
                            }

                            adapter.setFriends(users);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Greška pri učitavanju prijatelja", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAlliance() {
        String allianceName = inputAllianceName.getText().toString().trim();

        if (allianceName.isEmpty()) {
            Toast.makeText(getContext(), "Unesi naziv saveza!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedFriends = adapter.getSelectedUserIds();

        // VAŽNO: Filtriraj samog sebe iz liste (ne mogu pozvati sebe u savez!)
        selectedFriends.removeIf(userId -> userId.equals(currentUserId));

        // Kreiraj savez
        allianceService.createAlliance(currentUserId, allianceName, new AllianceService.ServiceCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                // Savez je kreiran, sada pozovi prijatelje
                // Note: AllianceRepository.createAlliance već setuje currentAllianceId za leadera
                // Ali treba nam allianceId da bi pozvali prijatelje

                // Workaround: Re-učitaj UserProfile da dohvatimo allianceId
                userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        String allianceId = userProfile.getCurrentAllianceId();

                        if (allianceId != null && !selectedFriends.isEmpty()) {
                            inviteFriends(allianceId, allianceName, selectedFriends);
                        } else {
                            // Savez kreiran, ali nema poziva
                            requireActivity().onBackPressed();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Greška pri dohvatanju allianceId", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inviteFriends(String allianceId, String allianceName, List<String> friendIds) {
        android.util.Log.d("ALLIANCE_DEBUG", "inviteFriends pozvano za " + friendIds.size() + " prijatelja");
        android.util.Log.d("ALLIANCE_DEBUG", "allianceId = " + allianceId);

        final int totalInvites = friendIds.size();
        final int[] completedInvites = {0};

        for (String friendId : friendIds) {
            android.util.Log.d("ALLIANCE_DEBUG", "Pozivam prijatelja: " + friendId);

            allianceService.inviteToAlliance(allianceId, currentUserId, friendId, new AllianceService.ServiceCallback() {
                @Override
                public void onSuccess(String message) {
                    android.util.Log.d("ALLIANCE_DEBUG", "✅ Poziv poslat za: " + friendId);

                    // Pošalji notifikaciju prijatelju (proveri da li je fragment još attached)
                 //  if (isAdded() && getContext() != null) {
                  //     sendNotificationToFriend(allianceName, friendId);
                    //}

                    // Proveri da li su svi pozivi završeni
                    completedInvites[0]++;
                    if (completedInvites[0] == totalInvites) {
                        // Svi pozivi završeni, zatvori fragment
                        if (isAdded() && getActivity() != null) {
                            Toast.makeText(getContext(), "✅ Savez kreiran i pozivi poslati!", Toast.LENGTH_LONG).show();
                            requireActivity().onBackPressed();
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    android.util.Log.e("ALLIANCE_DEBUG", "❌ Greška kod poziva za " + friendId + ": " + error);

                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Greška kod poziva: " + error, Toast.LENGTH_SHORT).show();
                    }

                    // Proveri da li su svi pozivi završeni (uključujući i greške)
                    completedInvites[0]++;
                    if (completedInvites[0] == totalInvites) {
                        if (isAdded() && getActivity() != null) {
                            Toast.makeText(getContext(), "Savez kreiran sa greškama", Toast.LENGTH_LONG).show();
                            requireActivity().onBackPressed();
                        }
                    }
                }
            });
        }
    }

    private void sendNotificationToFriend(String allianceName, String friendId) {
        // Generiši notification ID od allianceId + friendId
        int notificationId = (allianceName + friendId).hashCode();

        NotificationHelper.sendAllianceInviteNotification(
                requireContext().getApplicationContext(),
                allianceName,
                currentUsername != null ? currentUsername : "Nepoznat vođa",
                notificationId
        );
    }
}
