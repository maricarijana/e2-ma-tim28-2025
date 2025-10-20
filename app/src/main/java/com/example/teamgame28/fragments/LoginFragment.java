package com.example.teamgame28.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.activities.MainActivity;
import com.example.teamgame28.viewmodels.UserViewModel;

public class LoginFragment extends Fragment {

    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        EditText emailInput = view.findViewById(R.id.editTextEmail);
        EditText passwordInput = view.findViewById(R.id.editTextPassword);
        Button loginButton = view.findViewById(R.id.buttonLogin);
        TextView registerLink = view.findViewById(R.id.textRegister);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            userViewModel.loginUser(email, password)
                    .addOnSuccessListener(msg -> {
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        requireActivity().finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        registerLink.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.auth_fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
