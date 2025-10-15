package com.example.teamgame28.service;

import com.example.teamgame28.model.User;
import com.example.teamgame28.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public Task<String> registerUser(String email, String password, String confirmPassword, String username, String avatarName) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty()) {
            tcs.setException(new Exception("Sva polja moraju biti popunjena."));
            return tcs.getTask();
        }
        if (!password.equals(confirmPassword)) {
            tcs.setException(new Exception("Lozinke se ne poklapaju."));
            return tcs.getTask();
        }

        userRepository.isEmailTaken(email)
                .addOnSuccessListener(exists -> {
                    if (exists) {
                        tcs.setException(new Exception("User with this account already exists."));
                    } else {
                        userRepository.registerAccount(email, password)
                                .addOnSuccessListener(authResult -> {
                                    String uid = authResult.getUser().getUid();
                                    User newUser = new User(uid, email, username, password, avatarName);

                                    userRepository.saveProfile(newUser)
                                            .addOnSuccessListener(ref -> {
                                                userRepository.sendEmailVerificationToCurrentUser();
                                                tcs.setResult("Registration successfull! Check email for activation of account.");
                                            })
                                            .addOnFailureListener(tcs::setException);
                                })
                                .addOnFailureListener(tcs::setException);
                    }
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }
    public Task<String> loginUser(String email, String password) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        if (email.isEmpty() || password.isEmpty()) {
            tcs.setException(new Exception("Email i lozinka moraju biti uneti."));
            return tcs.getTask();
        }

        userRepository.authLogin(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null && authResult.getUser().isEmailVerified()) {
                        tcs.setResult("Uspešno prijavljivanje!");
                    } else {
                        tcs.setException(new Exception("Email nije verifikovan. Proverite poštu."));
                    }
                })
                .addOnFailureListener(e -> tcs.setException(new Exception("Neuspešna prijava: " + e.getMessage())));

        return tcs.getTask();
    }

    public void logout() {
        userRepository.signOut();
    }

}

