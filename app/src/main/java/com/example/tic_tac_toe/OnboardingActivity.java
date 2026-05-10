package com.example.tic_tac_toe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS      = "tictactoe_prefs";
    private static final String KEY_SHOWN  = "onboarding_shown";

    private static final int PAGE_COUNT = 4;

    private static final String[] EMOJIS = { "🎮", "✕", "🤖", "🌐" };

    private int[] titleIds = {
        R.string.onboarding_title_1,
        R.string.onboarding_title_2,
        R.string.onboarding_title_3,
        R.string.onboarding_title_4
    };

    private int[] descIds = {
        R.string.onboarding_desc_1,
        R.string.onboarding_desc_2,
        R.string.onboarding_desc_3,
        R.string.onboarding_desc_4
    };

    private int currentPage = 0;

    private TextView tvEmoji, tvTitle, tvDesc;
    private Button   btnNext;
    private TextView tvSkip;
    private View[]   dots;

    public static boolean shouldShow(Context ctx) {
        return !ctx.getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE)
                   .getBoolean(KEY_SHOWN, false);
    }

    public static void markShown(Context ctx) {
        ctx.getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE)
           .edit().putBoolean(KEY_SHOWN, true).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        tvEmoji = findViewById(R.id.tvOnboardingEmoji);
        tvTitle = findViewById(R.id.tvOnboardingTitle);
        tvDesc  = findViewById(R.id.tvOnboardingDesc);
        btnNext = findViewById(R.id.btnOnboardingNext);
        tvSkip  = findViewById(R.id.tvOnboardingSkip);

        dots = new View[]{
            findViewById(R.id.dot0),
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        };

        // Entrance animation
        View card = findViewById(R.id.onboardingCard);
        card.setAlpha(0f);
        card.setTranslationY(60f);
        card.animate().alpha(1f).translationY(0f)
            .setDuration(500)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .setStartDelay(150)
            .start();

        updatePage(false);

        btnNext.setOnClickListener(v -> {
            if (currentPage < PAGE_COUNT - 1) {
                currentPage++;
                updatePage(true);
            } else {
                finish();
            }
        });

        tvSkip.setOnClickListener(v -> finish());
    }

    private void updatePage(boolean animate) {
        if (animate) {
            View card = findViewById(R.id.onboardingCard);
            card.animate().alpha(0f).translationX(-40f).setDuration(150).withEndAction(() -> {
                applyPageContent();
                card.setTranslationX(60f);
                card.animate().alpha(1f).translationX(0f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            }).start();
        } else {
            applyPageContent();
        }
    }

    private void applyPageContent() {
        tvEmoji.setText(EMOJIS[currentPage]);
        tvTitle.setText(titleIds[currentPage]);
        tvDesc.setText(descIds[currentPage]);

        boolean isLast = currentPage == PAGE_COUNT - 1;
        btnNext.setText(isLast ? R.string.onboarding_start : R.string.onboarding_next);
        tvSkip.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

        for (int i = 0; i < dots.length; i++) {
            dots[i].setAlpha(i == currentPage ? 1f : 0.3f);
            float scale = i == currentPage ? 1.3f : 1f;
            dots[i].setScaleX(scale);
            dots[i].setScaleY(scale);
        }
    }

    @Override
    public void finish() {
        markShown(this);
        super.finish();
        overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
    }
}
