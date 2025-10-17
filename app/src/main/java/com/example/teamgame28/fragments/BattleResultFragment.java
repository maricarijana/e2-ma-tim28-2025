package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.teamgame28.R;

public class BattleResultFragment extends Fragment {

    private static final String ARG_DEFEATED = "arg_defeated";
    private static final String ARG_COINS = "arg_coins";
    private static final String ARG_EQUIPMENT = "arg_equipment";

    public static BattleResultFragment newInstance(boolean defeated, int coins, boolean equipmentDropped) {
        BattleResultFragment fragment = new BattleResultFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_DEFEATED, defeated);
        args.putInt(ARG_COINS, coins);
        args.putBoolean(ARG_EQUIPMENT, equipmentDropped);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_reward, container, false);

        TextView resultTitle = view.findViewById(R.id.resultTitleText);
        TextView resultMessage = view.findViewById(R.id.resultMessageText);
        TextView coinsText = view.findViewById(R.id.coinsRewardText);
        LinearLayout equipmentLayout = view.findViewById(R.id.equipmentRewardLayout);
        ImageView chestClosed = view.findViewById(R.id.chestClosedImage);
        ImageView chestOpen = view.findViewById(R.id.chestOpenImage);
        LottieAnimationView chestAnimation = view.findViewById(R.id.chestOpenAnimation);
        LottieAnimationView confettiAnimation = view.findViewById(R.id.confettiAnimation);
        Button continueButton = view.findViewById(R.id.continueButton);

        Bundle args = getArguments();
        if (args != null) {
            boolean defeated = args.getBoolean(ARG_DEFEATED);
            int coins = args.getInt(ARG_COINS);
            boolean equipmentDropped = args.getBoolean(ARG_EQUIPMENT);

            resultTitle.setText(defeated ? "VICTORY!" : "TRY AGAIN!");
            resultMessage.setText(defeated ? "You have defeated the boss!" : "Boss survived. Better luck next time!");
            coinsText.setText(coins + " Coins");

            if (equipmentDropped) {
                equipmentLayout.setVisibility(View.VISIBLE);
            } else {
                equipmentLayout.setVisibility(View.GONE);
            }

            // Animacije
            chestClosed.setVisibility(View.GONE);
            chestOpen.setVisibility(View.VISIBLE);

            chestAnimation.setAnimation(R.raw.chest_open);
            chestAnimation.setVisibility(View.VISIBLE);
            chestAnimation.playAnimation();

            confettiAnimation.setAnimation(R.raw.confetti);
            confettiAnimation.setVisibility(View.VISIBLE);
            confettiAnimation.playAnimation();

            continueButton.setVisibility(View.VISIBLE);
        }

        continueButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .popBackStack(); // Vrati se nazad na BattleFragment
        });

        return view;
    }
}
