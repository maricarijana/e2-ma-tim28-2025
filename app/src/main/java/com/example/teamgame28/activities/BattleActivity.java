package com.example.teamgame28.activities;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.teamgame28.R;
import com.example.teamgame28.model.BattleResult;
import com.example.teamgame28.model.Boss;
import com.example.teamgame28.repository.BossRepository;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.BattleService;
import com.example.teamgame28.service.BossService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BattleActivity extends AppCompatActivity implements SensorEventListener {

    // UI Components
    private ImageView bossImageView;
    private ProgressBar bossHPBar;
    private TextView bossHPText;
    private ProgressBar playerPPBar;
    private TextView playerPPText;
    private TextView equipmentText;
    private TextView successRateText;
    private TextView attackCounterText;
    private TextView battleMessageText;
    private Button attackButton;
    private LottieAnimationView bossHitAnimation;

    // Battle Data
    private Boss currentBoss;
    private int playerPP;
    private double successRate; // (0-100)
    private int attacksRemaining = 5;
    private int maxAttacks = 5;
    private String activeEquipment = "None";
    private boolean battleEnded = false;
    private boolean isExistingBoss = false; // Da li je boss već bio u bazi

    // Services
    private BattleService battleService;
    private BossService bossService;
    private com.example.teamgame28.service.EquipmentService equipmentService;

    // Sensor for shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_COOLDOWN = 1000; // 1 sekunda između napada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        // 🔹 Servisi
        bossService = new BossService(new BossRepository());
        battleService = new BattleService(bossService, this);
        equipmentService = new com.example.teamgame28.service.EquipmentService();

        // 🔹 UI
        initializeUI();

        // 🔹 Podaci iz intent-a
        getIntentData();

        // 🔹 Senzor za "shake to attack"
        initializeSensor();

        // 🔹 Podešavanje početnog UI
        setupBattle();

        // 🔹 Listeneri
        setupListeners();
    }

    private void initializeUI() {
        bossImageView = findViewById(R.id.bossImageView);
        bossHPBar = findViewById(R.id.bossHPBar);
        bossHPText = findViewById(R.id.bossHPText);
        playerPPBar = findViewById(R.id.playerPPBar);
        playerPPText = findViewById(R.id.playerPPText);
        equipmentText = findViewById(R.id.equipmentText);
        successRateText = findViewById(R.id.successRateText);
        attackCounterText = findViewById(R.id.attackCounterText);
        battleMessageText = findViewById(R.id.battleMessageText);
        attackButton = findViewById(R.id.attackButton);
        bossHitAnimation = findViewById(R.id.bossHitAnimation);
    }

    private void getIntentData() {
        Intent intent = getIntent();

        playerPP = intent.getIntExtra("PLAYER_PP", 50);
        successRate = intent.getDoubleExtra("SUCCESS_RATE", 67.0);
        maxAttacks = intent.getIntExtra("TOTAL_ATTACKS", 5);
        attacksRemaining = maxAttacks;
        activeEquipment = intent.getStringExtra("ACTIVE_EQUIPMENT");
        if (activeEquipment == null || activeEquipment.isEmpty()) {
            activeEquipment = "None";
        }

        // 🔥 Kreiraj Boss objekat iz Intent podataka
        String bossId = intent.getStringExtra("BOSS_ID");  // Firestore document ID
        int bossLevel = intent.getIntExtra("BOSS_LEVEL", 1);
        int bossHP = intent.getIntExtra("BOSS_HP", 200);
        int bossCurrentHP = intent.getIntExtra("BOSS_CURRENT_HP", 200);
        int bossCoinsReward = intent.getIntExtra("BOSS_COINS_REWARD", 200);
        isExistingBoss = intent.getBooleanExtra("IS_EXISTING_BOSS", false);

        currentBoss = new Boss();
        currentBoss.setId(bossId);  // Setuj Boss ID iz Firestore (null ako je novi boss)
        currentBoss.setBossLevel(bossLevel);
        currentBoss.setHp(bossHP);
        currentBoss.setCurrentHP(bossCurrentHP);
        currentBoss.setCoinsReward(bossCoinsReward);
        currentBoss.setDefeated(false);

        // Setuj userId za FireStore
        com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            currentBoss.setUserId(fbUser.getUid());
        }

        // 🔹 DEBUG: Log svih podataka iz intent-a
        android.util.Log.d("BattleActivity", "========== BATTLE DATA FROM INTENT ==========");
        android.util.Log.d("BattleActivity", "BOSS_ID: " + (bossId != null ? bossId : "null (new boss)"));
        android.util.Log.d("BattleActivity", "PLAYER_PP: " + playerPP);
        android.util.Log.d("BattleActivity", "SUCCESS_RATE: " + successRate);
        android.util.Log.d("BattleActivity", "TOTAL_ATTACKS: " + maxAttacks);
        android.util.Log.d("BattleActivity", "BOSS_LEVEL: " + bossLevel);
        android.util.Log.d("BattleActivity", "BOSS_HP: " + bossHP + " / " + bossCurrentHP);
        android.util.Log.d("BattleActivity", "BOSS_COINS: " + bossCoinsReward);
        android.util.Log.d("BattleActivity", isExistingBoss ? "🔴 UNDEFEATED BOSS" : "✅ NEW BOSS");
        android.util.Log.d("BattleActivity", "ACTIVE_EQUIPMENT: " + activeEquipment);
        android.util.Log.d("BattleActivity", "============================================");
    }

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void setupBattle() {
        updateUI();
    }

    private void setupListeners() {
        attackButton.setOnClickListener(v -> performAttack());
    }

    private void updateUI() {
        int maxHP = currentBoss.getHp();
        int currentHP = currentBoss.getCurrentHP();
        bossHPBar.setMax(maxHP);
        bossHPBar.setProgress(currentHP);
        bossHPText.setText(currentHP + " / " + maxHP);

        playerPPBar.setMax(100);
        playerPPBar.setProgress(playerPP);
        playerPPText.setText(playerPP + " PP");

        successRateText.setText(String.format("Attack Success Rate: %.0f%%", successRate));
        attackCounterText.setText(String.format("Attacks Remaining: %d / %d", attacksRemaining, maxAttacks));

        // Formatiran prikaz opreme
        if (activeEquipment != null && !activeEquipment.equals("None")) {
            String[] items = activeEquipment.split(", ");
            String formatted = "⚔️ " + String.join("\n⚔️ ", items) + " (" + items.length + " items)";
            equipmentText.setText(formatted);
            equipmentText.setTextColor(getColor(R.color.gold));
        } else {
            equipmentText.setText("⚠️ No equipment equipped");
            equipmentText.setTextColor(getColor(R.color.gray));
        }
    }

    private void performAttack() {
        if (battleEnded || attacksRemaining <= 0) return;

        attackButton.setEnabled(false);

        boolean hit = battleService.performAttack(currentBoss, playerPP, successRate);
        attacksRemaining--;

        if (hit) {
            showAttackSuccess();
        } else {
            showAttackMiss();
        }

        updateUI();

        if (currentBoss.getDefeated() || attacksRemaining <= 0) {
            endBattle();
        } else {
            new Handler().postDelayed(() -> attackButton.setEnabled(true), 1200);
        }
    }

    private void showAttackSuccess() {
        battleMessageText.setText("HIT! Dealt " + playerPP + " damage!");
        battleMessageText.setTextColor(Color.GREEN);

        bossImageView.setImageResource(R.drawable.boss_hit);

        bossHitAnimation.setVisibility(View.VISIBLE);
        bossHitAnimation.setAnimation(R.raw.boss_hit);
        bossHitAnimation.playAnimation();

        new Handler().postDelayed(() -> {
            bossHitAnimation.setVisibility(View.GONE);
            bossImageView.setImageResource(R.drawable.boss_idle);
        }, 800);
    }

    private void showAttackMiss() {
        battleMessageText.setText("MISS! Attack failed!");
        battleMessageText.setTextColor(Color.RED);

        bossImageView.animate()
                .translationX(15f)
                .setDuration(50)
                .withEndAction(() -> bossImageView.animate()
                        .translationX(-15f)
                        .setDuration(50)
                        .withEndAction(() -> bossImageView.animate()
                                .translationX(0f)
                                .setDuration(50)
                                .start())
                        .start())
                .start();
    }

    private void endBattle() {
        battleEnded = true;
        attackButton.setEnabled(false);

        android.util.Log.d("BattleActivity", "========== END BATTLE CALLED ==========");

        // Prikaži status poruku
        if (currentBoss.getDefeated()) {
            battleMessageText.setText("VICTORY! Boss defeated!");
            battleMessageText.setTextColor(Color.parseColor("#FFD700"));
        } else {
            double hpPercent = (double) currentBoss.getCurrentHP() / currentBoss.getHp();
            if (hpPercent <= 0.5) {
                battleMessageText.setText("Boss survived, but you dealt serious damage!");
                battleMessageText.setTextColor(Color.YELLOW);
            } else {
                battleMessageText.setText("Defeat! Try again next time.");
                battleMessageText.setTextColor(Color.RED);
            }
        }

        android.util.Log.d("BattleActivity", "🎲 Calling calculateRewards...");

        // 🔹 Izračunaj nagrade i ODMAH otvori RewardActivity
        battleService.calculateRewards(currentBoss, attacksRemaining, new BattleService.RewardsCallback() {
            @Override
            public void onSuccess(com.example.teamgame28.model.BattleResult result) {
                android.util.Log.d("BattleActivity", "✅ Rewards calculated - opening RewardActivity immediately");

                // ODMAH otvori RewardActivity
                openRewardActivity(result);

                // 🔹 U POZADINI uradi post-battle cleanup (potroši potions, smanji clothing duration)
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    android.util.Log.d("BattleActivity", "🧹 Starting background post-battle cleanup for userId: " + userId);

                    // 1️⃣ Sačuvaj/ažuriraj bosa u Firestore
                    saveBossToFirestore(userId);

                    // 2️⃣ Procesuj post-battle opremu
                    equipmentService.processPostBattle(userId, new com.example.teamgame28.service.EquipmentService.PostBattleCallback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("BattleActivity", "✅ Background post-battle cleanup SUCCESS");
                        }

                        @Override
                        public void onFailure(Exception e) {
                            android.util.Log.e("BattleActivity", "❌ Background post-battle cleanup FAILURE", e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("BattleActivity", "❌ Rewards callback FAILURE: " + e.getMessage(), e);
                // Otvori RewardActivity sa default vrednostima
                openRewardActivity(new com.example.teamgame28.model.BattleResult());
            }
        });
    }

    private void openRewardActivity(com.example.teamgame28.model.BattleResult result) {
        android.util.Log.d("BattleActivity", "========== OPENING REWARD ACTIVITY ==========");
        android.util.Log.d("BattleActivity", "Coins earned: " + result.getCoinsEarned());
        android.util.Log.d("BattleActivity", "Equipment dropped: " + result.isEquipmentDropped());
        android.util.Log.d("BattleActivity", "Is weapon: " + result.isWeapon());
        android.util.Log.d("BattleActivity", "Boss defeated: " + result.isBossDefeated());

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(BattleActivity.this, RewardActivity.class);
            intent.putExtra("COINS_EARNED", result.getCoinsEarned());
            intent.putExtra("EQUIPMENT_DROPPED", result.isEquipmentDropped());
            intent.putExtra("IS_WEAPON", result.isWeapon());
            intent.putExtra("BOSS_DEFEATED", result.isBossDefeated());
            intent.putExtra("EQUIPMENT_ID", result.getEquipmentId());
            intent.putExtra("EQUIPMENT_NAME", result.getEquipmentName());
            intent.putExtra("EQUIPMENT_IMAGE_RES_ID", result.getEquipmentImageResId());

            android.util.Log.d("BattleActivity", "🚀 Starting RewardActivity...");
            startActivity(intent);
            finish();
        }, 1000); // Skraćen delay jer već čekamo na asinkrone operacije
    }

    // ---------------- SENSOR LOGIKA ----------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            if (acceleration > 15 && (currentTime - lastShakeTime) > SHAKE_COOLDOWN) {
                lastShakeTime = currentTime;
                onShakeDetected();
            }
        }
    }

    private void onShakeDetected() {
        if (!battleEnded && attacksRemaining > 0 && attackButton.isEnabled()) {
            performAttack();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    /**
     * Čuva/ažurira bosa u Firestore nakon borbe.
     * LOGIKA:
     * - Ako je postojeći boss (već u bazi) → ažuriraj ga (HP ili defeated status)
     * - Ako je novi boss → nije potrebno čuvati jer je već sačuvan u getBossForBattle()
     */
    private void saveBossToFirestore(String userId) {
        currentBoss.setUserId(userId);

        if (currentBoss.getId() != null) {
            // ✅ Boss ima ID → znači da je već u Firestore, ažuriraj ga
            android.util.Log.d("BattleActivity", "🔄 Updating boss (ID: " + currentBoss.getId() + ") in Firestore...");
            android.util.Log.d("BattleActivity", "  - HP: " + currentBoss.getCurrentHP() + "/" + currentBoss.getHp());
            android.util.Log.d("BattleActivity", "  - Defeated: " + currentBoss.getDefeated());

            bossService.updateBossAfterBattle(currentBoss, new BossService.UpdateBossCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("BattleActivity", "✅ Boss updated successfully in Firestore");
                }

                @Override
                public void onFailure(String error) {
                    android.util.Log.e("BattleActivity", "❌ Failed to update boss: " + error);
                }
            });
        } else {
            // ℹ️ Boss nema ID → znači da nije sačuvan (što je bug, jer getBossForBattle() bi trebao da ga sačuva)
            android.util.Log.w("BattleActivity", "⚠️ Boss nema ID - mogući bug u getBossForBattle()!");
        }
    }
}
