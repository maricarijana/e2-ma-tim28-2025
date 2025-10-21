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
import com.example.teamgame28.adapters.FriendRequestAdapter;
import com.example.teamgame28.model.FriendRequest;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.FriendRepository;
import com.example.teamgame28.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestsFragment extends Fragment implements FriendRequestAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private TextView emptyMessage;

    private FriendRequestAdapter adapter;
    private FriendRepository friendRepository;
    private UserRepository userRepository;
    private FirebaseAuth auth;

    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);

        // Inicijalizacija view komponenti
        recyclerView = view.findViewById(R.id.recycler_view_requests);
        emptyMessage = view.findViewById(R.id.empty_message);

        // Firebase i Repository
        auth = FirebaseAuth.getInstance();
        friendRepository = new FriendRepository();
        userRepository = new UserRepository();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadPendingRequests();
        }

        // Postavi RecyclerView
        adapter = new FriendRequestAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    /**
     * Učitaj pending friend requests za trenutnog korisnika.
     */
    private void loadPendingRequests() {
        friendRepository.getPendingFriendRequests(currentUserId, new FriendRepository.FriendRequestListCallback() {
            @Override
            public void onSuccess(List<FriendRequest> requests) {
                if (requests == null || requests.isEmpty()) {
                    // Nema zahteva
                    emptyMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Dohvati User podatke za sve fromUserId
                    loadUsersForRequests(requests);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju zahteva: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Dohvati User objekte za sve fromUserId u listama requests.
     */
    private void loadUsersForRequests(List<FriendRequest> requests) {
        // Izvuci unique fromUserId-jeve
        List<String> userIds = new ArrayList<>();
        for (FriendRequest request : requests) {
            if (!userIds.contains(request.getFromUserId())) {
                userIds.add(request.getFromUserId());
            }
        }

        // Dohvati User objekte
        userRepository.getUsersByIds(userIds, new UserRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                // Kreiraj mapu userId -> User
                Map<String, User> usersById = new HashMap<>();
                for (User user : users) {
                    usersById.put(user.getUid(), user);
                }

                // Postavi podatke u adapter
                adapter.setData(requests, usersById);
                emptyMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju korisnika: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAcceptClick(FriendRequest request) {
        // Prihvati friend request
        friendRepository.acceptFriendRequest(
                request.getId(),
                request.getFromUserId(),
                request.getToUserId(),
                new FriendRepository.RepoCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "✅ Zahtev prihvaćen!", Toast.LENGTH_SHORT).show();
                        // Osveži listu
                        loadPendingRequests();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "❌ Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
