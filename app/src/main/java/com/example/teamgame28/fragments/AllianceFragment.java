package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.viewmodels.AllianceViewModel;
import com.example.teamgame28.fragments.AllianceMissionFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;

/**
 * Fragment za pregled saveza i kreiranje novog saveza.
 */
public class AllianceFragment extends Fragment {

    private AllianceViewModel viewModel;
    private String userId;

    // UI komponente
    private ProgressBar progressBar;
    private TextView tvAllianceName;
    private TextView tvLeader;
    private TextView tvMemberCount;
    private Button btnViewMission;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alliance, container, false);

        // Inicijalizacija ViewModel-a
        viewModel = new ViewModelProvider(this).get(AllianceViewModel.class);

        // Dohvati trenutnog korisnika
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show();
            return view;
        }
        userId = currentUser.getUid();
        Log.d("AllianceFragment", "🔑 Ulogovani korisnik ID: " + userId);

        // Inicijalizacija UI
        initializeUI(view);
        setupListeners();
        observeViewModel();

        // Učitaj savez korisnika
        viewModel.loadUserAlliance(userId);

        return view;
    }

    private void initializeUI(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        tvAllianceName = view.findViewById(R.id.tv_alliance_name);
        tvLeader = view.findViewById(R.id.tv_leader);
        tvMemberCount = view.findViewById(R.id.tv_member_count);
        btnViewMission = view.findViewById(R.id.btn_view_mission);
    }

    private void setupListeners() {
        btnViewMission.setOnClickListener(v -> {
            // Navigacija na AllianceMissionFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AllianceMissionFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            Log.d("AllianceFragment", "📬 Primljen savez iz ViewModel: " + (alliance != null ? alliance.getName() : "null"));

            if (alliance == null) {
                // Korisnik nema savez
                Log.w("AllianceFragment", "⚠️ Alliance je null - korisnik nije u savezu");
                Toast.makeText(getContext(), "Niste u savezu. Proverite Firestore bazu!", Toast.LENGTH_LONG).show();
            } else {
                // Korisnik ima savez
                Log.d("AllianceFragment", "✅ Prikazujem informacije o savezu: " + alliance.getName());
                displayAllianceInfo(alliance);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null && !success.isEmpty()) {
                Toast.makeText(getContext(), success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAllianceInfo(Alliance alliance) {
        tvAllianceName.setText("Savez: " + alliance.getName());
        tvLeader.setText("Vođa: " + alliance.getLeaderId().substring(0, Math.min(8, alliance.getLeaderId().length())));
        tvMemberCount.setText("Članovi: " + alliance.getMembers().size());
    }
}
