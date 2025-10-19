package com.example.teamgame28.repository;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Badge;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.PotionType;
import com.example.teamgame28.model.User;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.service.EquipmentService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
        profilePayload.put("ownedPotions", userProfile.getOwnedPotions());
        profilePayload.put("ownedClothing", userProfile.getOwnedClothing());
        profilePayload.put("ownedWeapons", userProfile.getOwnedWeapons());
        profilePayload.put("activePotions", userProfile.getActivePotions());
        profilePayload.put("activeClothing", userProfile.getActiveClothing());
        profilePayload.put("activeWeapons", userProfile.getActiveWeapons());
        profilePayload.put("activeDays", userProfile.getActiveDays());
        profilePayload.put("lastLoginTime", userProfile.getLastLoginTime());
        profilePayload.put("currentLevelStartTimestamp", userProfile.getCurrentLevelStartTimestamp());
        profilePayload.put("xpHistory", userProfile.getXpHistory());

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
                        profilePayload.put("ownedPotions", defaultProfile.getOwnedPotions());
                        profilePayload.put("ownedClothing", defaultProfile.getOwnedClothing());
                        profilePayload.put("ownedWeapons", defaultProfile.getOwnedWeapons());
                        profilePayload.put("activePotions", defaultProfile.getActivePotions());
                        profilePayload.put("activeClothing", defaultProfile.getActiveClothing());
                        profilePayload.put("activeWeapons", defaultProfile.getActiveWeapons());
                        profilePayload.put("activeDays", defaultProfile.getActiveDays());
                        profilePayload.put("lastLoginTime", defaultProfile.getLastLoginTime());
                        profilePayload.put("currentLevelStartTimestamp", defaultProfile.getCurrentLevelStartTimestamp());
                        profilePayload.put("xpHistory", defaultProfile.getXpHistory());

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
        profilePayload.put("ownedPotions", userProfile.getOwnedPotions());
        profilePayload.put("ownedClothing", userProfile.getOwnedClothing());
        profilePayload.put("ownedWeapons", userProfile.getOwnedWeapons());
        profilePayload.put("activePotions", userProfile.getActivePotions());
        profilePayload.put("activeClothing", userProfile.getActiveClothing());
        profilePayload.put("activeWeapons", userProfile.getActiveWeapons());
        profilePayload.put("activeDays", userProfile.getActiveDays());
        profilePayload.put("lastLoginTime", userProfile.getLastLoginTime());
        profilePayload.put("currentLevelStartTimestamp", userProfile.getCurrentLevelStartTimestamp());
        profilePayload.put("xpHistory", userProfile.getXpHistory());

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

        // Današnji datum u formatu "yyyy-MM-dd"
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(profileRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Profil ne postoji!",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // Ažuriraj ukupan XP
            long currentXp = snapshot.getLong("xp") != null ? snapshot.getLong("xp") : 0;
            long newXp = currentXp + xpToAdd;
            transaction.update(profileRef, "xp", newXp);

            // Ažuriraj XP istoriju za današnji dan
            Map<String, Object> xpHistory = (Map<String, Object>) snapshot.get("xpHistory");
            if (xpHistory == null) {
                xpHistory = new HashMap<>();
            }

            // Dodaj XP za današnji dan (kumulativno)
            long todayXp = 0;
            if (xpHistory.containsKey(today)) {
                Object todayValue = xpHistory.get(today);
                if (todayValue instanceof Long) {
                    todayXp = (Long) todayValue;
                } else if (todayValue instanceof Integer) {
                    todayXp = ((Integer) todayValue).longValue();
                }
            }
            xpHistory.put(today, todayXp + xpToAdd);

            transaction.update(profileRef, "xpHistory", xpHistory);

            // Transakcija mora da vrati null
            return null;
        }).addOnSuccessListener(aVoid ->
                Log.d("XP_SYSTEM", "✅ Transakcija za XP uspešna za korisnika " + userId + " (+" + xpToAdd + " XP)")
        ).addOnFailureListener(e ->
                Log.e("XP_SYSTEM", "❌ Greška u transakciji za XP: ", e)
        );
    }
    public void addCoinsToUser(String userId, int coinsToAdd, CoinsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference profileRef = db
                .collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        profileRef.update("coins", FieldValue.increment(coinsToAdd))
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepo", "✅ Coins added successfully: +" + coinsToAdd);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepo", "❌ Failed to add coins", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Overload za backward compatibility
    public void addCoinsToUser(String userId, int coinsToAdd) {
        addCoinsToUser(userId, coinsToAdd, null);
    }

    public interface CoinsCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void addEquipmentToUser(String userId, String newEquipment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ako oprema još nije implementirana, možeš hardcodovati
        if (newEquipment == null || newEquipment.isEmpty()) {
            newEquipment = "Basic Armor"; // ili "Wooden Sword"
        }

        DocumentReference profileRef = db
                .collection("app_users")
                .document(userId)
                .collection("profile")
                .document(userId);

        profileRef.update("equipment", FieldValue.arrayUnion(newEquipment))
                .addOnSuccessListener(aVoid ->
                        Log.d("UserRepo", "✅ Equipment added successfully"))
                .addOnFailureListener(e ->
                        Log.e("UserRepo", "❌ Failed to add equipment", e));
    }



    // Metoda za ažuriranje login streak-a
    public void updateLoginStreak(String userId) {
        final DocumentReference profileRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(profileRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Profil ne postoji!",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            long now = System.currentTimeMillis();
            Long lastLoginTime = snapshot.getLong("lastLoginTime");
            Long activeDays = snapshot.getLong("activeDays");

            if (lastLoginTime == null) lastLoginTime = 0L;
            if (activeDays == null) activeDays = 0L;

            // Izračunaj razliku u danima
            long oneDayInMillis = 24 * 60 * 60 * 1000;
            long daysSinceLastLogin = (now - lastLoginTime) / oneDayInMillis;

            if (lastLoginTime == 0) {
                // Prvi login
                activeDays = 1L;
                Log.d(TAG, "Prvi login za korisnika " + userId);
            } else if (daysSinceLastLogin >= 1 && daysSinceLastLogin < 2) {
                // Logovanje u narednom danu - povećaj streak
                activeDays++;
                Log.d(TAG, "Uzastopni login za korisnika " + userId + ", aktivni dani: " + activeDays);
            } else if (daysSinceLastLogin >= 2) {
                // Propušten je dan - resetuj streak
                activeDays = 1L;
                Log.d(TAG, "Streak resetovan za korisnika " + userId);
            } else {
                // Isti dan - ne menjaj streak
                Log.d(TAG, "Login u istom danu za korisnika " + userId);
            }

            transaction.update(profileRef, "activeDays", activeDays);
            transaction.update(profileRef, "lastLoginTime", now);

            return null;
        }).addOnSuccessListener(aVoid ->
                Log.d(TAG, "✅ Login streak ažuriran za korisnika " + userId)
        ).addOnFailureListener(e ->
                Log.e(TAG, "❌ Greška pri ažuriranju login streak-a: ", e)
        );
    }



}
