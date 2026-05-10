package com.example.tic_tac_toe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends BaseGameActivity {

    private static final String PREFS_NAME    = "tictactoe_prefs";
    private static final String KEY_VIBRATION = "pref_vibration";
    private static final String KEY_SOUND     = "pref_sound";
    private static final String KEY_BGM       = "pref_bgm";
    private static final String KEY_TIMER     = "pref_timer";
    private static final String KEY_BEST      = "best_streak";
    private static final String KEY_STREAK    = "win_streak";
    private static final String KEY_USERNAME   = "last_username";
    private static final String KEY_BOARD_SIZE = "pref_board_size";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Button   btnBack          = findViewById(R.id.btnBack);
        Switch   switchVibration  = findViewById(R.id.switchVibration);
        Switch   switchSound      = findViewById(R.id.switchSound);
        Switch   switchBgm        = findViewById(R.id.switchBgm);
        Switch   switchTimer      = findViewById(R.id.switchTimer);
        TextView tvBestStreak     = findViewById(R.id.tvBestStreak);
        Button   btnResetStreak   = findViewById(R.id.btnResetStreak);
        TextView tvUsername       = findViewById(R.id.tvUsername);
        TextView tvUsernameAvatar = findViewById(R.id.tvUsernameAvatar);
        TextView btnEditUsername  = findViewById(R.id.btnEditUsername);

        // Загружаем username
        String username = prefs.getString(KEY_USERNAME, "Player");
        tvUsername.setText(username);
        tvUsernameAvatar.setText(username.substring(0, 1).toUpperCase());

        // Загружаем переключатели
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchBgm.setChecked(prefs.getBoolean(KEY_BGM, false));
        switchTimer.setChecked(prefs.getBoolean(KEY_TIMER, true));
        tvBestStreak.setText(String.valueOf(prefs.getInt(KEY_BEST, 0)));

        switchVibration.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_VIBRATION, checked).apply());
        switchSound.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_SOUND, checked).apply());
        switchBgm.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(KEY_BGM, checked).apply();
            MusicManager.get(this).setEnabled(checked); // сразу включает/выключает музыку
        });
        switchTimer.setOnCheckedChangeListener((b, checked) ->
            prefs.edit().putBoolean(KEY_TIMER, checked).apply());

        btnResetStreak.setOnClickListener(v -> {
            prefs.edit()
                .putInt(KEY_STREAK, 0).putInt(KEY_BEST, 0)
                .putInt("stats_easy_W", 0).putInt("stats_easy_L", 0).putInt("stats_easy_D", 0)
                .putInt("stats_normal_W", 0).putInt("stats_normal_L", 0).putInt("stats_normal_D", 0)
                .putInt("stats_hard_W", 0).putInt("stats_hard_L", 0).putInt("stats_hard_D", 0)
                .apply();
            tvBestStreak.setText("0");
            Toast.makeText(this, "Statistics reset!", Toast.LENGTH_SHORT).show();
        });

        btnEditUsername.setOnClickListener(v ->
            showEditUsernameDialog(prefs, tvUsername, tvUsernameAvatar));

        Button btnBoard3x3 = findViewById(R.id.btnBoard3x3);
        Button btnBoard4x4 = findViewById(R.id.btnBoard4x4);
        Button btnBoard5x5 = findViewById(R.id.btnBoard5x5);
        updateBoardSizeUi(prefs.getInt(KEY_BOARD_SIZE, 3), btnBoard3x3, btnBoard4x4, btnBoard5x5);
        btnBoard3x3.setOnClickListener(v -> saveBoardSize(prefs, 3, btnBoard3x3, btnBoard4x4, btnBoard5x5));
        btnBoard4x4.setOnClickListener(v -> saveBoardSize(prefs, 4, btnBoard3x3, btnBoard4x4, btnBoard5x5));
        btnBoard5x5.setOnClickListener(v -> saveBoardSize(prefs, 5, btnBoard3x3, btnBoard4x4, btnBoard5x5));

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        LinearLayout rowSupport = findViewById(R.id.rowSupport);
        rowSupport.setOnClickListener(v -> {
            startActivity(new Intent(this, SupportActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
    }

    private void saveBoardSize(SharedPreferences prefs, int size,
                               Button b3, Button b4, Button b5) {
        prefs.edit().putInt(KEY_BOARD_SIZE, size).apply();
        updateBoardSizeUi(size, b3, b4, b5);
    }

    private void updateBoardSizeUi(int selected, Button b3, Button b4, Button b5) {
        b3.setAlpha(selected == 3 ? 1f : 0.5f);
        b4.setAlpha(selected == 4 ? 1f : 0.5f);
        b5.setAlpha(selected == 5 ? 1f : 0.5f);
        b3.setScaleX(selected == 3 ? 1.08f : 1f); b3.setScaleY(selected == 3 ? 1.08f : 1f);
        b4.setScaleX(selected == 4 ? 1.08f : 1f); b4.setScaleY(selected == 4 ? 1.08f : 1f);
        b5.setScaleX(selected == 5 ? 1.08f : 1f); b5.setScaleY(selected == 5 ? 1.08f : 1f);
    }

    private void showEditUsernameDialog(SharedPreferences prefs,
                                        TextView tvUsername, TextView tvAvatar) {
        EditText input = new EditText(this);
        input.setHint("Enter username");
        input.setText(prefs.getString(KEY_USERNAME, "Player"));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
            .setTitle("Edit Username")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) return;
                if (name.length() > 20) name = name.substring(0, 20);
                prefs.edit().putString(KEY_USERNAME, name).apply();
                tvUsername.setText(name);
                tvAvatar.setText(name.substring(0, 1).toUpperCase());
                Toast.makeText(this, "Username saved!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
