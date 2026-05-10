package com.example.tic_tac_toe;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Achievement / Skin imports (same package — no extra import needed)

public class SinglePlayerActivity extends BaseGameActivity {

    private static final String PREFS_NAME        = "tictactoe_prefs";
    private static final String KEY_STREAK        = "win_streak";
    private static final String KEY_BEST          = "best_streak";
    private static final String KEY_VIBRATION     = "pref_vibration";
    private static final String KEY_SOUND         = "pref_sound";
    private static final String KEY_BGM           = "pref_bgm";
    private static final String KEY_AI_DIFFICULTY = "pref_ai_difficulty";
    private static final String KEY_PLAYER_FIRST  = "pref_player_first";
    private static final String KEY_BOARD_SIZE    = "pref_board_size";

    private static final String PLAYER   = "X";
    private static final String AI       = "O";
    private static final int    AI_DELAY = 450;

    private enum Difficulty { EASY, NORMAL, HARD }

    // Dynamic board
    private int boardSize  = 3;
    private int winLength  = 3;
    private String[] board;
    private Button[]  cells;
    private List<int[]> winCombos;
    private GridLayout gameBoardGrid;

    private TextView  tvStatus, tvMyScore, tvAiScore, tvMySymbol, tvAiSymbol;
    private TextView  tvMyStats, tvAiStats, tvCenterDiff;
    private TextView  tvDifficultyHint, tvOverallStats, tvHardMedals, tvStreakBanner;
    private CompoundButton switchPlayerFirst;
    private Button    btnRematch, btnQuickSettings;
    private Button    btnDifficultyEasy, btnDifficultyNormal, btnDifficultyHard;

    private boolean gameOver    = false;
    private boolean playerTurn  = true;
    private boolean playerFirst = true;

    private int myWins, myLosses, myDraws;
    private int aiWins, aiLosses, aiDraws;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Vibrator      vibrator;
    private ToneGenerator toneGen;
    private final Random  random = new Random();
    private Difficulty    difficulty = Difficulty.NORMAL;

