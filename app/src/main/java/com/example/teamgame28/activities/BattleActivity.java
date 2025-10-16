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
import com.example.teamgame28.service.BattleService;
import com.example.teamgame28.service.BossService;

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
    private boolean battleEnded = false;

    // Services
    private BattleService battleService;
    private BossService bossService;

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
        battleService = new BattleService(bossService);

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

        int bossLevel = intent.getIntExtra("BOSS_LEVEL", 1);
        currentBoss = battleService.createBoss(bossLevel, null);
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
        attackCounterText.setText(String.format("Attacks Remaining: %d / 5", attacksRemaining));
        equipmentText.setText("None equipped");
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

        BattleResult result = battleService.calculateRewards(currentBoss, attacksRemaining);

        if (result.isBossDefeated()) {
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

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(BattleActivity.this, RewardActivity.class);
            intent.putExtra("COINS_EARNED", result.getCoinsEarned());
            intent.putExtra("EQUIPMENT_DROPPED", result.isEquipmentDropped());
            intent.putExtra("IS_WEAPON", result.isWeapon());
            intent.putExtra("BOSS_DEFEATED", result.isBossDefeated());
            startActivity(intent);
            finish();
        }, 2000);
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
}
