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

import com.example.teamgame28.R;
import com.example.teamgame28.model.BattleResult;
import com.example.teamgame28.model.Boss;
import com.example.teamgame28.repository.BossRepository;
import com.example.teamgame28.service.BattleService;
import com.example.teamgame28.service.BossService;

public class BattleFragment extends Fragment {

    private ProgressBar bossHPBar;
    private TextView bossHPText, attackCounterText, battleMessageText;
    private Button attackButton;

    private Boss currentBoss;
    private int attacksRemaining = 5;

    private BattleService battleService;
    private BossRepository bossRepository;

    // Privremene vrednosti (dok se ne poveže s tačkom 4)
    private static final int PLAYER_PP = 70;
    private static final double SUCCESS_RATE = 0.67;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_battle, container, false);

        // UI elementi
        bossHPBar = view.findViewById(R.id.bossHPBar);
        bossHPText = view.findViewById(R.id.bossHPText);
        attackCounterText = view.findViewById(R.id.attackCounterText);
        battleMessageText = view.findViewById(R.id.battleMessageText);
        attackButton = view.findViewById(R.id.attackButton);

        // Servisi
        bossRepository = new BossRepository();
        battleService = new BattleService(new BossService(bossRepository), requireContext());

        // Privremeni boss dok se ne povuče iz baze
        currentBoss = new Boss();
        currentBoss.setBossLevel(1);
        currentBoss.setHp(200);
        currentBoss.setCurrentHP(200);
        currentBoss.setDefeated(false);
        currentBoss.setCoinsReward(200);

        setupUI();
        setupAttackButton();

        return view;
    }

    private void setupUI() {
        bossHPBar.setMax(currentBoss.getHp());
        bossHPBar.setProgress(currentBoss.getCurrentHP());
        bossHPText.setText(currentBoss.getCurrentHP() + " / " + currentBoss.getHp());
        attackCounterText.setText("Attacks Remaining: " + attacksRemaining + " / 5");
    }

    private void setupAttackButton() {
        attackButton.setOnClickListener(v -> {
            if (attacksRemaining <= 0 || currentBoss.getDefeated()) {
                Toast.makeText(getContext(), "No more attacks available!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hit = battleService.performAttack(currentBoss, PLAYER_PP, SUCCESS_RATE * 100);

            if (hit) {
                Toast.makeText(getContext(), "Hit successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Attack missed!", Toast.LENGTH_SHORT).show();
            }

            attacksRemaining--;
            updateUIAfterAttack();

            if (currentBoss.getDefeated() || attacksRemaining == 0) {
                endBattle();
            }
        });
    }

    private void updateUIAfterAttack() {
        bossHPBar.setProgress(currentBoss.getCurrentHP());
        bossHPText.setText(currentBoss.getCurrentHP() + " / " + currentBoss.getHp());
        attackCounterText.setText("Attacks Remaining: " + attacksRemaining + " / 5");
    }

    private void endBattle() {
        battleService.calculateRewards(currentBoss, attacksRemaining, new com.example.teamgame28.service.BattleService.RewardsCallback() {
            @Override
            public void onSuccess(com.example.teamgame28.model.BattleResult result) {
                currentBoss.setAttemptedThisLevel(true);
                currentBoss.setCoinsRewardPercent(
                        result.getCoinsEarned() == currentBoss.getCoinsReward() ? 1.0 : 0.5
                );

                bossRepository.updateBoss(
                        currentBoss.getId() != null ? currentBoss.getId().toString() : "tempBoss",
                        currentBoss
                );

                openBattleResultFragment(result);
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("BattleFragment", "Failed to calculate rewards: " + e.getMessage());
                // Otvori fragment sa praznim rezultatom
                openBattleResultFragment(new com.example.teamgame28.model.BattleResult());
            }
        });
    }

    private void openBattleResultFragment(BattleResult result) {
        BattleResultFragment fragment = BattleResultFragment.newInstance(
                result.isBossDefeated(),
                result.getCoinsEarned(),
                result.isEquipmentDropped()
        );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
