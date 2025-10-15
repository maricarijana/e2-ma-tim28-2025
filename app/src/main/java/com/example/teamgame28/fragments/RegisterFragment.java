package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.viewmodels.UserViewModel;

public class RegisterFragment extends Fragment {

    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        EditText emailInput = view.findViewById(R.id.editTextEmail);
        EditText passwordInput = view.findViewById(R.id.editTextPassword);
        EditText confirmInput = view.findViewById(R.id.editTextConfirmPassword);
        EditText usernameInput = view.findViewById(R.id.editTextUsername);
        EditText avatarInput = view.findViewById(R.id.editTextAvatar);
        Button registerButton = view.findViewById(R.id.buttonRegister);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            String confirm = confirmInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String avatar = avatarInput.getText().toString().trim();

            userViewModel.registerUser(email, pass, confirm, username, avatar);

            userViewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
                if (msg == null) return;

                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();

                // 🔹 Ako poruka sadrži reč "success" → registracija je uspela
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
}
