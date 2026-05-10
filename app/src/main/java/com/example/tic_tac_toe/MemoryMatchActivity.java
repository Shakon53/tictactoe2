package com.example.tic_tac_toe;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryMatchActivity extends BaseGameActivity {

    private static final String PREFS_NAME = "tictactoe_prefs";
    private static final String KEY_MM_BEST = "mm_best_moves";

    private static final String[] EMOJIS = {
        "🐶", "🐱", "🎮", "🌟", "🎯", "🔥", "💎", "🚀"
    };

    private final String[]  cards   = new String[16];
    private final boolean[] flipped = new boolean[16];
    private final boolean[] matched = new boolean[16];
    private final Button[]  cardBtns = new Button[16];

    private int firstIdx  = -1;
    private int secondIdx = -1;
    private boolean checking   = false;
    private boolean gameActive = false;
    private int moves      = 0;
    private int pairsFound = 0;
    private int seconds    = 0;

    private TextView tvMoves, tvTimer, tvPairs, tvBest;
    private GridLayout cardGrid;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_match);

        tvMoves  = findViewById(R.id.tvMMMoves);
        tvTimer  = findViewById(R.id.tvMMTimer);
        tvPairs  = findViewById(R.id.tvMMPairs);
        tvBest   = findViewById(R.id.tvMMBest);
        cardGrid = findViewById(R.id.mmCardGrid);

        Button btnBack    = findViewById(R.id.btnMMBack);
        Button btnNewGame = findViewById(R.id.btnMMNewGame);

        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnNewGame);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });
        btnNewGame.setOnClickListener(v -> startNewGame());

        updateBestDisplay();
        startNewGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ─── Game setup ───────────────────────────────────────────────────────────

    private void startNewGame() {
        handler.removeCallbacksAndMessages(null);
        Arrays.fill(flipped, false);
        Arrays.fill(matched, false);
        firstIdx   = -1;
        secondIdx  = -1;
        checking   = false;
        gameActive = false;
        moves      = 0;
        pairsFound = 0;
        seconds    = 0;

        tvMoves.setText("0 moves");
        tvTimer.setText("0:00");
        tvPairs.setText("0 / 8");

        shuffleCards();
        buildGrid();
    }

    private void shuffleCards() {
        List<String> list = new ArrayList<>();
        for (String e : EMOJIS) { list.add(e); list.add(e); }
        Collections.shuffle(list);
        for (int i = 0; i < 16; i++) cards[i] = list.get(i);
    }

    private void buildGrid() {
        cardGrid.removeAllViews();
        cardGrid.setColumnCount(4);
        cardGrid.setRowCount(4);

        for (int i = 0; i < 16; i++) {
            final int idx = i;
            Button btn = new Button(this);
            btn.setText("?");
            btn.setTextSize(26f);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setTextColor(0xFFB8A8D8);
            btn.setBackground(ContextCompat.getDrawable(this, R.drawable.game_cell_transparent));
            btn.setStateListAnimator(null);

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width  = 0;
            p.height = 0;
            p.rowSpec    = GridLayout.spec(i / 4, 1, 1f);
            p.columnSpec = GridLayout.spec(i % 4, 1, 1f);
            p.setMargins(5, 5, 5, 5);
            btn.setLayoutParams(p);

            btn.setOnClickListener(v -> onCardClick(idx));
            cardBtns[i] = btn;
            cardGrid.addView(btn);
        }
    }

    // ─── Gameplay ─────────────────────────────────────────────────────────────

    private void onCardClick(int idx) {
        if (checking || flipped[idx] || matched[idx]) return;

        if (!gameActive) startTimer();

        flipToFront(idx);

        if (firstIdx == -1) {
            firstIdx = idx;
        } else {
            secondIdx = idx;
            moves++;
            tvMoves.setText(moves + " moves");
            checking = true;
            handler.postDelayed(this::checkMatch, 750);
        }
    }

    private void checkMatch() {
        if (cards[firstIdx].equals(cards[secondIdx])) {
            // Matched!
            matched[firstIdx] = true;
            matched[secondIdx] = true;
            cardBtns[firstIdx].setBackgroundColor(0x5010B981);
            cardBtns[secondIdx].setBackgroundColor(0x5010B981);
            pairsFound++;
            tvPairs.setText(pairsFound + " / 8");

            if (pairsFound == 8) {
                stopTimer();
                handler.postDelayed(this::showWinDialog, 400);
            }
        } else {
            flipToBack(firstIdx);
            flipToBack(secondIdx);
        }
        firstIdx  = -1;
        secondIdx = -1;
        checking  = false;
    }

    private void flipToFront(int idx) {
        flipped[idx] = true;
        Button btn = cardBtns[idx];
        btn.animate().scaleX(0f).setDuration(100).withEndAction(() -> {
            btn.setText(cards[idx]);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundColor(0x338B5CF6);
            btn.animate().scaleX(1f).setDuration(100).start();
        }).start();
    }

    private void flipToBack(int idx) {
        flipped[idx] = false;
        Button btn = cardBtns[idx];
        btn.animate().scaleX(0f).setDuration(100).withEndAction(() -> {
            btn.setText("?");
            btn.setTextColor(0xFFB8A8D8);
            btn.setBackground(ContextCompat.getDrawable(this, R.drawable.game_cell_transparent));
            btn.animate().scaleX(1f).setDuration(100).start();
        }).start();
    }

    // ─── Timer ────────────────────────────────────────────────────────────────

    private void startTimer() {
        gameActive = true;
        timerRunnable = new Runnable() {
            @Override public void run() {
                seconds++;
                int m = seconds / 60, s = seconds % 60;
                tvTimer.setText(String.format("%d:%02d", m, s));
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        gameActive = false;
        if (timerRunnable != null) handler.removeCallbacks(timerRunnable);
    }

    // ─── Win dialog ───────────────────────────────────────────────────────────

    private void showWinDialog() {
        if (isFinishing() || isDestroyed()) return;

        // Save best score
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int prevBest = prefs.getInt(KEY_MM_BEST, Integer.MAX_VALUE);
        boolean newBest = moves < prevBest;
        if (newBest) prefs.edit().putInt(KEY_MM_BEST, moves).apply();
        updateBestDisplay();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        // Build inline layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_glass_card));
        int p = dpToPx(24);
        layout.setPadding(p, p, p, p);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText("★");
        tvEmoji.setTextColor(android.graphics.Color.parseColor("#FBCE5C"));
        tvEmoji.setTextSize(54f);
        tvEmoji.setGravity(android.view.Gravity.CENTER);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("YOU WIN!");
        tvTitle.setTextSize(28f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
        tvTitle.setGravity(android.view.Gravity.CENTER);

        TextView tvInfo = new TextView(this);
        int m = seconds / 60, s = seconds % 60;
        tvInfo.setText("Moves: " + moves + "  ·  Time: " + m + ":" + String.format("%02d", s)
            + (newBest ? "\nNew best time!" : ""));
        tvInfo.setTextSize(14f);
        tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvInfo.setGravity(android.view.Gravity.CENTER);

        int gap = dpToPx(12);
        android.widget.LinearLayout.LayoutParams lp =
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, gap, 0, 0);

        Button btnAgain = new Button(this);
        btnAgain.setText("↺  PLAY AGAIN");
        btnAgain.setTextSize(14f);
        btnAgain.setTypeface(null, Typeface.BOLD);
        btnAgain.setTextColor(Color.WHITE);
        btnAgain.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_new_game_btn));
        btnAgain.setStateListAnimator(null);
        android.widget.LinearLayout.LayoutParams lpBtn =
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        lpBtn.setMargins(0, dpToPx(20), 0, 0);

        layout.addView(tvEmoji);
        layout.addView(tvTitle, lp);
        layout.addView(tvInfo, lp);
        layout.addView(btnAgain, lpBtn);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.75f);
        }

        btnAgain.setOnClickListener(v -> { dialog.dismiss(); startNewGame(); });

        dialog.show();
        if (dialog.getWindow() != null) {
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(w, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Entrance animation
        View dv = dialog.getWindow().getDecorView();
        dv.setScaleX(0.7f); dv.setScaleY(0.7f); dv.setAlpha(0f);
        dv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(340)
            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
    }

    private void updateBestDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int best = prefs.getInt(KEY_MM_BEST, Integer.MAX_VALUE);
        tvBest.setText(best == Integer.MAX_VALUE ? "—" : String.valueOf(best));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
