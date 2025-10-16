package com.example.teamgame28.activities;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.teamgame28.R;

public class RewardActivity extends AppCompatActivity implements SensorEventListener {

    // UI Components
    private TextView resultTitleText;
    private TextView resultMessageText;
    private ImageView chestClosedImage;
    private ImageView chestOpenImage;
    private LottieAnimationView chestOpenAnimation;
    private LottieAnimationView confettiAnimation;
    private TextView shakeInstructionText;
    private LinearLayout rewardsContainer;
    private LinearLayout coinsRewardLayout;
    private TextView coinsRewardText;
    private LinearLayout equipmentRewardLayout;
    private ImageView equipmentRewardImage;
    private TextView equipmentRewardText;
    private Button continueButton;

    // Reward Data
    private int coinsEarned;
    private boolean equipmentDropped;
    private boolean isWeapon;
    private boolean bossDefeated;
    private boolean chestOpened = false;

    // Sensor for shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_COOLDOWN = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        // Initialize UI
        initializeUI();

        // Get data from intent
        getIntentData();

        // Initialize sensor
        initializeSensor();

        // Setup UI based on results
        setupResults();

        // Setup listeners
        setupListeners();
    }

    private void initializeUI() {
        resultTitleText = findViewById(R.id.resultTitleText);
        resultMessageText = findViewById(R.id.resultMessageText);
        chestClosedImage = findViewById(R.id.chestClosedImage);
        chestOpenImage = findViewById(R.id.chestOpenImage);
        chestOpenAnimation = findViewById(R.id.chestOpenAnimation);
        confettiAnimation = findViewById(R.id.confettiAnimation);
        shakeInstructionText = findViewById(R.id.shakeInstructionText);
        rewardsContainer = findViewById(R.id.rewardsContainer);
        coinsRewardLayout = findViewById(R.id.coinsRewardLayout);
        coinsRewardText = findViewById(R.id.coinsRewardText);
        equipmentRewardLayout = findViewById(R.id.equipmentRewardLayout);
        equipmentRewardImage = findViewById(R.id.equipmentRewardImage);
        equipmentRewardText = findViewById(R.id.equipmentRewardText);
        continueButton = findViewById(R.id.continueButton);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        coinsEarned = intent.getIntExtra("COINS_EARNED", 0);
        equipmentDropped = intent.getBooleanExtra("EQUIPMENT_DROPPED", false);
        isWeapon = intent.getBooleanExtra("IS_WEAPON", false);
        bossDefeated = intent.getBooleanExtra("BOSS_DEFEATED", false);
    }

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void setupResults() {
        if (bossDefeated) {
            resultTitleText.setText("VICTORY!");
            resultTitleText.setTextColor(getResources().getColor(android.R.color.holo_orange_light, null));
            resultMessageText.setText("You have defeated the boss!");
        } else {
            if (coinsEarned > 0) {
                resultTitleText.setText("GOOD EFFORT!");
                resultTitleText.setTextColor(getResources().getColor(android.R.color.holo_orange_light, null));
                resultMessageText.setText("Boss survived, but you dealt significant damage!");
            } else {
                resultTitleText.setText("DEFEAT");
                resultTitleText.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
                resultMessageText.setText("The boss was too strong this time. Train harder!");

                // No rewards, skip chest
                if (coinsEarned == 0) {
                    shakeInstructionText.setVisibility(View.GONE);
                    chestClosedImage.setVisibility(View.GONE);
                    continueButton.setVisibility(View.VISIBLE);
                }
            }
        }

        // Prepare rewards display
        coinsRewardText.setText(coinsEarned + " Coins");

        if (equipmentDropped) {
            equipmentRewardLayout.setVisibility(View.VISIBLE);
            if (isWeapon) {
                equipmentRewardText.setText("New Weapon!");
                // TODO: Set weapon icon
            } else {
                equipmentRewardText.setText("New Armor!");
                // TODO: Set armor icon
            }
        }
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> {
            // Return to main activity
            finish();
        });
    }

    private void openChest() {
        if (chestOpened) return;
        chestOpened = true;

        // Hide instruction
        shakeInstructionText.setVisibility(View.GONE);

        // Hide closed chest
        chestClosedImage.setVisibility(View.GONE);

        // Show chest opening animation
        chestOpenAnimation.setVisibility(View.VISIBLE);
        chestOpenAnimation.setAnimation(R.raw.chest_open);
        chestOpenAnimation.playAnimation();

        // After animation, show open chest and rewards
        new Handler().postDelayed(() -> {
            chestOpenAnimation.setVisibility(View.GONE);
            chestOpenImage.setVisibility(View.VISIBLE);

            // Show confetti
            confettiAnimation.setVisibility(View.VISIBLE);
            confettiAnimation.setAnimation(R.raw.confetti);
            confettiAnimation.playAnimation();

            // Show rewards
            rewardsContainer.setVisibility(View.VISIBLE);

            // Animate coins appearing
            coinsRewardLayout.setAlpha(0f);
            coinsRewardLayout.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();

            // Animate equipment appearing if dropped
            if (equipmentDropped) {
                equipmentRewardLayout.setAlpha(0f);
                new Handler().postDelayed(() -> {
                    equipmentRewardLayout.setAlpha(0f);
                    equipmentRewardLayout.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .start();
                }, 300);
            }

            // Show continue button
            new Handler().postDelayed(() -> {
                continueButton.setVisibility(View.VISIBLE);
                continueButton.setAlpha(0f);
                continueButton.animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start();
            }, 1000);

            // Hide confetti after animation
            new Handler().postDelayed(() -> {
                confettiAnimation.setVisibility(View.GONE);
            }, 3000);

        }, 1000); // Wait for chest opening animation
    }

    // Sensor methods for shake detection
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float acceleration = x * x + y * y + z * z;
            float normalizedAcceleration = acceleration / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

            long currentTime = System.currentTimeMillis();

            if (normalizedAcceleration > 3 && (currentTime - lastShakeTime) > SHAKE_COOLDOWN) {
                lastShakeTime = currentTime;
                onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    private void onShakeDetected() {
        if (!chestOpened && coinsEarned > 0) {
            openChest();
        }
    }

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
