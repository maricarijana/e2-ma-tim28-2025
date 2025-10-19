package com.example.teamgame28.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.teamgame28.activities.BattleActivity;
import com.example.teamgame28.adapters.EquipmentActivationAdapter;
import com.example.teamgame28.model.BattleData;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.EquipmentType;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.example.teamgame28.repository.BossRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.BattleService;
import com.example.teamgame28.service.BossService;
import com.example.teamgame28.service.EquipmentService;
import com.example.teamgame28.viewmodels.EquipmentViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment za aktivaciju opreme pre borbe.
 * Prikazuje posedovanu opremu i omogućava aktivaciju/deaktivaciju.
 */
public class EquipmentActivationFragment extends Fragment {

    private EquipmentViewModel viewModel;
    private UserRepository userRepository;
    private EquipmentService equipmentService;

    private RecyclerView recyclerActiveEquipment;
    private RecyclerView recyclerPotions;
    private RecyclerView recyclerClothing;
    private RecyclerView recyclerWeapons;

    private TextView tvNoActiveEquipment;
    private TextView tvNoPotions;
    private TextView tvNoClothing;
    private TextView tvNoWeapons;

    private ProgressBar progressBar;
    private Button btnConfirm;

    private EquipmentActivationAdapter activeAdapter;
    private EquipmentActivationAdapter potionsAdapter;
    private EquipmentActivationAdapter clothingAdapter;
    private EquipmentActivationAdapter weaponsAdapter;

