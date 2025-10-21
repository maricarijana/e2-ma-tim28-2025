package com.example.teamgame28.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Badge;
import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.LevelingService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class ProfileFragment extends Fragment {

    private ImageView profileAvatar;
    private TextView profileUsername;
    private TextView profileLevel;
    private TextView profileTitle;
    private TextView profileXp;
    private TextView profilePowerPoints;
    private TextView profileCoins;

    private TextView profileEquipment;
    private ImageView profileQrCode;
    private Button btnChangePassword;
    private Button btnViewStats;
    private Button btnViewLevelProgress;
    private LinearLayout privateDataContainer;

    private UserRepository userRepository;
    private FirebaseAuth auth;
    private String currentUserId;
    private String viewingUserId; // ID korisnika čiji profil gledamo

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inicijalizacija view komponenti
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileUsername = view.findViewById(R.id.profile_username);
        profileLevel = view.findViewById(R.id.profile_level);
        profileTitle = view.findViewById(R.id.profile_title);
        profileXp = view.findViewById(R.id.profile_xp);
        profilePowerPoints = view.findViewById(R.id.profile_power_points);
        profileCoins = view.findViewById(R.id.profile_coins);
        profileEquipment = view.findViewById(R.id.profile_equipment);
        profileQrCode = view.findViewById(R.id.profile_qr_code);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnViewStats = view.findViewById(R.id.btn_view_stats);
        btnViewLevelProgress = view.findViewById(R.id.btn_view_level_progress);
        privateDataContainer = view.findViewById(R.id.private_data_container);

        // Firebase i Repository
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Trenutno prijavljeni korisnik
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();

            // Proveri da li gledamo tuđi profil (iz Bundle-a)
            Bundle args = getArguments();
            if (args != null && args.containsKey("userId")) {
                viewingUserId = args.getString("userId");
            } else {
                viewingUserId = currentUserId; // Gledamo svoj profil
            }

            loadUserProfile(viewingUserId);
        }

        // Dugme za level progress
        btnViewLevelProgress.setOnClickListener(v -> {
            LevelProgressFragment levelProgressFragment = new LevelProgressFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, levelProgressFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Dugme za statistiku
        btnViewStats.setOnClickListener(v -> {
            StatsFragment statsFragment = new StatsFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, statsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Dugme za promenu lozinke
        btnChangePassword.setOnClickListener(v -> {
            ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, changePasswordFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadUserProfile(String userId) {
        // Prvo učitaj osnovne User podatke
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    // Zatim učitaj UserProfile podatke iz podkolekcije
                    userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
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

                                displayUserProfile(user, userProfile, userId.equals(currentUserId));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Greška pri učitavanju profila korisnika", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju korisnika", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserProfile(User user, UserProfile userProfile, boolean isOwnProfile) {
        // Avatar
        String avatarName = user.getAvatar();
        Log.d("PROFILE_DEBUG", "Avatar iz baze: " + avatarName);
        if (avatarName != null && !avatarName.isEmpty()) {
            // Konvertuj u lowercase jer su drawable resursi u lowercase formatu
            String resourceName = avatarName.toLowerCase();
            int resId = getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());
            Log.d("PROFILE_DEBUG", "Tražim drawable: " + resourceName + " (resId=" + resId + ")");
            if (resId != 0) {
                profileAvatar.setImageResource(resId);
            }
            else{
                Log.w("PROFILE_DEBUG", "⚠️ Drawable nije pronađen za: " + resourceName);
            }
        }

        // Osnovni podaci (javno vidljivi)
        profileUsername.setText(user.getUsername());
        profileLevel.setText("Level " + userProfile.getLevel());
        profileTitle.setText(userProfile.getTitle());
        profileXp.setText(userProfile.getXp() + " XP");

        // Privatni podaci (vidljivi samo vlasniku)
        if (isOwnProfile) {
            privateDataContainer.setVisibility(View.VISIBLE);
            btnChangePassword.setVisibility(View.VISIBLE);
            btnViewStats.setVisibility(View.VISIBLE);
            btnViewLevelProgress.setVisibility(View.VISIBLE);

            profilePowerPoints.setText(userProfile.getPowerPoints() + " PP");
            profileCoins.setText(String.valueOf(userProfile.getCoins()));

            // Bedževi
            LinearLayout badgesContainer = requireView().findViewById(R.id.badges_container);
            TextView noBadgesText = requireView().findViewById(R.id.profile_badges_placeholder);
            badgesContainer.removeAllViews();

            if (userProfile.getBadges() != null && !userProfile.getBadges().isEmpty()) {
                noBadgesText.setVisibility(View.GONE);

                for (Badge badge : userProfile.getBadges()) {
                    // svaki bedž ima istu sliku (npr. ic_badge_special)
                    LinearLayout badgeLayout = new LinearLayout(requireContext());
                    badgeLayout.setOrientation(LinearLayout.VERTICAL);
                    badgeLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                    badgeLayout.setPadding(16, 0, 16, 0);

                    ImageView badgeImage = new ImageView(requireContext());
                    badgeImage.setImageResource(R.drawable.ic_badge_special);
                    LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(100, 100);
                    badgeImage.setLayoutParams(imageParams);

                    TextView badgeName = new TextView(requireContext());
                    badgeName.setText(badge.getName());
                    badgeName.setTextSize(12);
                    badgeName.setTextColor(getResources().getColor(R.color.black));
                    badgeName.setGravity(android.view.Gravity.CENTER);

                    badgeLayout.addView(badgeImage);
                    badgeLayout.addView(badgeName);
                    badgesContainer.addView(badgeLayout);
                }

            } else {
                noBadgesText.setVisibility(View.VISIBLE);
            }


            // Oprema (posedovana)
            if (userProfile.getOwnedEquipment() != null && !userProfile.getOwnedEquipment().isEmpty()) {
                StringBuilder equipmentText = new StringBuilder();
                for (com.example.teamgame28.model.Equipment eq : userProfile.getOwnedEquipment()) {
                    equipmentText.append("• ").append(eq.getName());
                    if (eq.isActive()) {
                        equipmentText.append(" (ACTIVE)");
                    }
                    equipmentText.append("\n");
                }
                profileEquipment.setText(equipmentText.toString().trim());
            } else {
                profileEquipment.setText("Nema opreme");
            }
        } else {
            // Ako gledamo tuđi profil, sakrij privatne podatke
            privateDataContainer.setVisibility(View.GONE);
            btnChangePassword.setVisibility(View.GONE);
            btnViewStats.setVisibility(View.GONE);
            btnViewLevelProgress.setVisibility(View.GONE);
        }

        // QR kod (javno vidljiv)
        generateQRCode(user.getUid());
    }

    private void generateQRCode(String userId) {
        try {
            // Generišemo QR kod koji sadrži ID korisnika
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400);
            profileQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
        }
    }
}
