package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.AllianceChatMessageAdapter;
import com.example.teamgame28.model.AllianceMessage;
import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.AllianceMessageService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AllianceChatFragment extends Fragment {

    private TextView tvAllianceName;
    private RecyclerView recyclerMessages;
    private TextView tvEmptyChat;
    private EditText etMessage;
    private ImageView btnSend;

    private AllianceChatMessageAdapter adapter;
    private AllianceMessageService messageService;
    private UserRepository userRepository;
    private FirebaseFirestore db;

    private String currentUserId;
    private String currentUsername;
    private String allianceId;
    private String allianceName;

    private ListenerRegistration messageListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alliance_chat, container, false);

        // Inicijalizacija view-a
        tvAllianceName = view.findViewById(R.id.tv_chat_alliance_name);
        recyclerMessages = view.findViewById(R.id.recycler_chat_messages);
        tvEmptyChat = view.findViewById(R.id.tv_empty_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);

        // Firebase i servisi
        messageService = new AllianceMessageService();
        userRepository = new UserRepository();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Postavi RecyclerView
        adapter = new AllianceChatMessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);

        // Učitaj podatke korisnika i saveza
        loadUserData();
        loadAllianceData();

        // Postavi listener za dugme Pošalji
        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Zaustavi notifikacioni listener dok si u chatu
        if (allianceId != null) {
            com.example.teamgame28.listener.AllianceMessageRealtimeListener.stopForAlliance(allianceId);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Ponovo pokreni notifikacioni listener kada izađeš iz chata
        if (allianceId != null && allianceName != null) {
            com.example.teamgame28.listener.AllianceMessageRealtimeListener.startForAlliance(
                    requireContext().getApplicationContext(),
                    currentUserId,
                    allianceId,
                    allianceName
            );
        }
    }

    private void loadUserData() {
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUsername = user.getUsername();
                }
            }

            @Override
            public void onFailure(Exception e) {
                currentUsername = "Korisnik";
            }
        });
    }

    private void loadAllianceData() {
        // Dohvati allianceId iz UserProfile
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(com.example.teamgame28.model.UserProfile userProfile) {
                if (userProfile != null && userProfile.getCurrentAllianceId() != null) {
                    allianceId = userProfile.getCurrentAllianceId();
                    loadAllianceName(allianceId);
                    startListeningForMessages();
                } else {
                    Toast.makeText(getContext(), "Nisi u savezu", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllianceName(String allianceId) {
        db.collection("alliances")
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        allianceName = documentSnapshot.getString("name");
                        if (allianceName != null) {
                            tvAllianceName.setText("Chat: " + allianceName);
                        }
                    }
                });
    }

    private void startListeningForMessages() {
        messageListener = messageService.listenForMessages(allianceId, (snapshots, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Greška pri učitavanju poruka: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshots != null) {
                // Proveri da li ima poruka uopšte
                if (snapshots.isEmpty()) {
                    recyclerMessages.setVisibility(View.GONE);
                    tvEmptyChat.setVisibility(View.VISIBLE);
                    return;
                }

                // Obrada DocumentChanges - PRAVILNO
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    AllianceMessage message = dc.getDocument().toObject(AllianceMessage.class);

                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        // Dodaj novu poruku u adapter
                        adapter.addMessage(message);
                        recyclerMessages.setVisibility(View.VISIBLE);
                        tvEmptyChat.setVisibility(View.GONE);

                        // Scroll na dno
                        recyclerMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Upiši poruku prvo!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allianceId == null) {
            Toast.makeText(getContext(), "Greška: nisi u savezu", Toast.LENGTH_SHORT).show();
            return;
        }

        messageService.sendMessage(allianceId, currentUserId, currentUsername, text, new AllianceMessageService.ServiceCallback() {
            @Override
            public void onSuccess() {
                etMessage.setText("");
                // Poruka će se automatski pojaviti kroz realtime listener
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Greška pri slanju: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Zaustavi listener kada napustiš fragment
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
