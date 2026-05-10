package com.example.tic_tac_toe;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends BaseGameActivity {

    private static final String PREFS_NAME = "tictactoe_prefs";
    private TextView tvMainWins, tvMainDraws, tvMainLosses;

    // Track which buttons are pressed (to pause idle anim)
    private final boolean[] isPressed = new boolean[4];

    // Idle ObjectAnimators per button (glow alpha, glow scaleY, btn scaleX, btn scaleY)
    private final ObjectAnimator[][] idleAnims = new ObjectAnimator[4][4];

    // Title animators (kept so we can cancel on destroy)
    private ValueAnimator titleColorAnim;
    private ObjectAnimator titleFloatAnim;
    private AnimatorSet    onlinePulseSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView     tvTitleMain     = findViewById(R.id.tvTitleMain);
        TextView     tvTitleOnline   = findViewById(R.id.tvTitleOnline);
        LinearLayout heroCard        = findViewById(R.id.heroCard);
        ImageView    ivGameLogo      = findViewById(R.id.ivGameLogo);
        Button       btnSinglePlayer = findViewById(R.id.btnSinglePlayer);
        Button       btnMultiplayer  = findViewById(R.id.btnMultiplayer);
        Button       btnSettings     = findViewById(R.id.btnSettings);
        Button       btnMoreGames    = findViewById(R.id.btnMoreGames);
        Button       btnProfile      = findViewById(R.id.btnProfile);
        Button       btnAchievements = findViewById(R.id.btnAchievements);
        Button       btnSkins        = findViewById(R.id.btnSkins);
        View         glowSingle      = findViewById(R.id.glowSinglePlayer);
        View         glowMulti       = findViewById(R.id.glowMultiplayer);
        View         glowSett        = findViewById(R.id.glowSettings);
        View         glowMoreGames   = findViewById(R.id.glowMoreGames);
        View         glowProfile     = findViewById(R.id.glowProfile);
        tvMainWins   = findViewById(R.id.tvMainWins);
        tvMainDraws  = findViewById(R.id.tvMainDraws);
        tvMainLosses = findViewById(R.id.tvMainLosses);

        View[] btns  = {btnSinglePlayer, btnMultiplayer, btnSettings, btnMoreGames};
        View[] glows = {glowSingle,      glowMulti,      glowSett,    glowMoreGames};

        GameButtonHelper.applyAll(btnSinglePlayer, btnMultiplayer, btnSettings,
            btnMoreGames, btnProfile, btnAchievements, btnSkins);

        // Show onboarding on first ever launch
        if (OnboardingActivity.shouldShow(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        // Touch glow + start idle animations per button
        for (int i = 0; i < btns.length; i++) {
            applyNeonGlow(btns[i], glows[i], i);
        }

        playEntrance(heroCard, ivGameLogo, btnSinglePlayer, btnMultiplayer, btnSettings, btnMoreGames, btnProfile);

        // Start idle "living" animations after entrance finishes (~1s)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startIdleAnimations(btns, glows);
            startTitleAnimations(tvTitleMain, tvTitleOnline, heroCard);
        }, 950);

        // Clicks
        btnSinglePlayer.setOnClickListener(v -> {
            startActivity(new Intent(this, SinglePlayerActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnMultiplayer.setOnClickListener(v -> {
            startActivity(new Intent(this, MultiplayerActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnMoreGames.setOnClickListener(v -> showMoreGamesDialog());
        btnAchievements.setOnClickListener(v -> {
            startActivity(new Intent(this, AchievementsActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnSkins.setOnClickListener(v -> {
            startActivity(new Intent(this, SkinsActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    // ─── Idle "living" animations ─────────────────────────────────────────────

    private void startIdleAnimations(View[] buttons, View[] glows) {
        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            View btn  = buttons[i];
            View glow = glows[i];
            long delay = i * 320L;

            // Glow alpha: pulse 0.05 ↔ 0.50
            ObjectAnimator alpha = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.05f, 0.50f);
            alpha.setDuration(1100 + i * 100L);
            alpha.setStartDelay(delay);
            alpha.setRepeatMode(ValueAnimator.REVERSE);
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            alpha.setInterpolator(new AccelerateDecelerateInterpolator());
            alpha.start();

            // Glow scaleY: expand/contract vertically
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 0.85f, 1.55f);
            scaleY.setDuration(1100 + i * 100L);
            scaleY.setStartDelay(delay);
            scaleY.setRepeatMode(ValueAnimator.REVERSE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.start();

            // Button breathing scaleX
            ObjectAnimator breathX = ObjectAnimator.ofFloat(btn, View.SCALE_X, 1.0f, 1.016f);
            breathX.setDuration(1800 + i * 120L);
            breathX.setStartDelay(delay + 400);
            breathX.setRepeatMode(ValueAnimator.REVERSE);
            breathX.setRepeatCount(ValueAnimator.INFINITE);
            breathX.setInterpolator(new AccelerateDecelerateInterpolator());
            breathX.start();

            // Button breathing scaleY
            ObjectAnimator breathY = ObjectAnimator.ofFloat(btn, View.SCALE_Y, 1.0f, 1.016f);
            breathY.setDuration(1800 + i * 120L);
            breathY.setStartDelay(delay + 400);
            breathY.setRepeatMode(ValueAnimator.REVERSE);
            breathY.setRepeatCount(ValueAnimator.INFINITE);
            breathY.setInterpolator(new AccelerateDecelerateInterpolator());
            breathY.start();

            idleAnims[i] = new ObjectAnimator[]{alpha, scaleY, breathX, breathY};

            // Shimmer sweep: light wave across button
            addShimmer(btn, delay + 800 + i * 600L);
        }
    }

    // ─── Title "living" animations ────────────────────────────────────────────

    private void startTitleAnimations(TextView tvMain, TextView tvOnline, LinearLayout heroCard) {

        // 1) "TIC TAC TOE" — color shimmer cycling: white → neon purple → neon pink → white
        int[] colors = {
            Color.WHITE,
            Color.parseColor("#C084FC"),   // soft purple
            Color.parseColor("#FF79C6"),   // neon pink
            Color.parseColor("#A78BFA"),   // violet
            Color.WHITE
        };
        titleColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), (Object[]) new Integer[]{
            colors[0], colors[1], colors[2], colors[3], colors[4]
        });
        titleColorAnim.setDuration(3200);
        titleColorAnim.setRepeatMode(ValueAnimator.REVERSE);
        titleColorAnim.setRepeatCount(ValueAnimator.INFINITE);
        titleColorAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        titleColorAnim.addUpdateListener(a -> tvMain.setTextColor((int) a.getAnimatedValue()));
        titleColorAnim.start();

        // 2) "TIC TAC TOE" container — gentle vertical float: 0 → -8dp → 0
        float floatDp = dpToPx(7);
        titleFloatAnim = ObjectAnimator.ofFloat(heroCard, View.TRANSLATION_Y, 0f, -floatDp, 0f);
        titleFloatAnim.setDuration(2800);
        titleFloatAnim.setRepeatMode(ValueAnimator.REVERSE);
        titleFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
        titleFloatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        titleFloatAnim.start();

        // 3) "ONLINE" — scale pulse: 1.0 → 1.12 → 1.0, slightly offset from float
        ObjectAnimator onlineSX = ObjectAnimator.ofFloat(tvOnline, View.SCALE_X, 1.0f, 1.13f);
        ObjectAnimator onlineSY = ObjectAnimator.ofFloat(tvOnline, View.SCALE_Y, 1.0f, 1.13f);
        for (ObjectAnimator a : new ObjectAnimator[]{onlineSX, onlineSY}) {
            a.setDuration(1400);
            a.setStartDelay(300);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
        }
        onlinePulseSet = new AnimatorSet();
        onlinePulseSet.playTogether(onlineSX, onlineSY);
        onlinePulseSet.start();

        // 4) "ONLINE" — letter spacing breathe: 0.15 → 0.22 → 0.15
        ValueAnimator spacingAnim = ValueAnimator.ofFloat(0.15f, 0.24f);
        spacingAnim.setDuration(1400);
        spacingAnim.setStartDelay(300);
        spacingAnim.setRepeatMode(ValueAnimator.REVERSE);
        spacingAnim.setRepeatCount(ValueAnimator.INFINITE);
        spacingAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        spacingAnim.addUpdateListener(a -> tvOnline.setLetterSpacing((float) a.getAnimatedValue()));
        spacingAnim.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (titleColorAnim != null) titleColorAnim.cancel();
        if (titleFloatAnim != null) titleFloatAnim.cancel();
        if (onlinePulseSet != null) onlinePulseSet.cancel();
    }

    /** Adds a semi-transparent white gradient that sweeps across the button periodically */
    private void addShimmer(View button, long startDelay) {
        if (!(button.getParent() instanceof FrameLayout)) return;
        FrameLayout container = (FrameLayout) button.getParent();

        View shimmer = new View(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            dpToPx(90), dpToPx(60));
        lp.gravity = Gravity.CENTER_VERTICAL;
        shimmer.setLayoutParams(lp);

        int[] colors = {0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF};
        GradientDrawable grad = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, colors);
        shimmer.setBackground(grad);
        shimmer.setAlpha(0f);
        container.addView(shimmer);

        float btnWidth = getResources().getDisplayMetrics().widthPixels;

        ValueAnimator sweep = ValueAnimator.ofFloat(-dpToPx(100), btnWidth + dpToPx(100));
        sweep.setDuration(1400);
        sweep.setStartDelay(startDelay);
        sweep.setRepeatCount(ValueAnimator.INFINITE);
        sweep.setRepeatMode(ValueAnimator.RESTART);
        // Long pause between sweeps: total cycle = duration + repeatDelay trick via delay in listener
        sweep.addUpdateListener(anim -> {
            float x = (float) anim.getAnimatedValue();
            float fraction = anim.getAnimatedFraction();
            shimmer.setTranslationX(x);
            // Fade in at start, full in middle, fade out at end
            float a;
            if (fraction < 0.12f)      a = fraction / 0.12f;
            else if (fraction > 0.88f) a = (1f - fraction) / 0.12f;
            else                        a = 1f;
            shimmer.setAlpha(a * 0.65f);
        });
        // Re-trigger every ~3.5s using handler
        Runnable[] runRef = new Runnable[1];
        runRef[0] = () -> {
            sweep.start();
            new Handler(Looper.getMainLooper()).postDelayed(runRef[0], 3500);
        };
        new Handler(Looper.getMainLooper()).postDelayed(runRef[0], startDelay);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ─── Touch glow ──────────────────────────────────────────────────────────

    private void applyNeonGlow(View button, View glowView, int index) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isPressed[index] = true;
                    // Pause idle animations
                    if (idleAnims[index] != null)
                        for (ObjectAnimator a : idleAnims[index]) a.pause();
                    // Touch press effect
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    glowView.animate().alpha(1f).scaleX(1.05f).scaleY(1.7f).setDuration(110).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isPressed[index] = false;
                    // Touch release
                    v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(280).setInterpolator(new OvershootInterpolator(2.2f)).start();
                    glowView.animate().alpha(0.05f).scaleX(1f).scaleY(1f).setDuration(400)
                        .withEndAction(() -> {
                            // Resume idle animations
                            if (idleAnims[index] != null)
                                for (ObjectAnimator a : idleAnims[index]) a.resume();
                        }).start();
                    break;
            }
            return false;
        });
    }

    // ─── Entrance animation ───────────────────────────────────────────────────

    private void playEntrance(View heroCard, View logo, View... buttons) {
        heroCard.setAlpha(0f);
        heroCard.setTranslationY(70f);
        heroCard.animate().alpha(1f).translationY(0f).setDuration(480)
            .setInterpolator(new OvershootInterpolator(1.1f)).start();

        logo.setScaleX(0.75f); logo.setScaleY(0.75f);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(450).start();

        for (int i = 0; i < buttons.length; i++) {
            View b = buttons[i];
            b.setAlpha(0f); b.setTranslationY(36f);
            b.animate().alpha(1f).translationY(0f).setDuration(320)
                .setStartDelay(140L + i * 80L).start();
        }
    }

    // ─── More Games dialog ────────────────────────────────────────────────────

    private void showMoreGamesDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_more_games);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.75f);
        }

        Button btnPlayMemory = dialog.findViewById(R.id.btnPlayMemory);
        Button btnPlaySnake  = dialog.findViewById(R.id.btnPlaySnake);
        Button btnPlay2048   = dialog.findViewById(R.id.btnPlay2048);
        Button btnPlaySudoku = dialog.findViewById(R.id.btnPlaySudoku);
        Button btnClose      = dialog.findViewById(R.id.btnMoreGamesClose);

        GameButtonHelper.applyAll(btnPlayMemory, btnPlaySnake, btnPlay2048, btnPlaySudoku, btnClose);
        btnPlayMemory.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MemoryMatchActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnPlaySnake.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SnakeActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnPlay2048.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, Game2048Activity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnPlaySudoku.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SudokuActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View dv = dialog.getWindow().getDecorView();
        dv.setScaleX(0.85f); dv.setScaleY(0.85f); dv.setAlpha(0f);
        dv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300)
            .setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int wins   = prefs.getInt("stats_easy_W", 0) + prefs.getInt("stats_normal_W", 0) + prefs.getInt("stats_hard_W", 0);
        int losses = prefs.getInt("stats_easy_L", 0) + prefs.getInt("stats_normal_L", 0) + prefs.getInt("stats_hard_L", 0);
        int draws  = prefs.getInt("stats_easy_D", 0) + prefs.getInt("stats_normal_D", 0) + prefs.getInt("stats_hard_D", 0);
        tvMainWins.setText(String.valueOf(wins));
        tvMainDraws.setText(String.valueOf(draws));
        tvMainLosses.setText(String.valueOf(losses));
    }
}
