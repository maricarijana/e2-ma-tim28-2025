package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.teamgame28.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnSubmit;
    private Button btnCancel;

    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        // Inicijalizacija view komponenti
        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        btnSubmit = view.findViewById(R.id.btn_submit_change_password);
        btnCancel = view.findViewById(R.id.btn_cancel);

        auth = FirebaseAuth.getInstance();

        // Dugme za promenu lozinke
        btnSubmit.setOnClickListener(v -> changePassword());

        // Dugme za otkazivanje
        btnCancel.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validacija
        if (TextUtils.isEmpty(oldPassword)) {
            Toast.makeText(getContext(), "Unesite staru lozinku", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(getContext(), "Unesite novu lozinku", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "Nova lozinka mora imati najmanje 6 karaktera", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getContext(), "Potvrdite novu lozinku", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Nove lozinke se ne poklapaju", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reauthenticate korisnika sa starom lozinkom
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Stara lozinka je tačna, promenite lozinku
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "Lozinka uspešno promenjena!", Toast.LENGTH_SHORT).show();
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        } else {
                                            Toast.makeText(getContext(), "Greška pri promeni lozinke", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "Stara lozinka nije tačna", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
