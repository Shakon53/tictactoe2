package com.example.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout heroCard = findViewById(R.id.heroCard);
        ImageView ivGameLogo = findViewById(R.id.ivGameLogo);
        Button btnSinglePlayer = findViewById(R.id.btnSinglePlayer);
        Button btnMultiplayer  = findViewById(R.id.btnMultiplayer);
        Button btnSettings     = findViewById(R.id.btnSettings);
        GameButtonHelper.applyAll(btnSinglePlayer, btnMultiplayer, btnSettings);
        playEntrance(heroCard, ivGameLogo, btnSinglePlayer, btnMultiplayer, btnSettings);

        btnSinglePlayer.setOnClickListener(v -> {
            startActivity(new Intent(this, SinglePlayerActivity.class));
        });

        btnMultiplayer.setOnClickListener(v -> {
            startActivity(new Intent(this, MultiplayerActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private void playEntrance(View heroCard, View logo, View... buttons) {
        heroCard.setAlpha(0f);
        heroCard.setTranslationY(70f);
        heroCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(480)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .start();

        logo.setScaleX(0.75f);
        logo.setScaleY(0.75f);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(450).start();

        for (int i = 0; i < buttons.length; i++) {
            View button = buttons[i];
            button.setAlpha(0f);
            button.setTranslationY(36f);
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320)
                .setStartDelay(140L + i * 80L)
                .start();
        }
    }

}
