package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class AchievementsActivity extends BaseGameActivity {

    private LinearLayout llList;
    private ProgressBar  pbProgress;
    private TextView     tvCount, tvProgressLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        llList          = findViewById(R.id.llAchievementList);
        pbProgress      = findViewById(R.id.pbAchProgress);
        tvCount         = findViewById(R.id.tvAchCount);
        tvProgressLabel = findViewById(R.id.tvAchProgressLabel);

        Button btnBack = findViewById(R.id.btnAchBack);
        GameButtonHelper.apply(btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        buildList();
    }

    private void buildList() {
        AchievementManager am   = AchievementManager.get(this);
        List<Achievement>  list = am.getAll();

        int unlocked = am.getUnlockedCount();
        int total    = list.size();

        tvCount.setText(unlocked + " / " + total);
        pbProgress.setMax(total);
        pbProgress.setProgress(unlocked);
        tvProgressLabel.setText(unlocked + " of " + total + " achievements unlocked");

        llList.removeAllViews();

        // ── Total wins hint ──────────────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE);
        int totalWins = prefs.getInt("stats_easy_W", 0)
                      + prefs.getInt("stats_normal_W", 0)
                      + prefs.getInt("stats_hard_W", 0);
        addWinsHint(totalWins);

        // ── Unlocked section ─────────────────────────────────────────────────
        addSectionHeader("UNLOCKED", "#10B981");
        boolean anyUnlocked = false;
        for (Achievement a : list) {
            if (a.unlocked) { addItem(a); anyUnlocked = true; }
        }
        if (!anyUnlocked) addEmptyNote("No achievements yet — keep playing!");

        // ── Locked section ───────────────────────────────────────────────────
        addSectionHeader("IN PROGRESS", "#FBCE5C");
        for (Achievement a : list) {
            if (!a.unlocked) addItem(a);
        }
    }

    // ── Section header ────────────────────────────────────────────────────────

    private void addSectionHeader(String text, String hexColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, dp(20), 0, dp(10));
        row.setLayoutParams(rLp);

        // Left line
        View line1 = new View(this);
        LinearLayout.LayoutParams l1Lp = new LinearLayout.LayoutParams(0, dp(1), 1f);
        line1.setLayoutParams(l1Lp);
        line1.setBackgroundColor(Color.parseColor(hexColor + "55"));
        row.addView(line1);

        TextView tv = new TextView(this);
        tv.setText("  " + text + "  ");
        tv.setTextColor(Color.parseColor(hexColor));
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.15f);
        row.addView(tv);

        // Right line
        View line2 = new View(this);
        LinearLayout.LayoutParams l2Lp = new LinearLayout.LayoutParams(0, dp(1), 1f);
        line2.setLayoutParams(l2Lp);
        line2.setBackgroundColor(Color.parseColor(hexColor + "55"));
        row.addView(line2);

        llList.addView(row);
    }

    // ── Wins hint banner ──────────────────────────────────────────────────────

    private void addWinsHint(int totalWins) {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, 0, 0, dp(8));
        banner.setLayoutParams(bLp);

        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(0x22FBCE5C);
        bannerBg.setCornerRadius(dp(12));
        bannerBg.setStroke(dp(1), 0x44FBCE5C);
        banner.setBackground(bannerBg);

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        ivLp.setMarginEnd(dp(10));
        iv.setLayoutParams(ivLp);
        iv.setImageResource(R.drawable.ic_ach_trophy);
        banner.addView(iv);

        TextView tv = new TextView(this);
        tv.setText("Total wins: " + totalWins + "  •  Play games to unlock new achievements!");
        tv.setTextColor(Color.parseColor("#FBCE5C"));
        tv.setTextSize(11);
        banner.addView(tv);

        llList.addView(banner);
    }

    private void addEmptyNote(String msg) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        tv.setLayoutParams(lp);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#4A3A6A"));
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(12), 0, dp(12));
        llList.addView(tv);
    }

    // ── Achievement item ──────────────────────────────────────────────────────

    private void addItem(Achievement a) {
        // Card container
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardLp);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(getDrawable(R.drawable.bg_glass_card));

        // ── Icon + badge ─────────────────────────────────────────────────
        FrameLayout iconFrame = new FrameLayout(this);
        LinearLayout.LayoutParams ifLp = new LinearLayout.LayoutParams(dp(56), dp(56));
        ifLp.setMarginEnd(dp(14));
        iconFrame.setLayoutParams(ifLp);

        // Icon circle background
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(a.unlocked ? 0x338B5CF6 : 0x222A1A4A);
        circleBg.setStroke(dp(1), a.unlocked ? 0xFF8B5CF6 : 0x44FFFFFF);
        iconFrame.setBackground(circleBg);

        ImageView ivIcon = new ImageView(this);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(30), dp(30));
        iconLp.gravity = Gravity.CENTER;
        ivIcon.setLayoutParams(iconLp);
        ivIcon.setImageResource(a.iconRes);
        ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (!a.unlocked) ivIcon.setAlpha(0.45f);
        iconFrame.addView(ivIcon);

        // Lock badge (small, bottom-right of icon circle)
        if (!a.unlocked) {
            FrameLayout lockBadge = new FrameLayout(this);
            FrameLayout.LayoutParams lbLp = new FrameLayout.LayoutParams(dp(18), dp(18));
            lbLp.gravity = Gravity.BOTTOM | Gravity.END;
            lockBadge.setLayoutParams(lbLp);

            GradientDrawable lbBg = new GradientDrawable();
            lbBg.setShape(GradientDrawable.OVAL);
            lbBg.setColor(Color.parseColor("#1A0A2A"));
            lbBg.setStroke(dp(1), Color.parseColor("#4A3A6A"));
            lockBadge.setBackground(lbBg);

            ImageView ivLock = new ImageView(this);
            FrameLayout.LayoutParams lkLp = new FrameLayout.LayoutParams(dp(10), dp(10));
            lkLp.gravity = Gravity.CENTER;
            ivLock.setLayoutParams(lkLp);
            ivLock.setImageResource(R.drawable.ic_lock_badge);
            lockBadge.addView(ivLock);
            iconFrame.addView(lockBadge);
        }

        card.addView(iconFrame);

        // ── Text block ───────────────────────────────────────────────────
        LinearLayout textBlock = new LinearLayout(this);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textBlock.setLayoutParams(tbLp);
        textBlock.setOrientation(LinearLayout.VERTICAL);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(a.title);
        tvTitle.setTextColor(a.unlocked ? Color.WHITE : Color.parseColor("#8A7AAA"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textBlock.addView(tvTitle);

        // Description / unlock hint
        if (a.unlocked) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(a.desc);
            tvDesc.setTextColor(Color.parseColor("#B8A8D8"));
            tvDesc.setTextSize(11);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            descLp.topMargin = dp(2);
            tvDesc.setLayoutParams(descLp);
            textBlock.addView(tvDesc);
        } else {
            // "HOW TO UNLOCK" row
            LinearLayout hintRow = new LinearLayout(this);
            hintRow.setOrientation(LinearLayout.HORIZONTAL);
            hintRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            hrLp.topMargin = dp(3);
            hintRow.setLayoutParams(hrLp);

            TextView tvHowLabel = new TextView(this);
            tvHowLabel.setText("HOW TO: ");
            tvHowLabel.setTextColor(Color.parseColor("#FBCE5C"));
            tvHowLabel.setTextSize(10);
            tvHowLabel.setTypeface(null, Typeface.BOLD);
            hintRow.addView(tvHowLabel);

            TextView tvHint = new TextView(this);
            tvHint.setText(a.desc);
            tvHint.setTextColor(Color.parseColor("#9A8ABB"));
            tvHint.setTextSize(10);
            hintRow.addView(tvHint);

            textBlock.addView(hintRow);
        }

        // Progress bar for multi-step achievements (when locked)
        if (!a.unlocked && a.maxProgress > 1) {
            // Progress text first
            TextView tvProg = new TextView(this);
            tvProg.setText(a.progress + " / " + a.maxProgress);
            tvProg.setTextColor(Color.parseColor("#FBCE5C"));
            tvProg.setTextSize(10);
            tvProg.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            progLp.topMargin = dp(5);
            tvProg.setLayoutParams(progLp);
            textBlock.addView(tvProg);

            // Progress bar
            ProgressBar pb = new ProgressBar(this,
                null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.topMargin = dp(3);
            pb.setLayoutParams(pbLp);
            pb.setProgressDrawable(getDrawable(R.drawable.pb_achievements));
            pb.setMax(a.maxProgress);
            pb.setProgress(a.progress);
            textBlock.addView(pb);

            // Percentage label
            int pct = a.maxProgress > 0 ? (a.progress * 100 / a.maxProgress) : 0;
            TextView tvPct = new TextView(this);
            tvPct.setText(pct + "% complete");
            tvPct.setTextColor(Color.parseColor("#6A5A8A"));
            tvPct.setTextSize(9);
            LinearLayout.LayoutParams pctLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pctLp.topMargin = dp(1);
            tvPct.setLayoutParams(pctLp);
            textBlock.addView(tvPct);
        }

        card.addView(textBlock);

        // ── Status badge (right side) ────────────────────────────────────
        FrameLayout statusFrame = new FrameLayout(this);
        LinearLayout.LayoutParams sfLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        sfLp.setMarginStart(dp(10));
        statusFrame.setLayoutParams(sfLp);

        if (a.unlocked) {
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            checkBg.setColor(0x2210B981);
            checkBg.setStroke(dp(2), Color.parseColor("#10B981"));
            statusFrame.setBackground(checkBg);

            ImageView ivCheck = new ImageView(this);
            FrameLayout.LayoutParams chLp = new FrameLayout.LayoutParams(dp(20), dp(20));
            chLp.gravity = Gravity.CENTER;
            ivCheck.setLayoutParams(chLp);
            ivCheck.setImageResource(R.drawable.ic_check_badge);
            // Tint green
            ivCheck.setColorFilter(Color.parseColor("#10B981"));
            statusFrame.addView(ivCheck);
        }

        card.addView(statusFrame);

        llList.addView(card);

        // ── Entrance animation ───────────────────────────────────────────
        int idx = llList.getChildCount();
        card.setAlpha(0f);
        card.setTranslationX(50f);
        card.animate()
            .alpha(a.unlocked ? 1f : 0.72f)
            .translationX(0f)
            .setDuration(280)
            .setStartDelay(idx * 45L)
            .start();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