    // ── Achievement + Skin state ───────────────────────────────────────────────
    private AchievementManager achManager;
    private SkinManager        skinManager;
    /** Milliseconds when the current game started (set in resetBoard) */
    private long   gameStartMs;
    /** When the current player turn started (to measure only player think time) */
    private long   playerTurnStartMs;
    /** Accumulated milliseconds the player spent thinking this game */
    private long   playerTotalThinkMs;
    /** Total moves in the current game */
    private int    gameMoves;
    /** Moves made only by the player this game */
    private int    playerMovesInGame;
    /** How many consecutive losses before this game */
    private int    consecutiveLosses;
    // Queue of achievements to show one by one
    private final List<Achievement> achQueue = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player);

        vibrator    = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        achManager  = AchievementManager.get(this);
        skinManager = SkinManager.get(this);
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 80); }
        catch (Exception ignored) {}

        // Load consecutive-losses counter from prefs
        consecutiveLosses = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("consecutive_losses", 0);

        tvStatus        = findViewById(R.id.tvGameStatus);
        tvMyScore       = findViewById(R.id.tvMyScore);
        tvAiScore       = findViewById(R.id.tvAiScore);
        tvMySymbol      = findViewById(R.id.tvMySymbol);
        tvAiSymbol      = findViewById(R.id.tvAiSymbol);
        tvMyStats       = findViewById(R.id.tvMyStats);
        tvAiStats       = findViewById(R.id.tvAiStats);
        tvCenterDiff    = findViewById(R.id.tvCenterDiff);
        tvDifficultyHint = findViewById(R.id.tvDifficultyHint);
        tvOverallStats  = findViewById(R.id.tvOverallStats);
        tvHardMedals    = findViewById(R.id.tvHardMedals);
        tvStreakBanner  = findViewById(R.id.tvStreakBanner);
        switchPlayerFirst = findViewById(R.id.switchPlayerFirst);
        btnRematch       = findViewById(R.id.btnRematch);
        btnQuickSettings = findViewById(R.id.btnQuickSettings);
        Button btnNewGame = findViewById(R.id.btnNewGame);
        btnDifficultyEasy   = findViewById(R.id.btnDifficultyEasy);
        btnDifficultyNormal = findViewById(R.id.btnDifficultyNormal);
        btnDifficultyHard   = findViewById(R.id.btnDifficultyHard);
        gameBoardGrid   = findViewById(R.id.gameBoardGrid);

        tvMySymbol.setText(PLAYER);
        tvMySymbol.setTextColor(0xFFFBBF24);
        tvAiSymbol.setText(AI);
        tvAiSymbol.setTextColor(0xFF38BDF8);

        Button btnBack = findViewById(R.id.btnBack);
        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnRematch);
        GameButtonHelper.apply(btnQuickSettings);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });
        btnRematch.setOnClickListener(v -> resetBoard());
        btnNewGame.setOnClickListener(v -> resetBoard());
        btnQuickSettings.setOnClickListener(v -> showQuickSettingsDialog());

        setupDifficultyButtons();
        setupPlayerOrderSwitch();
        loadBoardSize();
        setupBoard();
        updateOverallStats();
        resetBoard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (toneGen != null) toneGen.release();
    }

    // ── Board Setup ────────────────────────────────────────────────────────────

    private void loadBoardSize() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boardSize = prefs.getInt(KEY_BOARD_SIZE, 3);
        winLength = boardSize == 3 ? 3 : 4;
    }

    private void setupBoard() {
        gameBoardGrid.removeAllViews();
        gameBoardGrid.setColumnCount(boardSize);
        gameBoardGrid.setRowCount(boardSize);

        int total = boardSize * boardSize;
        board = new String[total];
        cells = new Button[total];

        float textSizeSp = boardSize == 3 ? 38f : boardSize == 4 ? 26f : 20f;
        int marginPx = dpToPx(boardSize == 5 ? 3 : 5);

        SkinManager.BoardSkin activeSkin = skinManager.getCurrentBoardSkin();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < total; i++) {
            final int idx = i;
            Button btn = new Button(this);
            btn.setTextSize(textSizeSp);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setBackground(skinManager.makeCellDrawable(activeSkin, density));
            btn.setStateListAnimator(null);
            btn.setTextColor(Color.WHITE);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = 0;
            params.height = 0;
            params.rowSpec    = GridLayout.spec(i / boardSize, 1, 1f);
            params.columnSpec = GridLayout.spec(i % boardSize, 1, 1f);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> onCellClick(idx));
            cells[i] = btn;
            gameBoardGrid.addView(btn);
        }
        winCombos = generateWinCombos(boardSize, winLength);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private List<int[]> generateWinCombos(int size, int wl) {
        List<int[]> list = new ArrayList<>();
        // rows
        for (int r = 0; r < size; r++)
            for (int c = 0; c <= size - wl; c++) {
                int[] combo = new int[wl];
                for (int i = 0; i < wl; i++) combo[i] = r * size + c + i;
                list.add(combo);
            }
        // cols
        for (int c = 0; c < size; c++)
            for (int r = 0; r <= size - wl; r++) {
                int[] combo = new int[wl];
                for (int i = 0; i < wl; i++) combo[i] = (r + i) * size + c;
                list.add(combo);
            }
        // diag TL-BR
        for (int r = 0; r <= size - wl; r++)
            for (int c = 0; c <= size - wl; c++) {
                int[] combo = new int[wl];
                for (int i = 0; i < wl; i++) combo[i] = (r + i) * size + (c + i);
                list.add(combo);
            }
        // diag TR-BL
        for (int r = 0; r <= size - wl; r++)
            for (int c = wl - 1; c < size; c++) {
                int[] combo = new int[wl];
                for (int i = 0; i < wl; i++) combo[i] = (r + i) * size + (c - i);
                list.add(combo);
            }
        return list;
    }

    // ── Gameplay ───────────────────────────────────────────────────────────────

    private void onCellClick(int idx) {
        if (gameOver || !playerTurn || board[idx] != null) return;
        playerTotalThinkMs += System.currentTimeMillis() - playerTurnStartMs;
        playerMovesInGame++;
        playMove();
        makeMove(idx, PLAYER);
        if (checkWin(PLAYER)) { endGame(true); return; }
        if (isBoardFull())    { endDraw();     return; }
        playerTurn = false;
        tvStatus.setText(R.string.status_ai_thinking);
        handler.postDelayed(this::aiMove, AI_DELAY);
    }

    private void aiMove() {
        playMove();
        makeMove(chooseAiMove(), AI);
        if (checkWin(AI)) { endGame(false); return; }
        if (isBoardFull()) { endDraw();     return; }
        playerTurn = true;
        playerTurnStartMs = System.currentTimeMillis();
        tvStatus.setText(getString(R.string.status_your_turn_difficulty, difficultyLabel(difficulty)));
    }

    private void makeMove(int idx, String symbol) {
        board[idx] = symbol;
        gameMoves++;
        SkinManager.SymbolSkin sym = skinManager.getCurrentSymbolSkin();
        cells[idx].setText(symbol.equals(PLAYER) ? sym.playerSymbol : sym.aiSymbol);
        cells[idx].setTextColor(symbol.equals(PLAYER) ? sym.playerColor : sym.aiColor);
        ObjectAnimator pop = ObjectAnimator.ofPropertyValuesHolder(cells[idx],
            PropertyValuesHolder.ofFloat("scaleX", 0.45f, 1.15f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.45f, 1.15f, 1f));
        pop.setDuration(260);
        pop.start();
    }

    private void endGame(boolean playerWon) {
        gameOver = true;
        long gameTimeMs = System.currentTimeMillis() - gameStartMs;
        highlightWin(playerWon ? PLAYER : AI);

        if (playerWon) {
            myWins++; aiLosses++;
            tvStatus.setText(R.string.status_you_win);
            playWin();
            saveStreak(true);
            saveDifficultyStats("W");
        } else {
            myLosses++; aiWins++;
            tvStatus.setText(R.string.status_ai_wins);
            playLose();
            saveStreak(false);
            saveDifficultyStats("L");
        }

        // ── Achievement check ────────────────────────────────────────────
        checkAchievements(playerWon, gameTimeMs);

        updateScores();
        updateOverallStats();
        btnRematch.setVisibility(Button.VISIBLE);
        handler.postDelayed(() -> showResultDialog(playerWon ? 1 : -1), 800);
    }

    private void endDraw() {
        gameOver = true;
        long gameTimeMs = System.currentTimeMillis() - gameStartMs;
        myDraws++; aiDraws++;
        tvStatus.setText(R.string.status_draw);
        playDraw();
        saveDifficultyStats("D");
        checkAchievements(false, gameTimeMs);
        updateScores();
        updateOverallStats();
        btnRematch.setVisibility(Button.VISIBLE);
        handler.postDelayed(() -> showResultDialog(0), 800);
    }

    private void resetBoard() {
        handler.removeCallbacks(this::aiMove);
        SkinManager.BoardSkin activeSkin = skinManager.getCurrentBoardSkin();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < board.length; i++) {
            board[i] = null;
            cells[i].setText("");
            cells[i].setBackground(skinManager.makeCellDrawable(activeSkin, density));
        }
        gameOver  = false;
        gameMoves = 0;
        playerMovesInGame = 0;
        playerTotalThinkMs = 0;
        gameStartMs = System.currentTimeMillis();
        btnRematch.setVisibility(Button.GONE);
        playerTurn = playerFirst;
        if (playerTurn) {
            playerTurnStartMs = System.currentTimeMillis();
            tvStatus.setText(getString(R.string.status_your_turn_difficulty, difficultyLabel(difficulty)));
        } else {
            tvStatus.setText(R.string.status_ai_starts);
            handler.postDelayed(this::aiMove, AI_DELAY);
        }
    }

    private void highlightWin(String symbol) {
        SkinManager.BoardSkin activeSkin = skinManager.getCurrentBoardSkin();
        float density = getResources().getDisplayMetrics().density;
        for (int[] c : winCombos) {
            boolean allMatch = true;
            for (int idx : c) if (!symbol.equals(board[idx])) { allMatch = false; break; }
            if (!allMatch) continue;
            for (int idx : c) {
                cells[idx].setBackground(skinManager.makeWinDrawable(activeSkin, density));
                ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(cells[idx],
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f));
                pulse.setDuration(200);
                pulse.setRepeatCount(3);
                pulse.setRepeatMode(ValueAnimator.REVERSE);
                pulse.start();
            }
            return;
        }
    }

    private void updateScores() {
        tvMyScore.setText(String.valueOf(myWins));
        tvMyStats.setText(myLosses + "L · " + myDraws + "D");
        tvAiScore.setText(String.valueOf(aiWins));
        tvAiStats.setText(aiLosses + "L · " + aiDraws + "D");
    }

    // ── Streak Banner ──────────────────────────────────────────────────────────

    private void saveStreak(boolean won) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int streak = prefs.getInt(KEY_STREAK, 0);
        int best   = prefs.getInt(KEY_BEST, 0);
        streak = won ? streak + 1 : 0;
        if (streak > best) best = streak;
        prefs.edit().putInt(KEY_STREAK, streak).putInt(KEY_BEST, best).apply();
        if (won && streak >= 2) showStreakBanner(streak);
    }

    private void showStreakBanner(int streak) {
        if (tvStreakBanner == null) return;
        tvStreakBanner.setText(streak + " wins in a row!");
        tvStreakBanner.setVisibility(View.VISIBLE);
        tvStreakBanner.setAlpha(0f);
        tvStreakBanner.setTranslationY(-50f);
        tvStreakBanner.animate()
            .alpha(1f).translationY(0f).setDuration(350)
            .withEndAction(() ->
                tvStreakBanner.postDelayed(() ->
                    tvStreakBanner.animate().alpha(0f).setDuration(350)
                        .withEndAction(() -> tvStreakBanner.setVisibility(View.GONE))
                        .start(),
                    2400)
            ).start();
    }

    // ── Vibration & Sound ──────────────────────────────────────────────────────

    private boolean vibrationOn() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true);
    }

    private boolean soundOn() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND, true);
    }

    private void vibrate(long ms) {
        if (!vibrationOn() || vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else { vibrator.vibrate(ms); }
    }

    private void vibratePattern(long[] pattern) {
        if (!vibrationOn() || vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else { vibrator.vibrate(pattern, -1); }
    }

    private void playTone(int tone, int durationMs) {
        if (!soundOn() || toneGen == null) return;
        try { toneGen.startTone(tone, durationMs); } catch (Exception ignored) {}
    }

    private void playMove()  { vibrate(30);  playTone(ToneGenerator.TONE_PROP_BEEP, 60); }
    private void playWin()   { vibratePattern(new long[]{0,80,60,120}); playTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 500); }
    private void playLose()  { vibratePattern(new long[]{0,60,60,60});  playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 400); }
    private void playDraw()  { vibrate(80);  playTone(ToneGenerator.TONE_PROP_NACK, 300); }

    // ── Difficulty ─────────────────────────────────────────────────────────────

    private void setupDifficultyButtons() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_AI_DIFFICULTY, Difficulty.NORMAL.name());
        try { difficulty = Difficulty.valueOf(saved); }
        catch (IllegalArgumentException ignored) { difficulty = Difficulty.NORMAL; }
        GameButtonHelper.applyAll(btnDifficultyEasy, btnDifficultyNormal, btnDifficultyHard);
        btnDifficultyEasy.setOnClickListener(v   -> setDifficulty(Difficulty.EASY));
        btnDifficultyNormal.setOnClickListener(v -> setDifficulty(Difficulty.NORMAL));
        btnDifficultyHard.setOnClickListener(v   -> setDifficulty(Difficulty.HARD));
        updateDifficultyUi();
    }

    private void setupPlayerOrderSwitch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        playerFirst = prefs.getBoolean(KEY_PLAYER_FIRST, true);
        switchPlayerFirst.setChecked(playerFirst);
        switchPlayerFirst.setOnCheckedChangeListener((buttonView, isChecked) -> {
            playerFirst = isChecked;
            prefs.edit().putBoolean(KEY_PLAYER_FIRST, isChecked).apply();
            resetBoard();
        });
    }

    private void setDifficulty(Difficulty selected) {
        if (difficulty == selected) return;
        difficulty = selected;
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_AI_DIFFICULTY, difficulty.name()).apply();
        updateDifficultyUi();
        resetBoard();
    }

    private void updateDifficultyUi() {
        styleDiffBtn(btnDifficultyEasy,   difficulty == Difficulty.EASY);
        styleDiffBtn(btnDifficultyNormal, difficulty == Difficulty.NORMAL);
        styleDiffBtn(btnDifficultyHard,   difficulty == Difficulty.HARD);
        tvDifficultyHint.setText(difficultyHint(difficulty));
        int diffColor = ContextCompat.getColor(this, difficultyColor(difficulty));
        tvDifficultyHint.setTextColor(diffColor);
        if (tvCenterDiff != null) {
            tvCenterDiff.setText(difficultyLabel(difficulty).toUpperCase());
            tvCenterDiff.setTextColor(diffColor);
        }
        if (!gameOver && playerTurn)
            tvStatus.setText("Твой ход!");
    }

    // ── Result Dialog ──────────────────────────────────────────────────────────

    /** result: 1=win, -1=lose, 0=draw */
    private void showResultDialog(int result) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sp_game_over);
        setupDialogWindow(dialog);
        dialog.setCancelable(false);

        TextView tvEmoji   = dialog.findViewById(R.id.tvSpResultEmoji);
        TextView tvTitle   = dialog.findViewById(R.id.tvSpResultTitle);
        TextView tvDiff    = dialog.findViewById(R.id.tvSpDiffLabel);
        TextView tvStreak  = dialog.findViewById(R.id.tvSpStreakBadge);
        TextView tvWins    = dialog.findViewById(R.id.tvSpWins);
        TextView tvLosses  = dialog.findViewById(R.id.tvSpLosses);
        TextView tvDraws   = dialog.findViewById(R.id.tvSpDraws);
        Button   btnHome   = dialog.findViewById(R.id.btnSpHome);
        Button   btnRematchD = dialog.findViewById(R.id.btnSpRematch);

        // Icon + title + color
        int titleColor;
        if (result > 0) {
            tvEmoji.setText("★");
            tvEmoji.setTextColor(android.graphics.Color.parseColor("#FBCE5C"));
            tvTitle.setText(R.string.result_win);
            titleColor = ContextCompat.getColor(this, R.color.accent_green);
        } else if (result < 0) {
            tvEmoji.setText("✗");
            tvEmoji.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
            tvTitle.setText(R.string.result_lose);
            titleColor = ContextCompat.getColor(this, R.color.accent_danger);
        } else {
            tvEmoji.setText("=");
            tvEmoji.setTextColor(android.graphics.Color.parseColor("#38BDF8"));
            tvTitle.setText(R.string.result_draw);
            titleColor = ContextCompat.getColor(this, R.color.game_symbol_x);
        }
        tvTitle.setTextColor(titleColor);

        // Difficulty badge
        tvDiff.setText(difficultyLabel(difficulty).toUpperCase());
        tvDiff.setTextColor(ContextCompat.getColor(this, difficultyColor(difficulty)));

        // Streak badge
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int streak = prefs.getInt(KEY_STREAK, 0);
        if (result > 0 && streak >= 2) {
            tvStreak.setText(streak + " wins in a row!");
            tvStreak.setVisibility(View.VISIBLE);
        }

        // Session stats
        tvWins.setText(String.valueOf(myWins));
        tvLosses.setText(String.valueOf(myLosses));
        tvDraws.setText(String.valueOf(myDraws));

        // Buttons
        GameButtonHelper.applyAll(btnHome, btnRematchD);
        btnHome.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        btnRematchD.setOnClickListener(v -> { dialog.dismiss(); resetBoard(); });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Entrance animation
        View decorView = dialog.getWindow().getDecorView();
        decorView.setScaleX(0.7f);
        decorView.setScaleY(0.7f);
        decorView.setAlpha(0f);
        decorView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(380)
            .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
            .start();
    }

    private void styleDiffBtn(Button btn, boolean selected) {
        btn.setAlpha(selected ? 1f : 0.6f);
        btn.setScaleX(selected ? 1.06f : 1f);
        btn.setScaleY(selected ? 1.06f : 1f);
    }

    private String difficultyLabel(Difficulty d) {
        switch (d) {
            case EASY:  return getString(R.string.diff_easy);
            case HARD:  return getString(R.string.diff_hard);
            default:    return getString(R.string.diff_normal);
        }
    }

    private String difficultyHint(Difficulty d) {
        switch (d) {
            case EASY:   return getString(R.string.diff_hint_easy);
            case HARD:   return getString(R.string.diff_hint_hard);
            default:     return getString(R.string.diff_hint_normal);
        }
    }

    private int difficultyColor(Difficulty d) {
        switch (d) {
            case EASY:  return R.color.difficulty_easy;
            case HARD:  return R.color.difficulty_hard;
            default:    return R.color.difficulty_normal;
        }
    }

    // ── AI Logic ───────────────────────────────────────────────────────────────

    private int chooseAiMove() {
        switch (difficulty) {
            case EASY:
                return randomMove();
            case NORMAL:
                if (boardSize == 3)
                    return random.nextFloat() < 0.55f ? bestMove3x3() : randomMove();
                else
                    return random.nextFloat() < 0.6f
                        ? bestMoveAB(boardSize == 4 ? 3 : 2) : randomMove();
            case HARD:
            default:
                if (boardSize == 3) return bestMove3x3();
                return bestMoveAB(boardSize == 4 ? 5 : 4);
        }
    }

    private int randomMove() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < board.length; i++)
            if (board[i] == null) available.add(i);
        return available.get(random.nextInt(available.size()));
    }

    /** Standard minimax for 3×3 (no depth limit needed). */
    private int bestMove3x3() {
        int bestScore = Integer.MIN_VALUE, move = 0;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                board[i] = AI;
                int score = minimax3(board, false, 0);
                board[i] = null;
                if (score > bestScore) { bestScore = score; move = i; }
            }
        }
        return move;
    }

    private int minimax3(String[] b, boolean isMax, int depth) {
        if (checkWinOn(b, AI))     return 10 - depth;
        if (checkWinOn(b, PLAYER)) return depth - 10;
        if (isBoardFullOn(b))      return 0;
        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < b.length; i++) if (b[i] == null) {
                b[i] = AI;
                best = Math.max(best, minimax3(b, false, depth + 1));
                b[i] = null;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < b.length; i++) if (b[i] == null) {
                b[i] = PLAYER;
                best = Math.min(best, minimax3(b, true, depth + 1));
                b[i] = null;
            }
            return best;
        }
    }

    /** Alpha-beta pruning for 4×4 / 5×5. */
    private int bestMoveAB(int maxDepth) {
        int bestScore = Integer.MIN_VALUE, move = 0;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                board[i] = AI;
                int score = alphaBeta(board, false, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, maxDepth);
                board[i] = null;
                if (score > bestScore) { bestScore = score; move = i; }
            }
        }
        return move;
    }

    private int alphaBeta(String[] b, boolean isMax, int depth, int alpha, int beta, int maxDepth) {
        if (checkWinOn(b, AI))     return 100 - depth;
        if (checkWinOn(b, PLAYER)) return depth - 100;
        if (isBoardFullOn(b) || depth >= maxDepth) return evalHeuristic(b);
        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < b.length; i++) if (b[i] == null) {
                b[i] = AI;
                best = Math.max(best, alphaBeta(b, false, depth + 1, alpha, beta, maxDepth));
                b[i] = null;
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < b.length; i++) if (b[i] == null) {
                b[i] = PLAYER;
                best = Math.min(best, alphaBeta(b, true, depth + 1, alpha, beta, maxDepth));
                b[i] = null;
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    private int evalHeuristic(String[] b) {
        int score = 0;
        for (int[] combo : winCombos) {
            int aiC = 0, plC = 0;
            for (int idx : combo) {
                if (AI.equals(b[idx]))     aiC++;
                else if (PLAYER.equals(b[idx])) plC++;
            }
            if (plC == 0) score += (int) Math.pow(10, aiC);
            if (aiC == 0) score -= (int) Math.pow(10, plC);
        }
        return score;
    }

    // ── Win / Board checks ─────────────────────────────────────────────────────

    private boolean checkWin(String s) { return checkWinOn(board, s); }

    private boolean checkWinOn(String[] b, String s) {
        for (int[] c : winCombos) {
            boolean all = true;
            for (int idx : c) { if (!s.equals(b[idx])) { all = false; break; } }
            if (all) return true;
        }
        return false;
    }

    private boolean isBoardFull()              { return isBoardFullOn(board); }
    private boolean isBoardFullOn(String[] b) {
        for (String c : b) if (c == null) return false;
        return true;
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    private void saveDifficultyStats(String outcome) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "stats_" + difficulty.name().toLowerCase() + "_" + outcome;
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
    }

    private void updateOverallStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ew = prefs.getInt("stats_easy_W", 0),   el = prefs.getInt("stats_easy_L", 0),   ed = prefs.getInt("stats_easy_D", 0);
        int nw = prefs.getInt("stats_normal_W", 0), nl = prefs.getInt("stats_normal_L", 0), nd = prefs.getInt("stats_normal_D", 0);
        int hw = prefs.getInt("stats_hard_W", 0),   hl = prefs.getInt("stats_hard_L", 0),   hd = prefs.getInt("stats_hard_D", 0);
        int totalWins  = ew + nw + hw;
        int totalGames = totalWins + (el + nl + hl) + (ed + nd + hd);
        int winRate    = totalGames == 0 ? 0 : Math.round((totalWins * 100f) / totalGames);
        int rating     = ew*10 + nw*20 + hw*30 + ed*5 + nd*10 + hd*15;
        tvOverallStats.setText(
            "Rating: " + rating + "  |  Winrate: " + winRate + "%" +
            "\nEasy " + ew+"-"+el+"-"+ed +
            "  ·  Normal " + nw+"-"+nl+"-"+nd +
            "  ·  Hard " + hw+"-"+hl+"-"+hd);
        tvHardMedals.setText("Hard wins: " + hw);
    }

    // ── Quick Settings Dialog ──────────────────────────────────────────────────

    private void showQuickSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quick_settings);
        setupDialogWindow(dialog);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        CompoundButton switchSound     = dialog.findViewById(R.id.switchDialogSound);
        CompoundButton switchBgm       = dialog.findViewById(R.id.switchDialogBgm);
        CompoundButton switchVibration = dialog.findViewById(R.id.switchDialogVibration);
        Button btnReplay      = dialog.findViewById(R.id.btnDialogReplay);
        Button btnHome        = dialog.findViewById(R.id.btnDialogHome);
        Button btnMoreGames   = dialog.findViewById(R.id.btnDialogMoreGames);
        Button btnMoreSettings = dialog.findViewById(R.id.btnDialogMoreSettings);

        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchBgm.setChecked(prefs.getBoolean(KEY_BGM, false));
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));

        switchSound.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_SOUND, c).apply());
        switchBgm.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_BGM, c).apply());
        switchVibration.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_VIBRATION, c).apply());

        GameButtonHelper.applyAll(btnReplay, btnHome, btnMoreGames, btnMoreSettings);
        btnReplay.setOnClickListener(v -> { dialog.dismiss(); resetBoard(); });
        btnHome.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        btnMoreGames.setOnClickListener(v -> { dialog.dismiss(); showMoreGamesDialog(); });
        btnMoreSettings.setOnClickListener(v -> { dialog.dismiss(); showMoreSettingsDialog(); });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showMoreGamesDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_more_games);
        setupDialogWindow(dialog);

        Button btnPlayMemory = dialog.findViewById(R.id.btnPlayMemory);
        Button btnPlaySnake  = dialog.findViewById(R.id.btnPlaySnake);
        Button btnClose      = dialog.findViewById(R.id.btnMoreGamesClose);

        GameButtonHelper.applyAll(btnPlayMemory, btnPlaySnake, btnClose);
        btnPlayMemory.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MemoryMatchActivity.class));
        });
        btnPlaySnake.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SnakeActivity.class));
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showMoreSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_more_settings);
        setupDialogWindow(dialog);

        // Share with Friends
        dialog.findViewById(R.id.rowShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Let's play Tic Tac Toe Online! 🎮");
            share.putExtra(Intent.EXTRA_TEXT,
                "🎮 Hey! Try this awesome Tic Tac Toe game!\n" +
                "Play against AI or challenge friends online with room codes.\n\n" +
                "Search 'Tic Tac Toe Online' on the Play Store!");
            startActivity(Intent.createChooser(share, "Share with Friends"));
        });

        // Rate the App → opens Play Store
        dialog.findViewById(R.id.rowRate).setOnClickListener(v -> {
            String pkg = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + pkg)));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }
        });

        // About Us → AlertDialog
        dialog.findViewById(R.id.rowAbout).setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("🎮 About Tic Tac Toe Online")
                .setMessage(
                    "Version 2.4.1\n\n" +
                    "A modern, feature-rich take on the classic game:\n\n" +
                    "• 🤖 Single Player vs AI — Easy, Normal, Hard\n" +
                    "• 🌐 Online Multiplayer — private room codes\n" +
                    "• 🃏 Memory Match bonus game\n" +
                    "• 🐍 Snake bonus game\n" +
                    "• ☁️ Cloud save with Google account\n" +
                    "• 🏆 Stats, streaks & win rate\n\n" +
                    "Made with ❤️ for players worldwide."
                )
                .setPositiveButton("Close", null)
                .show()
        );

        // Privacy Policy → AlertDialog
        dialog.findViewById(R.id.rowPrivacy).setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("🔒 Privacy Policy")
                .setMessage(
                    "We respect your privacy.\n\n" +
                    "What we collect:\n" +
                    "• Username and avatar you choose\n" +
                    "• Game statistics (wins, losses, draws)\n" +
                    "• Google account info for authentication only\n\n" +
                    "What we DON'T do:\n" +
                    "• We never sell your data\n" +
                    "• We never share data with third parties\n" +
                    "• We don't collect location or contacts\n\n" +
                    "Your data is stored securely in Firebase.\n" +
                    "Delete your account anytime via Profile → Sign Out."
                )
                .setPositiveButton("Got it", null)
                .show()
        );

        // Terms of Service → AlertDialog
        dialog.findViewById(R.id.rowTerms).setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("📄 Terms of Service")
                .setMessage(
                    "By using Tic Tac Toe Online you agree to:\n\n" +
                    "✅ Play fairly and respectfully\n" +
                    "✅ Not cheat, hack, or exploit the game\n" +
                    "✅ Not use the app for unlawful purposes\n" +
                    "✅ Accept that stats may reset for technical reasons\n\n" +
                    "We reserve the right to update these terms.\n" +
                    "Continued use means you accept any changes.\n\n" +
                    "For support: support@tictactoeonline.app"
                )
                .setPositiveButton("Got it", null)
                .show()
        );

        Button btnClose = dialog.findViewById(R.id.btnMoreSettingsClose);
        GameButtonHelper.apply(btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // ── Achievement integration ────────────────────────────────────────────────

    private void checkAchievements(boolean playerWon, long gameTimeMs) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int ew = prefs.getInt("stats_easy_W",0),   el = prefs.getInt("stats_easy_L",0);
        int nw = prefs.getInt("stats_normal_W",0), nl = prefs.getInt("stats_normal_L",0);
        int hw = prefs.getInt("stats_hard_W",0),   hl = prefs.getInt("stats_hard_L",0);
        int totalWins  = ew + nw + hw;
        int totalGames = totalWins + (el + nl + hl)
            + prefs.getInt("stats_easy_D",0)
            + prefs.getInt("stats_normal_D",0)
            + prefs.getInt("stats_hard_D",0);

        int streak = prefs.getInt(KEY_STREAK, 0);

        // Update consecutive-losses counter
        if (playerWon) {
            consecutiveLosses = 0;
        } else {
            consecutiveLosses++;
        }
        prefs.edit().putInt("consecutive_losses", consecutiveLosses).apply();

        List<Achievement> newUnlocks = achManager.checkAfterGame(
            playerWon, gameMoves, playerTotalThinkMs, streak,
            totalWins, totalGames,
            playerWon ? consecutiveLosses + 1 : consecutiveLosses,
            difficulty == Difficulty.HARD,
            playerMovesInGame
        );

        // Queue and show one by one (delayed so result dialog can show first)
        if (!newUnlocks.isEmpty()) {
            achQueue.addAll(newUnlocks);
            handler.postDelayed(this::showNextAchievement, 1400);
        }
    }

    private void showNextAchievement() {
        if (achQueue.isEmpty() || isFinishing() || isDestroyed()) return;
        Achievement a = achQueue.remove(0);
        showAchievementUnlocked(a);
    }

    private void showAchievementUnlocked(Achievement a) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_achievement_unlocked);
        setupDialogWindow(dialog);
        dialog.setCancelable(true);

        android.widget.ImageView ivIcon = dialog.findViewById(R.id.ivAchIcon);
        TextView tvTitle = dialog.findViewById(R.id.tvAchTitle);
        TextView tvDesc  = dialog.findViewById(R.id.tvAchDesc);

        ivIcon.setImageResource(a.iconRes);
        tvTitle.setText(a.title);
        tvDesc.setText(a.desc);

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Entrance animation
        View dv = dialog.getWindow().getDecorView();
        dv.setScaleX(0.6f); dv.setScaleY(0.6f); dv.setAlpha(0f);
        dv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400)
            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
            .start();

        // Vibrate on achievement
        vibrate(120);

        // Auto-dismiss + show next after 3.5s
        handler.postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
            handler.postDelayed(this::showNextAchievement, 300);
        }, 3500);

        // Tap to dismiss early
        dv.setOnClickListener(v -> {
            if (dialog.isShowing()) dialog.dismiss();
            handler.postDelayed(this::showNextAchievement, 300);
        });
    }

    /** Sets up dialog window: transparent bg + proper dim behind. */
    private void setupDialogWindow(Dialog dialog) {
        if (dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.75f);
    }
}
