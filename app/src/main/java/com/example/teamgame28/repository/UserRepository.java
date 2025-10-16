package com.example.teamgame28.repository;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private static final String TAG = "UserRepo";
    private static final String USERS_COLLECTION = "app_users";
    private static final String PROFILE_SUBCOLLECTION = "profile";
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    public UserRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }
    public Task<AuthResult> registerAccount(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }
    @Nullable
    public Task<Void> sendEmailVerificationToCurrentUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            Log.w(TAG, "sendEmailVerificationToCurrentUser: no logged in user");
            return null;
        }
        return u.sendEmailVerification();
    }
    public Task<DocumentReference> saveProfile(User user) {
        Log.d(TAG, "saveProfile() called for: " + user.getEmail());

        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();

        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            tcs.setException(new IllegalArgumentException("User or UID missing"));
            return tcs.getTask();
        }

        // Osnovni User podaci (bez profila)
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("uid", user.getUid());
        userPayload.put("email", user.getEmail());
        userPayload.put("username", user.getUsername());
        userPayload.put("avatar", user.getAvatar());
        userPayload.put("isActivated", false);
        userPayload.put("createdAt", new Date());

        // Kreiraj novi UserProfile sa default vrednostima
        UserProfile userProfile = new UserProfile();
        Map<String, Object> profilePayload = new HashMap<>();
        profilePayload.put("level", userProfile.getLevel());
        profilePayload.put("title", userProfile.getTitle());
        profilePayload.put("xp", userProfile.getXp());
        profilePayload.put("qrCode", userProfile.getQrCode());
        profilePayload.put("currentEquipment", userProfile.getCurrentEquipment());
        profilePayload.put("powerPoints", userProfile.getPowerPoints());
        profilePayload.put("coins", userProfile.getCoins());
        profilePayload.put("badges", userProfile.getBadges());
        profilePayload.put("equipment", userProfile.getEquipment());

        // Prvo snimi osnovne User podatke
        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userPayload)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "User data stored for uid=" + user.getUid());

                    // Zatim snimi UserProfile u podkolekciju
                    firestore.collection(USERS_COLLECTION)
                            .document(user.getUid())
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(user.getUid())
                            .set(profilePayload)
                            .addOnSuccessListener(profileVoid -> {
                                Log.i(TAG, "UserProfile stored for uid=" + user.getUid());
                                // Vrati referencu na User dokument
                                firestore.collection(USERS_COLLECTION)
                                        .document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> tcs.setResult(documentSnapshot.getReference()))
                                        .addOnFailureListener(tcs::setException);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "UserProfile write failed: ", e);
                                tcs.setException(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User write failed: ", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }
    public Task<AuthResult> authLogin(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }


    public Task<Boolean> isEmailTaken(String email) {
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();

        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> tcs.setResult(!qs.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "isEmailTaken failed", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    public void signOut() {
        auth.signOut();
        Log.d(TAG, "User signed out");
    }

    // Callback interface za asinhrone operacije
    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    // Callback interface za UserProfile
    public interface UserProfileCallback {
        void onSuccess(UserProfile userProfile);
        void onFailure(Exception e);
    }

    // Metoda za dobijanje korisnika po ID-u
    public void getUserById(String userId, UserCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("Korisnik nije pronađen"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Metoda za dobijanje UserProfile-a po ID-u
    public void getUserProfileById(String userId, UserProfileCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                        callback.onSuccess(userProfile);
                    } else {
                        // Ako UserProfile ne postoji, kreiraj default i snimi ga
                        Log.w(TAG, "UserProfile ne postoji za userId=" + userId + ", kreiram default...");
                        UserProfile defaultProfile = new UserProfile();

                        Map<String, Object> profilePayload = new HashMap<>();
                        profilePayload.put("level", defaultProfile.getLevel());
                        profilePayload.put("title", defaultProfile.getTitle());
                        profilePayload.put("xp", defaultProfile.getXp());
                        profilePayload.put("qrCode", defaultProfile.getQrCode());
                        profilePayload.put("currentEquipment", defaultProfile.getCurrentEquipment());
                        profilePayload.put("powerPoints", defaultProfile.getPowerPoints());
                        profilePayload.put("coins", defaultProfile.getCoins());
                        profilePayload.put("badges", defaultProfile.getBadges());
                        profilePayload.put("equipment", defaultProfile.getEquipment());

                        firestore.collection(USERS_COLLECTION)
                                .document(userId)
                                .collection(PROFILE_SUBCOLLECTION)
                                .document(userId)
                                .set(profilePayload)
                                .addOnSuccessListener(aVoid -> {
                                    Log.i(TAG, "Default UserProfile kreiran za userId=" + userId);
                                    callback.onSuccess(defaultProfile);
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Metoda za ažuriranje osnovnih User podataka
    public Task<Void> updateUser(User user) {
        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
            tcs.setException(new IllegalArgumentException("User or UID missing"));
            return tcs.getTask();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", user.getUid());
        payload.put("email", user.getEmail());
        payload.put("username", user.getUsername());
        payload.put("avatar", user.getAvatar());
        payload.put("isActivated", user.isActivated());

        return firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .update(payload);
    }

    // Metoda za ažuriranje UserProfile podataka
    public Task<Void> updateUserProfile(String userId, UserProfile userProfile) {
        if (userId == null || userId.isEmpty() || userProfile == null) {
            TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
            tcs.setException(new IllegalArgumentException("UserId or UserProfile missing"));
            return tcs.getTask();
        }

        Map<String, Object> profilePayload = new HashMap<>();
        profilePayload.put("level", userProfile.getLevel());
        profilePayload.put("title", userProfile.getTitle());
        profilePayload.put("xp", userProfile.getXp());
        profilePayload.put("qrCode", userProfile.getQrCode());
        profilePayload.put("currentEquipment", userProfile.getCurrentEquipment());
        profilePayload.put("powerPoints", userProfile.getPowerPoints());
        profilePayload.put("coins", userProfile.getCoins());
        profilePayload.put("badges", userProfile.getBadges());
        profilePayload.put("equipment", userProfile.getEquipment());

        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .update(profilePayload);
    }

    public void addXpToUser(String userId, int xpToAdd) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference profileRef = db.collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(profileRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Profil ne postoji!",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            long currentXp = snapshot.getLong("xp") != null ? snapshot.getLong("xp") : 0;
            long newXp = currentXp + xpToAdd;

            transaction.update(profileRef, "xp", newXp);

            // Transakcija mora da vrati null
            return null;
        }).addOnSuccessListener(aVoid ->
                Log.d("XP_SYSTEM", "✅ Transakcija za XP uspešna za korisnika " + userId)
        ).addOnFailureListener(e ->
                Log.e("XP_SYSTEM", "❌ Greška u transakciji za XP: ", e)
        );
    }

}
