package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.adapters.EquipmentAdapter;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.EquipmentService;
import com.example.teamgame28.viewmodels.EquipmentViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment za prikaz prodavnice opreme.
 * Prikazuje tabove za Potions i Clothing sa dinamičkim cenama.
 */
public class ShopFragment extends Fragment {

    private EquipmentViewModel viewModel;
    private EquipmentService equipmentService;
    private UserRepository userRepository;

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvUserCoins;

    private EquipmentAdapter adapter;
    private UserProfile currentUserProfile;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        // Inicijalizacija view komponenti
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.recycler_view_equipment);
        progressBar = view.findViewById(R.id.progress_bar);
        tvUserCoins = view.findViewById(R.id.tv_user_coins);

        // Inicijalizacija services i repositories
        viewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);
        equipmentService = new EquipmentService();
        userRepository = new UserRepository();

        // Dobij trenutnog korisnika
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show();
            return view;
        }
        userId = currentUser.getUid();

        // Setup RecyclerView
        setupRecyclerView();

        // Učitaj UserProfile
        loadUserProfile();

        // Observe poruke i errore
        observeViewModel();

        // Tab listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    loadPotions();
                } else if (position == 1) {
                    loadClothing();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Učitaj Potions kao default
        loadPotions();

        return view;
    }

    /**
     * Postavlja RecyclerView sa adapterom.
     */
    private void setupRecyclerView() {
        adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentClickListener() {
            @Override
            public void onBuyClick(Equipment equipment) {
                buyEquipment(equipment);
            }

            @Override
            public void onItemClick(Equipment equipment) {
                // Možeš dodati prikaz detalja opreme
                Toast.makeText(getContext(), "Kliknuto na: " + equipment.getName(), Toast.LENGTH_SHORT).show();
            }
        }, true); // showBuyButton = true

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Učitava UserProfile da bi dobio trenutni broj coina i level.
     */
    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                currentUserProfile = userProfile;
                tvUserCoins.setText("Coins: " + userProfile.getCoins());
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Učitava napitke sa dinamičkim cenama.
     */
    private void loadPotions() {
        if (currentUserProfile == null) return;

        List<Potion> potions = equipmentService.getAvailablePotions();

        // Setuj dinamičke cene na osnovu user levela
        List<Equipment> equipmentList = new ArrayList<>(potions);
        equipmentService.setPricesBasedOnUserLevel(equipmentList, currentUserProfile.getLevel());

        adapter.setEquipmentList(equipmentList);
    }

    /**
     * Učitava odeću sa dinamičkim cenama.
     */
    private void loadClothing() {
        if (currentUserProfile == null) return;

        List<Clothing> clothes = equipmentService.getAvailableClothes();

        // Setuj dinamičke cene na osnovu user levela
        List<Equipment> equipmentList = new ArrayList<>(clothes);
        equipmentService.setPricesBasedOnUserLevel(equipmentList, currentUserProfile.getLevel());

        adapter.setEquipmentList(equipmentList);
    }

    /**
     * Kupuje opremu.
     */
    private void buyEquipment(Equipment equipment) {
        if (currentUserProfile == null) {
            Toast.makeText(getContext(), "Greška: profil nije učitan", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.buyEquipment(equipment, currentUserProfile.getCoins());
    }

    /**
     * Observe ViewModel LiveData za poruke i errore.
     */
    private void observeViewModel() {
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();

                // Osveži profil nakon kupovine
                loadUserProfile();

                // Osveži listu (cene ostaju iste, samo refresh UI)
                int selectedTab = tabLayout.getSelectedTabPosition();
                if (selectedTab == 0) {
                    loadPotions();
                } else {
                    loadClothing();
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
