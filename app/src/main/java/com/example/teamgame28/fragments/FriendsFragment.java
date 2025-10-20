package com.example.teamgame28.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.activities.QrScannerActivity;
import com.example.teamgame28.adapters.UserSearchAdapter;
import com.example.teamgame28.fragments.CreateAllianceFragment;
import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.AllianceRepository;
import com.example.teamgame28.repository.FriendRepository;
import com.example.teamgame28.repository.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class FriendsFragment extends Fragment implements UserSearchAdapter.OnUserClickListener {

    private EditText searchInput;
    private Button btnSearch;
    private MaterialButton btnScanQr;
    private MaterialButton btnCreateAlliance;
    private RecyclerView recyclerView;
    private TextView emptyMessage;

    private UserSearchAdapter adapter;
    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private AllianceRepository allianceRepository;
    private FirebaseAuth auth;

    private String currentUserId;
    private String currentAllianceId; // ID saveza trenutnog korisnika (može biti null)
    private List<String> friendIds; // Lista userId-jeva prijatelja

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // Inicijalizacija view komponenti
        searchInput = view.findViewById(R.id.search_input);
        btnSearch = view.findViewById(R.id.btn_search);
        btnScanQr = view.findViewById(R.id.btn_scan_qr);
        btnCreateAlliance = view.findViewById(R.id.btn_create_alliance);
        recyclerView = view.findViewById(R.id.recycler_view_users);
        emptyMessage = view.findViewById(R.id.empty_message);

        // Firebase i Repository
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        friendRepository = new FriendRepository();
        allianceRepository = new AllianceRepository();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadFriendsList(); // Učitaj prijatelje pri otvaranju fragmenta
        }

        // Postavi RecyclerView
        adapter = new UserSearchAdapter(this, UserSearchAdapter.Mode.FRIEND_LIST);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Dugme za pretragu
        btnSearch.setOnClickListener(v -> performSearch());

        // Enter key u search input-u takođe pokreće pretragu
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        // Dugme za skeniranje QR koda
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QrScannerActivity.class);
            startActivity(intent);
        });

        // Dugme za kreiranje saveza
        btnCreateAlliance.setOnClickListener(v -> {
            CreateAllianceFragment createAllianceFragment = new CreateAllianceFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, createAllianceFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Osvježi listu prijatelja kada se fragment ponovo prikazuje (npr. nakon QR skeniranja)
        if (currentUserId != null) {
            loadFriendsList();
        }
    }

    /**
     * Učitaj listu prijatelja trenutnog korisnika.
     */
    private void loadFriendsList() {
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (userProfile != null) {
                    friendIds = userProfile.getFriends();
                    currentAllianceId = userProfile.getCurrentAllianceId();

                    if (friendIds == null || friendIds.isEmpty()) {
                        // Nema prijatelja
                        emptyMessage.setText("Nemate prijatelja.\nPretražite korisnike ili skenirajte QR kod.");
                        emptyMessage.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        // Učitaj User objekte za sve prijatelje
                        userRepository.getUsersByIds(friendIds, new UserRepository.UserListCallback() {
                            @Override
                            public void onSuccess(List<User> users) {
                                // Filtriraj samog sebe iz liste (safety check)
                                users.removeIf(user -> user.getUid().equals(currentUserId));

                                if (users.isEmpty()) {
                                    emptyMessage.setText("Nemate prijatelja.\nPretražite korisnike ili skenirajte QR kod.");
                                    emptyMessage.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    adapter.setMode(UserSearchAdapter.Mode.FRIEND_LIST);
                                    adapter.setFriendIds(friendIds);
                                    adapter.setUserList(users);
                                    emptyMessage.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Greška pri učitavanju prijatelja: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju profila: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Pretraži korisnike po username-u.
     */
    private void performSearch() {
        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Unesite korisničko ime", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pretraži korisnike
        userRepository.searchUsersByUsername(query, new UserRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                // Filtriraj samog sebe iz rezultata
                users.removeIf(user -> user.getUid().equals(currentUserId));

                if (users.isEmpty()) {
                    // Nema rezultata
                    emptyMessage.setText("Nema rezultata");
                    emptyMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Prikaži rezultate u search režimu
                    adapter.setMode(UserSearchAdapter.Mode.SEARCH_RESULTS);
                    adapter.setFriendIds(friendIds); // Postavi listu prijatelja za proveru
                    adapter.setUserList(users);
                    emptyMessage.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri pretrazi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewProfileClick(User user) {
        // Otvori profil korisnika
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", user.getUid());
        profileFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAddFriendClick(User user) {
        // Pošalji friend request
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Niste prijavljeni", Toast.LENGTH_SHORT).show();
            return;
        }

        friendRepository.sendFriendRequest(currentUserId, user.getUid(), new FriendRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "✅ Zahtev poslat korisniku " + user.getUsername(), Toast.LENGTH_SHORT).show();

                // Reset na listu prijatelja
                searchInput.setText("");
                loadFriendsList();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "❌ Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onInviteToAllianceClick(User user) {
        // Pozovi prijatelja u savez
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Niste prijavljeni", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentAllianceId == null || currentAllianceId.isEmpty()) {
            Toast.makeText(getContext(), "Niste u savezu. Prvo kreirajte savez.", Toast.LENGTH_SHORT).show();
            return;
        }

        allianceRepository.inviteToAlliance(currentAllianceId, currentUserId, user.getUid(), new AllianceRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "✅ Poziv poslat korisniku " + user.getUsername(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "❌ Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