    private String userId;
    private UserProfile currentUserProfile;
    private BattleService battleService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        battleService = new BattleService(new BossService(new BossRepository()), requireContext());
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_activation, container, false);

        // Inicijalizacija view komponenti
        recyclerActiveEquipment = view.findViewById(R.id.recycler_view_active_equipment);
        recyclerPotions = view.findViewById(R.id.recycler_view_potions);
        recyclerClothing = view.findViewById(R.id.recycler_view_clothing);
        recyclerWeapons = view.findViewById(R.id.recycler_view_weapons);

        tvNoActiveEquipment = view.findViewById(R.id.tv_no_active_equipment);
        tvNoPotions = view.findViewById(R.id.tv_no_potions);
        tvNoClothing = view.findViewById(R.id.tv_no_clothing);
        tvNoWeapons = view.findViewById(R.id.tv_no_weapons);

        progressBar = view.findViewById(R.id.progress_bar);
        btnConfirm = view.findViewById(R.id.btn_confirm);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);
        userRepository = new UserRepository();
        equipmentService = new EquipmentService();

        // Dobij trenutnog korisnika
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show();
            return view;
        }
        userId = currentUser.getUid();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Učitaj UserProfile
        loadUserProfile();

        // Učitaj podatke
        loadEquipment();

        // Observe ViewModel
        observeViewModel();

        // Dugme za potvrdu
        btnConfirm.setOnClickListener(v -> onConfirmClicked());

        return view;
    }

    /**
     * Postavlja sve RecyclerView-ove sa adapterima.
     */
    private void setupRecyclerViews() {
        // Aktivna oprema
        activeAdapter = new EquipmentActivationAdapter(
                new EquipmentActivationAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onActivateClick(Equipment equipment) {
                        // Ne bi trebalo da se desi (ova lista prikazuje već aktivnu opremu)
                    }

                    @Override
                    public void onDeactivateClick(Equipment equipment) {
                        viewModel.deactivateEquipment(equipment.getId());
                    }

                    @Override
                    public void onUpgradeClick(Weapon weapon) {
                        // Ne prikazuje se na aktivnoj listi
                    }
                }, true); // isActiveList = true

        recyclerActiveEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerActiveEquipment.setAdapter(activeAdapter);

        // Napici (neaktivni)
        potionsAdapter = new EquipmentActivationAdapter(
                new EquipmentActivationAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onActivateClick(Equipment equipment) {
                        viewModel.activateEquipment(equipment);
                    }

                    @Override
                    public void onDeactivateClick(Equipment equipment) {
                        // Ne bi trebalo da se desi
                    }

                    @Override
                    public void onUpgradeClick(Weapon weapon) {
                        // Potions ne mogu da se upgrade-uju
                    }
                }, false); // isActiveList = false

        recyclerPotions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPotions.setAdapter(potionsAdapter);

        // Odeća (neaktivna)
        clothingAdapter = new EquipmentActivationAdapter(
                new EquipmentActivationAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onActivateClick(Equipment equipment) {
                        viewModel.activateEquipment(equipment);
                    }

                    @Override
                    public void onDeactivateClick(Equipment equipment) {
                        // Ne bi trebalo da se desi
                    }

                    @Override
                    public void onUpgradeClick(Weapon weapon) {
                        // Clothing ne može da se upgrade-uje
                    }
                }, false);

        recyclerClothing.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerClothing.setAdapter(clothingAdapter);

        // Oružje (neaktivno)
        weaponsAdapter = new EquipmentActivationAdapter(
                new EquipmentActivationAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onActivateClick(Equipment equipment) {
                        viewModel.activateEquipment(equipment);
                    }

                    @Override
                    public void onDeactivateClick(Equipment equipment) {
                        // Ne bi trebalo da se desi
                    }

                    @Override
                    public void onUpgradeClick(Weapon weapon) {
                        upgradeWeapon(weapon);
                    }
                }, false);

        recyclerWeapons.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWeapons.setAdapter(weaponsAdapter);
    }

    /**
     * Učitava UserProfile.
     */
    private void loadUserProfile() {
        userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                currentUserProfile = userProfile;
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Učitava posedovanu i aktivnu opremu.
     */
    private void loadEquipment() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.loadOwnedEquipment(userId);
        viewModel.loadActiveEquipment(userId);
    }

    /**
     * Observe ViewModel LiveData.
     */
    private void observeViewModel() {
        // Aktivna oprema
        viewModel.getActiveEquipment().observe(getViewLifecycleOwner(), activeEquipment -> {
            if (activeEquipment != null && !activeEquipment.isEmpty()) {
                activeAdapter.setEquipmentList(activeEquipment);
                recyclerActiveEquipment.setVisibility(View.VISIBLE);
                tvNoActiveEquipment.setVisibility(View.GONE);
            } else {
                recyclerActiveEquipment.setVisibility(View.GONE);
                tvNoActiveEquipment.setVisibility(View.VISIBLE);
            }
        });

        // Posedovana oprema - filtriraj po tipu
        viewModel.getOwnedEquipment().observe(getViewLifecycleOwner(), ownedEquipment -> {
            if (ownedEquipment != null) {
                filterAndDisplayEquipment(ownedEquipment);
            }
            progressBar.setVisibility(View.GONE);
        });

        // Poruke
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();

                // Osvježi podatke nakon aktivacije/deaktivacije
                loadEquipment();
            }
        });

        // Errori
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });

        // Loading
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Filtrira posedovanu opremu po tipu i prikazuje u odgovarajućim RecyclerView-ovima.
     */
    private void filterAndDisplayEquipment(List<Equipment> ownedEquipment) {
        List<Equipment> potions = new ArrayList<>();
        List<Equipment> clothing = new ArrayList<>();
        List<Equipment> weapons = new ArrayList<>();

        // Filtriraj neaktivnu opremu po tipu
        for (Equipment eq : ownedEquipment) {
            if (eq.isActive()) continue; // Preskači aktivnu opremu

            if (eq instanceof Potion) {
                // Prikaži SVE potione, uključujući consumed PERMANENT
                // Adapter će onemogućiti dugme za consumed potione
                potions.add(eq);
            } else if (eq instanceof Clothing) {
                clothing.add(eq);
            } else if (eq instanceof Weapon) {
                weapons.add(eq);
            }
        }

        // Potions
        if (!potions.isEmpty()) {
            potionsAdapter.setEquipmentList(potions);
            recyclerPotions.setVisibility(View.VISIBLE);
            tvNoPotions.setVisibility(View.GONE);
        } else {
            recyclerPotions.setVisibility(View.GONE);
            tvNoPotions.setVisibility(View.VISIBLE);
        }

        // Clothing
        if (!clothing.isEmpty()) {
            clothingAdapter.setEquipmentList(clothing);
            recyclerClothing.setVisibility(View.VISIBLE);
            tvNoClothing.setVisibility(View.GONE);
        } else {
            recyclerClothing.setVisibility(View.GONE);
            tvNoClothing.setVisibility(View.VISIBLE);
        }

        // Weapons
        if (!weapons.isEmpty()) {
            weaponsAdapter.setEquipmentList(weapons);
            recyclerWeapons.setVisibility(View.VISIBLE);
            tvNoWeapons.setVisibility(View.GONE);
        } else {
            recyclerWeapons.setVisibility(View.GONE);
            tvNoWeapons.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Klik na dugme "Potvrdi" - pokreće Boss Battle.
     */
    private void onConfirmClicked() {
        Toast.makeText(getContext(), "Aktivacija potvrđena! Spreman za borbu.", Toast.LENGTH_SHORT).show();

        battleService.prepareBattleData(userId, new BattleService.PrepareBattleCallback() {
            @Override
            public void onSuccess(BattleData battleData) {
                Intent intent = new Intent(getActivity(), BattleActivity.class);
                intent.putExtra("BOSS_ID", battleData.getBossId());  // Boss ID iz Firestore
                intent.putExtra("BOSS_LEVEL", battleData.getBossLevel());
                intent.putExtra("BOSS_HP", battleData.getBossHP());
                intent.putExtra("BOSS_CURRENT_HP", battleData.getBossCurrentHP());
                intent.putExtra("BOSS_COINS_REWARD", battleData.getBossCoinsReward());
                intent.putExtra("IS_EXISTING_BOSS", battleData.isExistingBoss());
                intent.putExtra("PLAYER_PP", battleData.getTotalPP());
                intent.putExtra("SUCCESS_RATE", battleData.getSuccessRate());
                intent.putExtra("TOTAL_ATTACKS", battleData.getTotalAttacks());
                intent.putExtra("ACTIVE_EQUIPMENT", battleData.getActiveEquipmentNames());
                startActivity(intent);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Upgrade oružja.
     */
    private void upgradeWeapon(Weapon weapon) {
        // Proveri da li je UserProfile učitan
        if (currentUserProfile == null) {
            Toast.makeText(getContext(), "Greška: profil nije učitan", Toast.LENGTH_SHORT).show();
            return;
        }

        int userCoins = currentUserProfile.getCoins();
        int userLevel = currentUserProfile.getLevel();

        // Izračunaj cenu upgrade-a (60% boss reward-a od prethodnog levela)
        int upgradeCost = equipmentService.calculateUpgradePriceForLevel(userLevel);

        // Proveri da li korisnik ima dovoljno novca
        if (userCoins < upgradeCost) {
            Toast.makeText(getContext(),
                "Nemate dovoljno novčića! Potrebno: " + upgradeCost + ", imate: " + userCoins,
                Toast.LENGTH_LONG).show();
            return;
        }

        // Prikaži dijalog za potvrdu
        showUpgradeConfirmationDialog(weapon, upgradeCost, userCoins);
    }

    /**
     * Prikazuje dijalog za potvrdu upgrade-a.
     */
    private void showUpgradeConfirmationDialog(Weapon weapon, int upgradeCost, int userCoins) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Upgrade oružja")
                .setMessage("Želite da unapredite " + weapon.getName() + "?\n\n" +
                        "Cena: " + upgradeCost + " coins\n" +
                        "Vaši coins: " + userCoins + "\n\n" +
                        "Trenutna verovatnoća: " + String.format("%.1f", weapon.getProbability() * 100) + "%\n" +
                        "Nova verovatnoća: " + String.format("%.1f", (weapon.getProbability() + 0.01) * 100) + "%\n" +
                        "Novi nivo: " + (weapon.getUpgradeLevel() + 1))
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    // Pozovi upgrade
                    viewModel.upgradeWeapon(weapon, userCoins, upgradeCost);

                    // Osveži profil
                    loadUserProfile();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
}
