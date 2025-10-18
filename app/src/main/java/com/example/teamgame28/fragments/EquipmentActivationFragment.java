package com.example.teamgame28.fragments;

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
import com.example.teamgame28.adapters.EquipmentActivationAdapter;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.EquipmentType;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.Weapon;
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

        // Dobij trenutnog korisnika
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show();
            return view;
        }
        userId = currentUser.getUid();

        // Setup RecyclerViews
        setupRecyclerViews();

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
                }, false);

        recyclerWeapons.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWeapons.setAdapter(weaponsAdapter);
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
     * Klik na dugme "Potvrdi".
     */
    private void onConfirmClicked() {
        Toast.makeText(getContext(), "Aktivacija potvrđena! Spreman za borbu.", Toast.LENGTH_SHORT).show();

        // Ovde možeš pozvati BattleActivity ili se vratiti nazad
        // Npr. startActivity(new Intent(getActivity(), BattleActivity.class));

        // Ili samo zatvori fragment
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
