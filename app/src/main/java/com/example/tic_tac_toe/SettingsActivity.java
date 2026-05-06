package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "tictactoe_prefs";
    private static final String KEY_VIBRATION = "pref_vibration";
    private static final String KEY_SOUND     = "pref_sound";
    private static final String KEY_TIMER     = "pref_timer";
    private static final String KEY_BEST      = "best_streak";
    private static final String KEY_STREAK    = "win_streak";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Button   btnBack        = findViewById(R.id.btnBack);
        Switch   switchVibration = findViewById(R.id.switchVibration);
        Switch   switchSound    = findViewById(R.id.switchSound);
        Switch   switchTimer    = findViewById(R.id.switchTimer);
        TextView tvBestStreak   = findViewById(R.id.tvBestStreak);
        Button   btnResetStreak = findViewById(R.id.btnResetStreak);

        // Load saved prefs (default all on)
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchTimer.setChecked(prefs.getBoolean(KEY_TIMER, true));
        tvBestStreak.setText(String.valueOf(prefs.getInt(KEY_BEST, 0)));

        switchVibration.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_VIBRATION, checked).apply());

        switchSound.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_SOUND, checked).apply());

        switchTimer.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_TIMER, checked).apply());

        btnResetStreak.setOnClickListener(v -> {
            prefs.edit().putInt(KEY_STREAK, 0).putInt(KEY_BEST, 0).apply();
            tvBestStreak.setText("0");
            Toast.makeText(this, "Win streak reset!", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
