package com.example.tic_tac_toe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseGameActivity {

    private static final String PREFS_NAME   = "tictactoe_prefs";
    private static final String KEY_USERNAME = "profile_username";
    private static final String KEY_AVATAR   = "profile_avatar";
    private static final String KEY_JOINED   = "profile_joined";
    private static final String KEY_BEST     = "best_streak";

    // Avatars: simple letter-based or short symbols (no emoji dependency)
    private static final String[] AVATARS = {
        "X", "O", "A", "B", "C", "D",
        "E", "F", "G", "H", "J", "K",
        "L", "M", "N", "P"
    };

    private TextView     tvAvatar, tvUsername, tvJoined, tvEmail;
    private LinearLayout tvGoogleBadge;   // now a LinearLayout in the new XML
    private TextView     tvWins, tvLosses, tvDraws, tvWinRate, tvStreak, tvTotal;
    private TextView     tvOnlineDesc, tvAchSummary;
    private Button       btnConnectGoogle;
    private SharedPreferences prefs;

    // Firebase
    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private String            uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureJoinDate();

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        tvAvatar       = findViewById(R.id.tvProfileAvatar);
        tvUsername     = findViewById(R.id.tvProfileUsername);
        tvJoined       = findViewById(R.id.tvProfileJoined);
        tvEmail        = findViewById(R.id.tvProfileEmail);
        tvGoogleBadge  = findViewById(R.id.tvGoogleBadge);
        tvWins         = findViewById(R.id.tvProfileWins);
        tvLosses       = findViewById(R.id.tvProfileLosses);
        tvDraws        = findViewById(R.id.tvProfileDraws);
        tvWinRate      = findViewById(R.id.tvProfileWinRate);
        tvStreak       = findViewById(R.id.tvProfileStreak);
        tvTotal        = findViewById(R.id.tvProfileTotal);
        tvOnlineDesc   = findViewById(R.id.tvOnlineDesc);
        tvAchSummary   = findViewById(R.id.tvAchSummary);
        btnConnectGoogle = findViewById(R.id.btnConnectGoogle);

        Button   btnBack        = findViewById(R.id.btnProfileBack);
        Button   btnEdit        = findViewById(R.id.btnProfileEdit);
        TextView tvChangeAvatar = findViewById(R.id.tvChangeAvatar);

        // Clickable LinearLayouts acting as buttons
        LinearLayout btnGoAchievements = findViewById(R.id.btnGoAchievements);
        LinearLayout btnGoSkins        = findViewById(R.id.btnGoSkins);
        LinearLayout btnSignOut        = findViewById(R.id.btnSignOut);

        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnEdit);
        GameButtonHelper.apply(btnConnectGoogle);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });
        btnEdit.setOnClickListener(v -> showEditUsernameDialog());
        tvAvatar.setOnClickListener(v -> showAvatarPicker());
        tvChangeAvatar.setOnClickListener(v -> showAvatarPicker());

        btnGoAchievements.setOnClickListener(v -> {
            startActivity(new Intent(this, AchievementsActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnGoSkins.setOnClickListener(v -> {
            startActivity(new Intent(this, SkinsActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });

        LinearLayout btnGoSupport = findViewById(R.id.btnGoSupport);
        if (btnGoSupport != null) {
            btnGoSupport.setOnClickListener(v -> {
                startActivity(new Intent(this, SupportActivity.class));
                overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
            });
        }

        btnSignOut.setOnClickListener(v -> showSignOutDialog());

        loadProfile();
        loadStats();
        updateAchievementSummary();
        checkFirebaseStatus();
    }

    // ── Achievement summary label ─────────────────────────────────────────────

    private void updateAchievementSummary() {
        AchievementManager am = AchievementManager.get(this);
        int unlocked = am.getUnlockedCount();
        int total    = am.getAll().size();
        if (tvAchSummary != null) {
            tvAchSummary.setText(getString(R.string.ach_summary, unlocked, total));
        }
    }

    // ── Firebase ──────────────────────────────────────────────────────────────

    private void checkFirebaseStatus() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            showGuestMode();
            return;
        }

        uid = user.getUid();

        if (!user.isAnonymous()) {
            // Google account user
            String googleName  = user.getDisplayName();
            String googleEmail = user.getEmail();

            String savedName = prefs.getString(KEY_USERNAME, "Player");
            if (savedName.equals("Player") && googleName != null && !googleName.isEmpty()) {
                prefs.edit().putString(KEY_USERNAME, googleName).apply();
                tvUsername.setText(googleName);
            }

            if (googleEmail != null && tvEmail != null) {
                tvEmail.setText(googleEmail);
                tvEmail.setVisibility(View.VISIBLE);
            }
            if (tvGoogleBadge != null) tvGoogleBadge.setVisibility(View.VISIBLE);

            if (tvOnlineDesc != null) {
                tvOnlineDesc.setText(R.string.cloud_connected_desc);
            }
            btnConnectGoogle.setText(R.string.sync_now);
            btnConnectGoogle.setOnClickListener(v -> {
                syncToFirestore();
                Toast.makeText(this, R.string.stats_synced, Toast.LENGTH_SHORT).show();
            });

            syncToFirestore();
            loadFromFirestore();

        } else {
            showGuestMode();
        }
    }

    private void showGuestMode() {
        btnConnectGoogle.setText(R.string.sync_to_cloud);
        btnConnectGoogle.setOnClickListener(v -> anonymousSync());
    }

    private void anonymousSync() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && uid != null) {
            syncToFirestore();
            Toast.makeText(this, R.string.stats_synced, Toast.LENGTH_SHORT).show();
            return;
        }
        btnConnectGoogle.setEnabled(false);
        btnConnectGoogle.setText(R.string.connecting);
        mAuth.signInAnonymously().addOnCompleteListener(task -> {
            btnConnectGoogle.setEnabled(true);
            if (task.isSuccessful()) {
                uid = mAuth.getCurrentUser().getUid();
                syncToFirestore();
                btnConnectGoogle.setText(R.string.synced_label);
                Toast.makeText(this, R.string.cloud_synced, Toast.LENGTH_SHORT).show();
            } else {
                btnConnectGoogle.setText(R.string.sync_to_cloud);
                Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncToFirestore() {
        if (uid == null || isFinishing() || isDestroyed()) return;

        int ew = prefs.getInt("stats_easy_W",0),   el = prefs.getInt("stats_easy_L",0),   ed = prefs.getInt("stats_easy_D",0);
        int nw = prefs.getInt("stats_normal_W",0), nl = prefs.getInt("stats_normal_L",0), nd = prefs.getInt("stats_normal_D",0);
        int hw = prefs.getInt("stats_hard_W",0),   hl = prefs.getInt("stats_hard_L",0),   hd = prefs.getInt("stats_hard_D",0);

        int wins   = ew + nw + hw;
        int losses = el + nl + hl;
        int draws  = ed + nd + hd;
        int streak = prefs.getInt(KEY_BEST, 0);

        FirebaseUser user = mAuth.getCurrentUser();

        Map<String, Object> data = new HashMap<>();
        data.put("username",   prefs.getString(KEY_USERNAME, "Player"));
        data.put("avatar",     prefs.getString(KEY_AVATAR, "X"));
        data.put("joinedDate", prefs.getString(KEY_JOINED, ""));
        data.put("wins",   wins);
        data.put("losses", losses);
        data.put("draws",  draws);
        data.put("bestStreak", streak);
        data.put("totalGames", wins + losses + draws);
        data.put("lastSynced", new Date().toString());

        if (user != null && !user.isAnonymous()) {
            if (user.getEmail()       != null) data.put("email",      user.getEmail());
            if (user.getDisplayName() != null) data.put("googleName", user.getDisplayName());
        }

        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFromFirestore() {
        if (uid == null || isFinishing() || isDestroyed()) return;
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                String username = doc.getString("username");
                String avatar   = doc.getString("avatar");

                if (username != null && !username.equals("Player")) {
                    prefs.edit().putString(KEY_USERNAME, username).apply();
                    tvUsername.setText(username);
                }
                if (avatar != null && !avatar.isEmpty()) {
                    prefs.edit().putString(KEY_AVATAR, avatar).apply();
                    tvAvatar.setText(avatar);
                }

                Long cloudWins   = doc.getLong("wins");
                Long cloudLosses = doc.getLong("losses");
                Long cloudDraws  = doc.getLong("draws");
                Long cloudStreak = doc.getLong("bestStreak");
                Long cloudTotal  = doc.getLong("totalGames");

                if (cloudWins == null) return;

                int localWins   = prefs.getInt("stats_easy_W", 0)
                    + prefs.getInt("stats_normal_W", 0)
                    + prefs.getInt("stats_hard_W", 0);
                int localLosses = prefs.getInt("stats_easy_L", 0)
                    + prefs.getInt("stats_normal_L", 0)
                    + prefs.getInt("stats_hard_L", 0);
                int localDraws  = prefs.getInt("stats_easy_D", 0)
                    + prefs.getInt("stats_normal_D", 0)
                    + prefs.getInt("stats_hard_D", 0);
                int localTotal  = localWins + localLosses + localDraws;

                // Use cloud data if it has more total games played (i.e. another device has more progress)
                if (cloudTotal != null && cloudTotal > localTotal) {
                    int cW = cloudWins.intValue();
                    int cL = cloudLosses != null ? cloudLosses.intValue() : 0;
                    int cD = cloudDraws  != null ? cloudDraws.intValue()  : 0;
                    int cS = cloudStreak != null ? cloudStreak.intValue() : 0;
                    prefs.edit()
                        .putInt("stats_normal_W", cW)
                        .putInt("stats_normal_L", cL)
                        .putInt("stats_normal_D", cD)
                        .putInt("stats_easy_W", 0)
                        .putInt("stats_easy_L", 0)
                        .putInt("stats_easy_D", 0)
                        .putInt("stats_hard_W", 0)
                        .putInt("stats_hard_L", 0)
                        .putInt("stats_hard_D", 0)
                        .putInt(KEY_BEST, Math.max(cS, prefs.getInt(KEY_BEST, 0)))
                        .apply();
                    loadStats();
                }
            });
    }

    // ── Local profile ─────────────────────────────────────────────────────────

    private void ensureJoinDate() {
        if (prefs.getString(KEY_JOINED, null) == null) {
            String date = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(new Date());
            prefs.edit().putString(KEY_JOINED, date).apply();
        }
    }

    private void loadProfile() {
        tvAvatar.setText(prefs.getString(KEY_AVATAR, "X"));
        tvUsername.setText(prefs.getString(KEY_USERNAME, "Player"));
        tvJoined.setText(getString(R.string.member_since, prefs.getString(KEY_JOINED, "")));
    }

    private void loadStats() {
        int ew = prefs.getInt("stats_easy_W",0),   el = prefs.getInt("stats_easy_L",0),   ed = prefs.getInt("stats_easy_D",0);
        int nw = prefs.getInt("stats_normal_W",0), nl = prefs.getInt("stats_normal_L",0), nd = prefs.getInt("stats_normal_D",0);
        int hw = prefs.getInt("stats_hard_W",0),   hl = prefs.getInt("stats_hard_L",0),   hd = prefs.getInt("stats_hard_D",0);

        int wins    = ew + nw + hw;
        int losses  = el + nl + hl;
        int draws   = ed + nd + hd;
        int total   = wins + losses + draws;
        int winRate = total == 0 ? 0 : Math.round(wins * 100f / total);
        int streak  = prefs.getInt(KEY_BEST, 0);

        tvWins.setText(String.valueOf(wins));
        tvLosses.setText(String.valueOf(losses));
        tvDraws.setText(String.valueOf(draws));
        tvWinRate.setText(winRate + "%");
        tvStreak.setText(String.valueOf(streak));
        tvTotal.setText(String.valueOf(total));
    }

    // ── Avatar picker ─────────────────────────────────────────────────────────

    private void showAvatarPicker() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        int padPx = dpToPx(16);
        grid.setPadding(padPx, padPx, padPx, padPx);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_avatar);
        builder.setView(grid);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.75f);
        }

        for (String letter : AVATARS) {
            TextView tv = new TextView(this);
            tv.setText(letter);
            tv.setTextSize(28f);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            tv.setBackground(getDrawable(R.drawable.bg_profile_avatar));

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width  = dpToPx(64);
            p.height = dpToPx(64);
            p.setMargins(8, 8, 8, 8);
            tv.setLayoutParams(p);
            tv.setOnClickListener(v -> {
                prefs.edit().putString(KEY_AVATAR, letter).apply();
                tvAvatar.setText(letter);
                dialog.dismiss();
            });
            grid.addView(tv);
        }
        dialog.show();
    }

    // ── Edit username ─────────────────────────────────────────────────────────

    private void showEditUsernameDialog() {
        EditText et = new EditText(this);
        et.setText(prefs.getString(KEY_USERNAME, "Player"));
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(16) });
        et.setSingleLine();
        et.setSelection(et.getText().length());
        int pad = dpToPx(20);
        et.setPadding(pad, pad / 2, pad, pad / 2);

        AlertDialog d = new AlertDialog.Builder(this)
            .setTitle(R.string.edit_username_title)
            .setView(et)
            .setPositiveButton(R.string.save, (dlg, w) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) name = "Player";
                prefs.edit().putString(KEY_USERNAME, name).apply();
                tvUsername.setText(name);
                if (uid != null) syncToFirestore();
            })
            .setNegativeButton("Cancel", null)
            .create();
        if (d.getWindow() != null) {
            d.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.75f);
        }
        d.show();
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    private void showSignOutDialog() {
        AlertDialog d = new AlertDialog.Builder(this)
            .setTitle(R.string.sign_out_title)
            .setMessage(R.string.sign_out_message)
            .setPositiveButton(R.string.sign_out_confirm, (dlg, w) -> doSignOut())
            .setNegativeButton(R.string.cancel, null)
            .create();
        if (d.getWindow() != null) {
            d.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.75f);
        }
        d.show();
    }

    private void doSignOut() {
        mAuth.signOut();
        uid = null;
        Toast.makeText(this, R.string.signed_out, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
