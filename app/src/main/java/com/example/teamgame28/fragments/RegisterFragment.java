package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Avatar;
import com.example.teamgame28.viewmodels.UserViewModel;

import java.util.Arrays;
import java.util.List;

public class RegisterFragment extends Fragment {

    private UserViewModel userViewModel;
    private String selectedAvatarName = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        EditText emailInput = view.findViewById(R.id.editTextEmail);
        EditText passwordInput = view.findViewById(R.id.editTextPassword);
        EditText confirmInput = view.findViewById(R.id.editTextConfirmPassword);
        EditText usernameInput = view.findViewById(R.id.editTextUsername);
        Button registerButton = view.findViewById(R.id.buttonRegister);
        GridLayout avatarGrid = view.findViewById(R.id.avatarGrid);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        setupAvatars(avatarGrid);

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            String confirm = confirmInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();

            if (selectedAvatarName == null) {
                Toast.makeText(getContext(), "Morate izabrati avatar!", Toast.LENGTH_SHORT).show();
                return;
            }

            userViewModel.registerUser(email, pass, confirm, username, selectedAvatarName);

            userViewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
                if (msg == null) return;

                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();

                if (msg.toLowerCase().contains("success")) {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.auth_fragment_container, new LoginFragment())
                            .addToBackStack(null)
                            .commit();

                    Toast.makeText(getContext(), "Proveri email i prijavi se", Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
    }

    /** 🔹 Izdvojena funkcija za prikaz i izbor avatara */
    private void setupAvatars(GridLayout avatarGrid) {
        List<Avatar> avatars = Arrays.asList(
                new Avatar("Avatar1", R.drawable.avatar1),
                new Avatar("Avatar2", R.drawable.avatar2),
                new Avatar("Avatar3", R.drawable.avatar3),
                new Avatar("Avatar4", R.drawable.avatar4),
                new Avatar("Avatar5", R.drawable.avatar5)
        );

        for (Avatar avatar : avatars) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(avatar.getImageResId());
            imageView.setPadding(16, 16, 16, 16);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(200, 200));

            imageView.setOnClickListener(v -> {
                selectedAvatarName = avatar.getName();
                Toast.makeText(getContext(), "Izabrali ste " + avatar.getName(), Toast.LENGTH_SHORT).show();

                // reset alpha na svima
                for (int i = 0; i < avatarGrid.getChildCount(); i++) {
                    avatarGrid.getChildAt(i).setAlpha(1f);
                }
                // obeleži selektovanog
                imageView.setAlpha(0.5f);
            });

            avatarGrid.addView(imageView);
        }
    }
}
