package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.example.teamgame28.adapters.MemberProgressAdapter;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.AllianceMission;
import com.example.teamgame28.service.SpecialTaskMissionService;
import com.example.teamgame28.viewmodels.AllianceViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fragment za prikaz specijalne misije saveza.
 */
public class AllianceMissionFragment extends Fragment {

    private AllianceViewModel viewModel;
    private MemberProgressAdapter adapter;
    private String userId;

    // UI komponente
    private ProgressBar progressBar;
    private LinearLayout layoutNoMission;
    private LinearLayout layoutActiveMission;
    private Button btnStartMission;
    private TextView tvBossHp;
    private TextView tvTimeRemaining;
    private TextView tvMissionStatus;
    private ProgressBar bossHpBar;
    private RecyclerView rvMemberProgress;
    private Button btnRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alliance_mission, container, false);

        // Inicijalizacija ViewModel-a
        viewModel = new ViewModelProvider(requireActivity()).get(AllianceViewModel.class);

        // Dohvati trenutnog korisnika
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show();
            return view;
        }
        userId = currentUser.getUid();
        new SpecialTaskMissionService().recordAllianceMessage(userId);



        // Inicijalizacija UI
        initializeUI(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        // 🔥 UČITAJ PODATKE IZ VIEWMODEL-A
        Log.d("AllianceMissionFrag", "📱 Fragment kreiran, proveravam podatke...");
        Alliance alliance = viewModel.getCurrentAlliance().getValue();
        AllianceMission mission = viewModel.getActiveMission().getValue();

        Log.d("AllianceMissionFrag", "  - Alliance: " + (alliance != null ? alliance.getName() : "null"));
        Log.d("AllianceMissionFrag", "  - Mission: " + (mission != null ? "postoji" : "null"));

        // Ako nema učitanih podataka, učitaj ih sada
        if (alliance == null) {
            Log.w("AllianceMissionFrag", "⚠️ Savez nije učitan, učitavam...");
            viewModel.loadUserAlliance(userId);
        } else if (mission == null) {
            Log.w("AllianceMissionFrag", "⚠️ Misija nije učitana, učitavam...");
            viewModel.loadActiveMission(alliance.getId());
        }

        // ℹ️ Napomena: Provera isteka misije se sada obavlja u observer-u (linija 177)

        return view;
    }

    private void initializeUI(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        layoutNoMission = view.findViewById(R.id.layout_no_mission);
        layoutActiveMission = view.findViewById(R.id.layout_active_mission);
        btnStartMission = view.findViewById(R.id.btn_start_mission);
        tvBossHp = view.findViewById(R.id.tv_boss_hp);
        tvTimeRemaining = view.findViewById(R.id.tv_time_remaining);
        tvMissionStatus = view.findViewById(R.id.tv_mission_status);
        bossHpBar = view.findViewById(R.id.boss_hp_bar);
        rvMemberProgress = view.findViewById(R.id.rv_member_progress);
        btnRefresh = view.findViewById(R.id.btn_refresh);
    }

    private void setupRecyclerView() {
        adapter = new MemberProgressAdapter();
        rvMemberProgress.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMemberProgress.setAdapter(adapter);
    }

    private void setupListeners() {
        btnStartMission.setOnClickListener(v -> {
            Alliance alliance = viewModel.getCurrentAlliance().getValue();
            if (alliance != null) {
                viewModel.startSpecialMission(alliance.getId(), userId);
            }
        });

        btnRefresh.setOnClickListener(v -> {
            viewModel.refreshData(userId);
        });
    }

    private void observeViewModel() {
        // 🔄 Loading indikator
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // 👥 Savez korisnika
        viewModel.getCurrentAlliance().observe(getViewLifecycleOwner(), alliance -> {
            if (alliance != null) {
                boolean isLeader = alliance.getLeaderId().equals(userId);
                btnStartMission.setVisibility(isLeader ? View.VISIBLE : View.GONE);

                // 🔥 Kad znamo koji je savez, slušaj realtime promene misije
                viewModel.listenToActiveMission(alliance.getId());
            }
        });

        // ⚔️ Aktivna misija
        viewModel.getActiveMission().observe(getViewLifecycleOwner(), mission -> {
            Log.d("AllianceMissionFrag", "📬 Primljena misija: " + (mission != null ? mission.getMissionId() : "null"));

            if (mission == null) {
                // ❌ Nema aktivne misije
                layoutNoMission.setVisibility(View.VISIBLE);
                layoutActiveMission.setVisibility(View.GONE);
                return;
            }

            // ⏳ Provera statusa misije i bossa
            long now = System.currentTimeMillis();
            long end = mission.getEndTime() != null ? mission.getEndTime().toDate().getTime() : 0;
            boolean expired = (end > 0 && now > end);
            if (expired && mission.isActive()) {
                Log.d("AllianceMissionFrag", "⏰ Misija je istekla i još je aktivna → pokrećem finalizaciju...");

                String currentUser = FirebaseAuth.getInstance().getUid();
                if (currentUser == null) return;

                SpecialTaskMissionService service = new SpecialTaskMissionService();

                // 🔹 1️⃣ Prvo proveri zadatke i smanji HP ako treba
                service.checkUnfinishedTasksForUser(currentUser, mission.getAllianceId(), () -> {

                    // ⏳ Sačekaj da Firestore završi ažuriranje HP-a
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> {
                                FirebaseFirestore.getInstance()
                                        .collection("alliance_missions")
                                        .document(mission.getMissionId())
                                        .get()
                                        .addOnSuccessListener(updatedDoc -> {
                                            if (!updatedDoc.exists()) return;
                                            AllianceMission updatedMission = updatedDoc.toObject(AllianceMission.class);
                                            if (updatedMission == null) return;

                                            int updatedHp = updatedMission.getBossHp();
                                            boolean defeated = updatedHp <= 0;

                                            Log.d("AllianceMissionFrag", "📊 Boss HP posle smanjenja: " + updatedHp);

                                            // 🔹 2️⃣ Deaktiviraj misiju (nakon smanjenja HP-a)
                                            updatedDoc.getReference().update("active", false);

                                            // 🔹 3️⃣ Prikaži dijalog tek sada
                                            if (defeated) {
                                                showMissionResultDialog(
                                                        "🎉 Čestitamo! Rok je istekao i uspešno ste porazili bosa!\n\n🎁 Nagrade su dodeljene svim članovima saveza:\n• Napitak\n• Komad odeće\n• 50% novčića\n• Bedž specijalnog pobednika"
                                                );
                                                Log.d("AllianceMissionFrag", "✅ Boss poražen nakon isteka roka – prikazujem pobedu.");
                                            } else {
                                                showMissionResultDialog(
                                                        "⏰ Rok za specijalnu misiju je istekao!\n\n❌ Boss nije poražen. Misija nije uspela."
                                                );
                                                Log.d("AllianceMissionFrag", "❌ Boss nije poražen nakon isteka roka – prikazujem poraz.");
                                            }

                                            // 🔹 4️⃣ Sakrij aktivni UI
                                            layoutNoMission.setVisibility(View.VISIBLE);
                                            layoutActiveMission.setVisibility(View.GONE);
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("AllianceMissionFrag", "❌ Greška pri ponovnom učitavanju misije", e));
                            }, 2000); // ⏳ kratko čekanje da Firestore ažurira bossHp
                });
                return;
            }

            // 🔹 AKO JE MISIJA VEĆ DEAKTIVIRANA → samo prikaži rezultat
            if (!mission.isActive() && expired) {
                if (mission.getBossHp() <= 0) {
                    showMissionResultDialog("🎉 Čestitamo! Rok je istekao i uspešno ste porazili bosa!\n\n🎁 Nagrade su dodeljene svim članovima saveza:\n• Napitak\n• Komad odeće\n• 50% novčića\n• Bedž specijalnog pobednika");
                    Log.d("AllianceMissionFrag", "✅ Boss poražen nakon isteka roka – prikazujem pobedu.");
                } else {
                    showMissionResultDialog("⏰ Rok za specijalnu misiju je istekao!\n\n❌ Boss nije poražen. Misija nije uspela.");
                    Log.d("AllianceMissionFrag", "❌ Boss nije poražen nakon isteka roka – prikazujem poraz.");
                }

                layoutNoMission.setVisibility(View.VISIBLE);
                layoutActiveMission.setVisibility(View.GONE);
                return;
            }

            // ✅ Aktivna misija koja još traje
            layoutNoMission.setVisibility(View.GONE);
            layoutActiveMission.setVisibility(View.VISIBLE);
            displayMissionInfo(mission);

            // ✅ Učitaj napredak
            viewModel.loadMissionProgress(mission.getMissionId());
        });

        // 📊 Napredak članova
        viewModel.getMissionProgress().observe(getViewLifecycleOwner(), adapter::setProgressList);

        // ⚠️ Poruke o greškama / uspehu
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


    private void showMissionResultDialog(String message) {
        if (getContext() == null) return;

        // 🔸 Jednostavan dijalog
        MissionResultDialogFragment dialog =
                MissionResultDialogFragment.newInstance(message);
        dialog.show(getParentFragmentManager(), "mission_result_dialog");
    }


    private void displayMissionInfo(AllianceMission mission) {
        // Prikaži HP bossa
        Alliance alliance = viewModel.getCurrentAlliance().getValue();
        int maxHp = alliance != null ? 100 * alliance.getMembers().size() : 100;
        int currentHp = mission.getBossHp();

        tvBossHp.setText("Boss HP: " + currentHp + " / " + maxHp);
        bossHpBar.setMax(maxHp);
        bossHpBar.setProgress(currentHp);

        // Prikaži preostalo vreme
        // Pretvori oba Timestamp-a u milisekunde
        long endMillis = mission.getEndTime().toDate().getTime();
        long nowMillis = Timestamp.now().toDate().getTime();

// Izračunaj razliku
        long timeRemaining = endMillis - nowMillis;

        if (timeRemaining > 0) {
            long days = TimeUnit.MILLISECONDS.toDays(timeRemaining);
            long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24;

            tvTimeRemaining.setText("⏱️ Preostalo: " + days + "d " + hours + "h");
            tvMissionStatus.setText("Status: 🔥 AKTIVNA");
            tvMissionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvTimeRemaining.setText("⏱️ Vreme isteklo!");
            tvMissionStatus.setText("Status: ⏰ ISTEKLA");
            tvMissionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }


        // Prikaži datum početka i kraja
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }
}
