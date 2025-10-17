package com.example.teamgame28.service;

import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
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
                        // Ažuriraj login streak nakon uspešnog logovanja
                        String userId = authResult.getUser().getUid();
                        userRepository.updateLoginStreak(userId);

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

    /**
     * Dodaje XP korisniku i automatski proverava level up sa novim leveling sistemom.
     * Poziva LevelingService za kalkulacije i UserRepository za upis u bazu.
     */
    public void addXpToUserWithLevelUp(String userId, int xpToAdd) {
        // Prvo učitaj trenutni profil
        userRepository.getUserProfileById(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                int oldLevel = profile.getLevel();
                int oldXp = profile.getXp();
                int oldPp = profile.getPowerPoints();

                // Dodaj novi XP
                int newXp = oldXp + xpToAdd;
                profile.setXp(newXp);

                // Proveri novi nivo pomoću LevelingService
                int newLevel = LevelingService.calculateLevelFromXp(newXp);

                // Ako je dostignut novi nivo, dodaj PP nagrade
                if (newLevel > oldLevel) {
                    int totalPpReward = 0;
                    for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                        totalPpReward += LevelingService.getPpRewardForLevel(lvl);
                    }

                    profile.setLevel(newLevel);
                    profile.setPowerPoints(oldPp + totalPpReward);
                    profile.updateTitle();

                    android.util.Log.d("UserService", "🎉 Level UP! " + userId +
                            " je prešao sa nivoa " + oldLevel + " na nivo " + newLevel +
                            " i dobio " + totalPpReward + " PP!");
                }

                // Snimi ažurirani profil u bazu
                userRepository.updateUserProfile(userId, profile)
                        .addOnSuccessListener(aVoid -> {
                            // Posle toga dodaj XP u istoriju (pozovi postojeću metodu)
                            userRepository.addXpToUser(userId, xpToAdd);
                        })
                        .addOnFailureListener(e ->
                            android.util.Log.e("UserService", "Greška pri ažuriranju profila: ", e)
                        );
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("UserService", "Greška pri učitavanju profila: ", e);
            }
        });
    }

}

