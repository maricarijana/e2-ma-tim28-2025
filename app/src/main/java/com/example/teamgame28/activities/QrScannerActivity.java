package com.example.teamgame28.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.teamgame28.repository.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QrScannerActivity extends AppCompatActivity {

    private FriendRepository friendRepository;
    private FirebaseAuth auth;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicijalizacija Firebase i Repository
        auth = FirebaseAuth.getInstance();
        friendRepository = new FriendRepository();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            startQrScanner();
        } else {
            Toast.makeText(this, "❌ Niste prijavljeni", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Skeniraj QR kod prijatelja");
        integrator.setCameraId(0); // Back camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                // Korisnik je otkazao skeniranje
                Toast.makeText(this, "❌ Skeniranje otkazano", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Uspešno skeniran QR kod
                String scannedUserId = result.getContents();
                sendFriendRequest(scannedUserId);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendFriendRequest(String scannedUserId) {
        friendRepository.sendFriendRequest(currentUserId, scannedUserId, new FriendRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(QrScannerActivity.this, "✅ Zahtev poslat!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(QrScannerActivity.this, "❌ Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
