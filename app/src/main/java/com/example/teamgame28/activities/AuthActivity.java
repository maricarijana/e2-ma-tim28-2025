package com.example.teamgame28.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.teamgame28.R;
import com.example.teamgame28.fragments.LoginFragment;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.auth_fragment_container, new LoginFragment())
                    .commit();
        }
    }
}
