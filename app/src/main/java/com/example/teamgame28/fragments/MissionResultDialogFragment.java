package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.teamgame28.R;

public class MissionResultDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    public static MissionResultDialogFragment newInstance(String message) {
        MissionResultDialogFragment fragment = new MissionResultDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_mission_result, container, false);

        // 🔹 Lottie animacija
        LottieAnimationView chestAnim = v.findViewById(R.id.chest_animation);
        chestAnim.setAnimation(R.raw.chest_open);
        chestAnim.playAnimation();

        // 🔹 Poruka (može biti “Boss poražen” ili “Misija istekla”)
        TextView tvMessage = v.findViewById(R.id.tv_message); // PRETPostAVKA DA IMAŠ TextView sa ovim ID-jem
        String message = getArguments() != null
                ? getArguments().getString(ARG_MESSAGE, "Greška!")
                : "Greška!";

        // POSTAVI PORUKU NA TEXTVIEW!
        tvMessage.setText(message);

        // 🔹 Zatvori dijalog
        v.findViewById(R.id.btn_ok).setOnClickListener(b -> dismiss());
        return v;
    }
}
