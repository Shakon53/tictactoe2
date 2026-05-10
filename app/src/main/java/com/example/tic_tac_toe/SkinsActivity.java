package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SkinsActivity extends BaseGameActivity {

    private static final String PREFS_NAME = "tictactoe_prefs";

    private SkinManager        sm;
    private AchievementManager am;
    private int                totalWins;

    private LinearLayout llBoardSkins, llSymbolSkins;
    private TextView     tvAdsStatus;
    private Button       btnRemoveAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skins);

        sm = SkinManager.get(this);
        am = AchievementManager.get(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ew = prefs.getInt("stats_easy_W",0), nw = prefs.getInt("stats_normal_W",0),
            hw = prefs.getInt("stats_hard_W",0);
        totalWins = ew + nw + hw;

        llBoardSkins  = findViewById(R.id.llBoardSkins);
        llSymbolSkins = findViewById(R.id.llSymbolSkins);
        tvAdsStatus   = findViewById(R.id.tvAdsStatus);
        btnRemoveAds  = findViewById(R.id.btnRemoveAds);

        Button btnBack = findViewById(R.id.btnSkinsBack);
        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnRemoveAds);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        buildBoardSkins();
        buildSymbolSkins();
        setupRemoveAds(prefs);
    }

    // ── Board Skins ───────────────────────────────────────────────────────────

    private void buildBoardSkins() {
        llBoardSkins.removeAllViews();
        String current = sm.getCurrentBoardSkinId();

        for (SkinManager.BoardSkin skin : sm.getBoardSkins()) {
            boolean unlocked = sm.isBoardUnlocked(skin, am, totalWins);
            boolean selected = skin.id.equals(current);
            android.view.View card = makeBoardCard(skin, unlocked, selected);

            // Stagger entrance animation
            int idx = llBoardSkins.getChildCount();
            card.setAlpha(0f);
            card.setScaleX(0.85f);
            card.setScaleY(0.85f);
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setStartDelay(idx * 60L)
                .start();

            llBoardSkins.addView(card);
        }
    }

    private android.view.View makeBoardCard(SkinManager.BoardSkin skin,
                                             boolean unlocked, boolean selected) {
        // ── Outer FrameLayout (allows badge overlay) ──────────────────────
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(dp(130), dp(190));
        frameLp.setMargins(dp(6), 0, dp(6), dp(8));
        frame.setLayoutParams(frameLp);

        // ── Card body ─────────────────────────────────────────────────────
        LinearLayout card = new LinearLayout(this);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT);
        card.setLayoutParams(cardLp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(14), dp(10), dp(12));
        card.setBackground(makeCardBg(
            selected ? 0xFF8B5CF6 : (unlocked ? 0x5510B981 : 0x33FFFFFF),
            selected));

        // ── Mini board preview (2×2 cells) ───────────────────────────────
        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int c = 0; c < 2; c++) {
                android.view.View cell = new android.view.View(this);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(24), dp(24));
                cp.setMargins(dp(2), dp(2), dp(2), dp(2));
                cell.setLayoutParams(cp);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(skin.cellBg);
                gd.setCornerRadius(dp(5));
                gd.setStroke(dp(1), skin.cellStroke);
                cell.setBackground(gd);
                row.addView(cell);
            }
            preview.addView(row);
        }
        // Add small X/O symbols inside preview cells for realism
        card.addView(preview);

        // ── Accent stripe ─────────────────────────────────────────────────
        android.view.View stripe = new android.view.View(this);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(3));
        stLp.setMargins(dp(6), dp(8), dp(6), 0);
        stripe.setLayoutParams(stLp);
        GradientDrawable stripeBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{skin.winStroke, adjustAlpha(skin.winStroke, 0.4f)});
        stripeBg.setCornerRadius(dp(2));
        stripe.setBackground(stripeBg);
        card.addView(stripe);

        // ── Skin name ─────────────────────────────────────────────────────
        TextView tvName = new TextView(this);
        tvName.setText(skin.name);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(12);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = dp(6);
        tvName.setLayoutParams(nameLp);
        card.addView(tvName);

        // ── State label ───────────────────────────────────────────────────
        if (selected) {
            TextView tvActive = new TextView(this);
            tvActive.setText("● ACTIVE");
            tvActive.setTextColor(Color.parseColor("#8B5CF6"));
            tvActive.setTextSize(9.5f);
            tvActive.setTypeface(null, Typeface.BOLD);
            tvActive.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            alp.topMargin = dp(4);
            tvActive.setLayoutParams(alp);
            card.addView(tvActive);
        } else if (unlocked) {
            TextView tvTap = new TextView(this);
            tvTap.setText("Tap to select");
            tvTap.setTextColor(Color.parseColor("#10B981"));
            tvTap.setTextSize(9);
            tvTap.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(4);
            tvTap.setLayoutParams(tlp);
            card.addView(tvTap);
        } else {
            // Show unlock requirement prominently
            TextView tvHow = new TextView(this);
            tvHow.setText("UNLOCK:");
            tvHow.setTextColor(Color.parseColor("#FBCE5C"));
            tvHow.setTextSize(8);
            tvHow.setTypeface(null, Typeface.BOLD);
            tvHow.setLetterSpacing(0.1f);
            tvHow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams howLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            howLp.topMargin = dp(5);
            tvHow.setLayoutParams(howLp);
            card.addView(tvHow);

            TextView tvReq = new TextView(this);
            tvReq.setText(sm.getBoardUnlockLabel(skin, am));
            tvReq.setTextColor(Color.parseColor("#FFD580"));
            tvReq.setTextSize(8.5f);
            tvReq.setGravity(Gravity.CENTER);
            tvReq.setLineSpacing(0, 1.2f);
            LinearLayout.LayoutParams reqLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            reqLp.topMargin = dp(2);
            tvReq.setLayoutParams(reqLp);
            card.addView(tvReq);

            // Dim the card
            card.setAlpha(0.6f);
        }

        frame.addView(card);

        // ── Badge overlay (top-right corner) ─────────────────────────────
        FrameLayout badge = new FrameLayout(this);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(26), dp(26));
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.setMargins(0, dp(6), dp(6), 0);
        badge.setLayoutParams(badgeLp);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        if (selected) {
            badgeBg.setColor(Color.parseColor("#8B5CF6"));
        } else if (unlocked) {
            badgeBg.setColor(Color.parseColor("#10B981"));
        } else {
            badgeBg.setColor(Color.parseColor("#CC3A1A6A"));
        }
        badge.setBackground(badgeBg);

        ImageView ivBadge = new ImageView(this);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(14), dp(14));
        iconLp.gravity = Gravity.CENTER;
        ivBadge.setLayoutParams(iconLp);
        if (selected) {
            ivBadge.setImageResource(R.drawable.ic_check_badge);
        } else if (unlocked) {
            ivBadge.setImageResource(R.drawable.ic_check_badge);
        } else {
            ivBadge.setImageResource(R.drawable.ic_lock_badge);
        }
        badge.addView(ivBadge);
        frame.addView(badge);

        // ── Click handling ────────────────────────────────────────────────
        if (unlocked && !selected) {
            frame.setOnClickListener(v -> {
                sm.setBoardSkin(skin.id);
                buildBoardSkins();
                Toast.makeText(this, skin.name + " theme applied!", Toast.LENGTH_SHORT).show();
            });
        } else if (!unlocked) {
            frame.setOnClickListener(v -> {
                showLockedToast("Unlock: " + sm.getBoardUnlockLabel(skin, am));
            });
        }

        return frame;
    }

    // ── Symbol Skins ──────────────────────────────────────────────────────────

    private void buildSymbolSkins() {
        llSymbolSkins.removeAllViews();
        String current = sm.getCurrentSymbolSkinId();

        for (SkinManager.SymbolSkin sym : sm.getSymbolSkins()) {
            boolean unlocked = sm.isSymbolUnlocked(sym, am, totalWins);
            boolean selected = sym.id.equals(current);
            android.view.View card = makeSymbolCard(sym, unlocked, selected);

            int idx = llSymbolSkins.getChildCount();
            card.setAlpha(0f);
            card.setScaleX(0.85f);
            card.setScaleY(0.85f);
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setStartDelay(idx * 60L)
                .start();

            llSymbolSkins.addView(card);
        }
    }

    private android.view.View makeSymbolCard(SkinManager.SymbolSkin sym,
                                              boolean unlocked, boolean selected) {
        // ── Outer FrameLayout ─────────────────────────────────────────────
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(dp(120), dp(185));
        frameLp.setMargins(dp(6), 0, dp(6), dp(8));
        frame.setLayoutParams(frameLp);

        // ── Card body ─────────────────────────────────────────────────────
        LinearLayout card = new LinearLayout(this);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT);
        card.setLayoutParams(cardLp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(16), dp(10), dp(12));
        card.setBackground(makeCardBg(
            selected ? 0xFF8B5CF6 : (unlocked ? 0x5510B981 : 0x33FFFFFF),
            selected));

        // ── Symbol preview row: X vs O ────────────────────────────────────
        LinearLayout symRow = new LinearLayout(this);
        symRow.setOrientation(LinearLayout.HORIZONTAL);
        symRow.setGravity(Gravity.CENTER);

        // Player symbol circle
        FrameLayout xCircle = makeSymbolCircle(sym.playerSymbol, sym.playerColor);
        symRow.addView(xCircle);

        TextView tvVs = new TextView(this);
        tvVs.setText("vs");
        tvVs.setTextColor(Color.parseColor("#6A5A8A"));
        tvVs.setTextSize(10);
        tvVs.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vsLp.setMargins(dp(6), 0, dp(6), 0);
        tvVs.setLayoutParams(vsLp);
        symRow.addView(tvVs);

        // AI symbol circle
        FrameLayout oCircle = makeSymbolCircle(sym.aiSymbol, sym.aiColor);
        symRow.addView(oCircle);

        card.addView(symRow);

        // ── Player / AI labels ────────────────────────────────────────────
        LinearLayout labelsRow = new LinearLayout(this);
        labelsRow.setOrientation(LinearLayout.HORIZONTAL);
        labelsRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lrLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lrLp.topMargin = dp(4);
        labelsRow.setLayoutParams(lrLp);

        TextView tvYou = makeLabel("YOU", sym.playerColor);
        LinearLayout.LayoutParams youLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvYou.setLayoutParams(youLp);
        labelsRow.addView(tvYou);

        android.view.View spacer = new android.view.View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(40), 0));
        labelsRow.addView(spacer);

        TextView tvAi = makeLabel("AI", sym.aiColor);
        LinearLayout.LayoutParams aiLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvAi.setLayoutParams(aiLp);
        labelsRow.addView(tvAi);

        card.addView(labelsRow);

        // ── Gradient accent stripe ─────────────────────────────────────────
        android.view.View stripe2 = new android.view.View(this);
        LinearLayout.LayoutParams st2Lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(3));
        st2Lp.setMargins(dp(6), dp(10), dp(6), 0);
        stripe2.setLayoutParams(st2Lp);
        GradientDrawable stripe2Bg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{sym.playerColor, sym.aiColor});
        stripe2Bg.setCornerRadius(dp(2));
        stripe2.setBackground(stripe2Bg);
        card.addView(stripe2);

        // ── Skin name ─────────────────────────────────────────────────────
        TextView tvName = new TextView(this);
        tvName.setText(sym.name);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(12);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = dp(6);
        tvName.setLayoutParams(nameLp);
        card.addView(tvName);

        // ── State label ───────────────────────────────────────────────────
        if (selected) {
            TextView tvActive = new TextView(this);
            tvActive.setText("● ACTIVE");
            tvActive.setTextColor(Color.parseColor("#8B5CF6"));
            tvActive.setTextSize(9.5f);
            tvActive.setTypeface(null, Typeface.BOLD);
            tvActive.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            alp.topMargin = dp(4);
            tvActive.setLayoutParams(alp);
            card.addView(tvActive);
        } else if (unlocked) {
            TextView tvTap = new TextView(this);
            tvTap.setText("Tap to select");
            tvTap.setTextColor(Color.parseColor("#10B981"));
            tvTap.setTextSize(9);
            tvTap.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(4);
            tvTap.setLayoutParams(tlp);
            card.addView(tvTap);
        } else {
            TextView tvHow = new TextView(this);
            tvHow.setText("UNLOCK:");
            tvHow.setTextColor(Color.parseColor("#FBCE5C"));
            tvHow.setTextSize(8);
            tvHow.setTypeface(null, Typeface.BOLD);
            tvHow.setLetterSpacing(0.1f);
            tvHow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams howLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            howLp.topMargin = dp(5);
            tvHow.setLayoutParams(howLp);
            card.addView(tvHow);

            TextView tvReq = new TextView(this);
            tvReq.setText(sm.getSymbolUnlockLabel(sym, am));
            tvReq.setTextColor(Color.parseColor("#FFD580"));
            tvReq.setTextSize(8.5f);
            tvReq.setGravity(Gravity.CENTER);
            tvReq.setLineSpacing(0, 1.2f);
            LinearLayout.LayoutParams reqLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            reqLp.topMargin = dp(2);
            tvReq.setLayoutParams(reqLp);
            card.addView(tvReq);

            card.setAlpha(0.6f);
        }

        frame.addView(card);

        // ── Badge overlay ─────────────────────────────────────────────────
        FrameLayout badge = new FrameLayout(this);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(26), dp(26));
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.setMargins(0, dp(6), dp(6), 0);
        badge.setLayoutParams(badgeLp);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        if (selected) {
            badgeBg.setColor(Color.parseColor("#8B5CF6"));
        } else if (unlocked) {
            badgeBg.setColor(Color.parseColor("#10B981"));
        } else {
            badgeBg.setColor(Color.parseColor("#CC3A1A6A"));
        }
        badge.setBackground(badgeBg);

        ImageView ivBadge = new ImageView(this);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(14), dp(14));
        iconLp.gravity = Gravity.CENTER;
        ivBadge.setLayoutParams(iconLp);
        if (selected || unlocked) {
            ivBadge.setImageResource(R.drawable.ic_check_badge);
        } else {
            ivBadge.setImageResource(R.drawable.ic_lock_badge);
        }
        badge.addView(ivBadge);
        frame.addView(badge);

        // ── Click handling ────────────────────────────────────────────────
        if (unlocked && !selected) {
            frame.setOnClickListener(v -> {
                sm.setSymbolSkin(sym.id);
                buildSymbolSkins();
                Toast.makeText(this, sym.name + " symbols applied!", Toast.LENGTH_SHORT).show();
            });
        } else if (!unlocked) {
            frame.setOnClickListener(v -> {
                showLockedToast("Unlock: " + sm.getSymbolUnlockLabel(sym, am));
            });
        }

        return frame;
    }

    // ── Remove Ads ────────────────────────────────────────────────────────────

    private void setupRemoveAds(SharedPreferences prefs) {
        boolean adsRemoved = prefs.getBoolean("ads_removed", false);
        if (adsRemoved) {
            tvAdsStatus.setText("✓ Ads removed — thank you!");
            tvAdsStatus.setTextColor(Color.parseColor("#10B981"));
            btnRemoveAds.setText("ACTIVE");
            btnRemoveAds.setAlpha(0.5f);
            btnRemoveAds.setEnabled(false);
        } else {
            btnRemoveAds.setOnClickListener(v -> showPurchaseDialog(prefs));
        }
    }

    private void showPurchaseDialog(SharedPreferences prefs) {
        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
            .setTitle("Remove Ads")
            .setMessage(
                "Remove all ads for just $0.99!\n\n" +
                "• No more banner ads\n" +
                "• Cleaner game experience\n" +
                "• One-time purchase, forever\n\n" +
                "This is a one-time payment — no subscription!")
            .setPositiveButton("Purchase $0.99", (dialog, w) -> {
                prefs.edit().putBoolean("ads_removed", true).apply();
                tvAdsStatus.setText("✓ Ads removed — thank you!");
                tvAdsStatus.setTextColor(Color.parseColor("#10B981"));
                btnRemoveAds.setText("ACTIVE");
                btnRemoveAds.setAlpha(0.5f);
                btnRemoveAds.setEnabled(false);
                Toast.makeText(this, "Ads removed! Thank you!", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Maybe later", null)
            .create();

        if (d.getWindow() != null) {
            d.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.75f);
        }
        d.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a small circle with the symbol inside — used in symbol skin preview */
    private FrameLayout makeSymbolCircle(String symbol, int color) {
        FrameLayout circle = new FrameLayout(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(38), dp(38));
        circle.setLayoutParams(clp);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(adjustAlpha(color, 0.18f));
        circleBg.setStroke(dp(1), adjustAlpha(color, 0.5f));
        circle.setBackground(circleBg);

        TextView tvSym = new TextView(this);
        FrameLayout.LayoutParams symLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        symLp.gravity = Gravity.CENTER;
        tvSym.setLayoutParams(symLp);
        tvSym.setText(symbol);
        tvSym.setTextColor(color);
        tvSym.setTextSize(18);
        tvSym.setTypeface(null, Typeface.BOLD);
        circle.addView(tvSym);

        return circle;
    }

    /** Tiny label below each symbol circle */
    private TextView makeLabel(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(adjustAlpha(color, 0.7f));
        tv.setTextSize(8);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private void showLockedToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable makeCardBg(int strokeColor, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(selected ? 0x308B5CF6 : 0x14FFFFFF);
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(selected ? 2 : 1), strokeColor);
        return gd;
    }

    /** Adjust alpha of a packed color (0–1f) */
    private int adjustAlpha(int color, float alpha) {
        int a = Math.round(Color.alpha(color) * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
