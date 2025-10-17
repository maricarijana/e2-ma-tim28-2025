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

import com.example.teamgame28.R;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.LevelingService;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Fragment za prikaz napredovanja kroz nivoe.
 * Prikazuje: titulu, PP, trenutni XP, XP potreban za sledeći nivo, progress bar.
 */
public class LevelProgressFragment extends Fragment {

    private TextView textCurrentLevel;
    private TextView textTitle;
    private TextView textPowerPoints;
    private TextView textCurrentXp;
    private TextView textXpForNextLevel;
    private TextView textXpRemaining;
    private ProgressBar progressBar;

    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_level_progress, container, false);

        // Inicijalizuj view komponente
        textCurrentLevel = view.findViewById(R.id.text_current_level);
        textTitle = view.findViewById(R.id.text_title);
        textPowerPoints = view.findViewById(R.id.text_power_points);
        textCurrentXp = view.findViewById(R.id.text_current_xp);
        textXpForNextLevel = view.findViewById(R.id.text_xp_for_next_level);
        textXpRemaining = view.findViewById(R.id.text_xp_remaining);
        progressBar = view.findViewById(R.id.progress_bar_level);

        userRepository = new UserRepository();

        // Učitaj podatke o trenutnom korisniku
        loadUserLevelProgress();

        return view;
    }

    private void loadUserLevelProgress() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (userProfile != null) {
                    // VAŽNO: Preračunaj nivo, titulu i PP iz XP-a PRIJE prikaza
                    int calculatedLevel = LevelingService.calculateLevelFromXp(userProfile.getXp());
                    userProfile.setLevel(calculatedLevel);

                    // Postavi titulu za trenutni nivo
                    String title = LevelingService.getTitleForLevel(calculatedLevel);
                    userProfile.setTitle(title);

                    // Postavi ukupan PP za trenutni nivo
                    int totalPP = LevelingService.getTotalPpForLevel(calculatedLevel);
                    userProfile.setPowerPoints(totalPP);

                    displayLevelProgress(userProfile);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayLevelProgress(UserProfile userProfile) {
        int currentLevel = userProfile.getLevel();
        int currentXp = userProfile.getXp();
        int powerPoints = userProfile.getPowerPoints();
        String title = userProfile.getTitle();

        // Izračunaj XP potreban za sledeći nivo
        int xpForCurrentLevel = LevelingService.getXpRequiredForLevel(currentLevel);
        int xpForNextLevel = LevelingService.getXpRequiredForLevel(currentLevel + 1);
        int xpRemaining = LevelingService.getXpRemainingForNextLevel(currentXp, currentLevel);

        // Postavi tekstove
        textCurrentLevel.setText("Nivo " + currentLevel);
        textTitle.setText(title);
        textPowerPoints.setText(powerPoints + " PP");
        textCurrentXp.setText("Trenutni XP: " + currentXp);
        textXpForNextLevel.setText("XP za nivo " + (currentLevel + 1) + ": " + xpForNextLevel);
        textXpRemaining.setText("Preostalo: " + xpRemaining + " XP");

        // Izračunaj progress
        int xpInCurrentLevel = currentXp - xpForCurrentLevel;
        int xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel;

        if (xpNeededForNextLevel > 0) {
            int progress = (int) ((xpInCurrentLevel / (float) xpNeededForNextLevel) * 100);
            progressBar.setProgress(Math.max(0, Math.min(100, progress)));
        } else {
            progressBar.setProgress(0);
        }
    }
}
