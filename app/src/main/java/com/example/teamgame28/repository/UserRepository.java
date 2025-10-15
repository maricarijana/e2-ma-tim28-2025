package com.example.teamgame28.repository;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.teamgame28.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private static final String TAG = "UserRepo";
    private static final String USERS_COLLECTION = "app_users";
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", user.getUid());
        payload.put("email", user.getEmail());
        payload.put("username", user.getUsername());
        payload.put("avatar", user.getAvatarName());
        payload.put("isActivated", false);
        payload.put("createdAt", new Date());

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(payload)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Profile stored for uid=" + user.getUid());
                    // fetch reference to return
                    firestore.collection(USERS_COLLECTION)
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> tcs.setResult(documentSnapshot.getReference()))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore write failed: ", e);
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

}
