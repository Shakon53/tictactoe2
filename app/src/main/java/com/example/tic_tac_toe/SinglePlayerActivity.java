package com.example.tic_tac_toe;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SinglePlayerActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "tictactoe_prefs";
    private static final String KEY_STREAK    = "win_streak";
    private static final String KEY_BEST      = "best_streak";
    private static final String KEY_VIBRATION = "pref_vibration";
    private static final String KEY_SOUND     = "pref_sound";
    private static final String KEY_BGM       = "pref_bgm";
    private static final String KEY_AI_DIFFICULTY = "pref_ai_difficulty";
    private static final String KEY_PLAYER_FIRST = "pref_player_first";

    private static final String PLAYER     = "X";
    private static final String AI         = "O";
    private static final int    AI_DELAY   = 450;

    private enum Difficulty {
        EASY,
        NORMAL,
        HARD
    }

    private static final int[][] WIN_COMBOS = {
        {0,1,2},{3,4,5},{6,7,8},
        {0,3,6},{1,4,7},{2,5,8},
        {0,4,8},{2,4,6}
    };

    private final String[] board = new String[9];
    private Button[]  cells;
    private TextView  tvStatus, tvMyScore, tvAiScore, tvMySymbol, tvAiSymbol;
    private TextView tvDifficultyHint;
    private TextView tvOverallStats;
    private TextView tvHardMedals;
    private CompoundButton switchPlayerFirst;
    private Button btnRematch;
    private Button btnQuickSettings;

    private boolean gameOver   = false;
    private boolean playerTurn = true;
    private boolean playerFirst = true;

    private int myWins, myLosses, myDraws;
    private int aiWins, aiLosses, aiDraws;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Vibrator      vibrator;
    private ToneGenerator toneGen;
    private final Random random = new Random();
    private Difficulty difficulty = Difficulty.NORMAL;
    private Button btnDifficultyEasy;
    private Button btnDifficultyNormal;
    private Button btnDifficultyHard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 80); }
        catch (Exception ignored) {}

        tvStatus   = findViewById(R.id.tvGameStatus);
        tvMyScore  = findViewById(R.id.tvMyScore);
        tvAiScore  = findViewById(R.id.tvAiScore);
        tvMySymbol = findViewById(R.id.tvMySymbol);
        tvAiSymbol = findViewById(R.id.tvAiSymbol);
        tvDifficultyHint = findViewById(R.id.tvDifficultyHint);
        tvOverallStats = findViewById(R.id.tvOverallStats);
        tvHardMedals = findViewById(R.id.tvHardMedals);
        switchPlayerFirst = findViewById(R.id.switchPlayerFirst);
        btnRematch = findViewById(R.id.btnRematch);
        btnQuickSettings = findViewById(R.id.btnQuickSettings);
        btnDifficultyEasy = findViewById(R.id.btnDifficultyEasy);
        btnDifficultyNormal = findViewById(R.id.btnDifficultyNormal);
        btnDifficultyHard = findViewById(R.id.btnDifficultyHard);

        tvMySymbol.setText(PLAYER);
        tvMySymbol.setTextColor(0xFFFBBF24);
        tvAiSymbol.setText(AI);
        tvAiSymbol.setTextColor(0xFF38BDF8);

        cells = new Button[]{
            findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
            findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
            findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8)
        };

        for (int i = 0; i < 9; i++) {
            final int idx = i;
            cells[i].setOnClickListener(v -> onCellClick(idx));
        }

        Button btnBack = findViewById(R.id.btnBack);
        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnRematch);
        GameButtonHelper.apply(btnQuickSettings);
        btnBack.setOnClickListener(v -> finish());
        btnRematch.setOnClickListener(v -> resetBoard());
        btnQuickSettings.setOnClickListener(v -> showQuickSettingsDialog());
        setupDifficultyButtons();
        setupPlayerOrderSwitch();
        updateOverallStats();
        resetBoard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (toneGen != null) toneGen.release();
    }

    // ── Gameplay ─────────────────────────────────────────────────────────────

    private void onCellClick(int idx) {
        if (gameOver || !playerTurn || board[idx] != null) return;

        playMove();
        makeMove(idx, PLAYER);

        if (checkWin(PLAYER)) { endGame(true); return; }
        if (isBoardFull())    { endDraw();     return; }

        playerTurn = false;
        tvStatus.setText("AI думает...");
        handler.postDelayed(this::aiMove, AI_DELAY);
    }

    private void aiMove() {
        playMove();
        makeMove(chooseAiMove(), AI);

        if (checkWin(AI)) { endGame(false); return; }
        if (isBoardFull()) { endDraw();     return; }

        playerTurn = true;
        tvStatus.setText("Твой ход! (" + difficultyLabel(difficulty) + ")");
    }

    private void makeMove(int idx, String symbol) {
        board[idx] = symbol;
        cells[idx].setText(symbol);
        cells[idx].setTextColor(symbol.equals(PLAYER) ? 0xFFFBBF24 : 0xFF38BDF8);

        ObjectAnimator pop = ObjectAnimator.ofPropertyValuesHolder(cells[idx],
            PropertyValuesHolder.ofFloat("scaleX", 0.55f, 1.12f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.55f, 1.12f, 1f));
        pop.setDuration(240);
        pop.start();
    }

    private void endGame(boolean playerWon) {
        gameOver = true;
        highlightWin(playerWon ? PLAYER : AI);

        if (playerWon) {
            myWins++; aiLosses++;
            tvStatus.setText("Ты победил! 🎉");
            playWin();
            saveStreak(true);
            saveDifficultyStats("W");
        } else {
            myLosses++; aiWins++;
            tvStatus.setText("ИИ победил!");
            playLose();
            saveStreak(false);
            saveDifficultyStats("L");
        }
        updateScores();
        updateOverallStats();
        btnRematch.setVisibility(Button.VISIBLE);
    }

    private void endDraw() {
        gameOver = true;
        myDraws++; aiDraws++;
        tvStatus.setText("Ничья!");
        playDraw();
        saveDifficultyStats("D");
        updateScores();
        updateOverallStats();
        btnRematch.setVisibility(Button.VISIBLE);
    }

    private void resetBoard() {
        handler.removeCallbacks(this::aiMove);
        for (int i = 0; i < 9; i++) {
            board[i] = null;
            cells[i].setText("");
            cells[i].setBackground(
                ContextCompat.getDrawable(this, R.drawable.game_cell_transparent));
        }
        gameOver   = false;
        btnRematch.setVisibility(Button.GONE);
        playerTurn = playerFirst;
        if (playerTurn) {
            tvStatus.setText("Твой ход! (" + difficultyLabel(difficulty) + ")");
        } else {
            tvStatus.setText("AI начинает...");
            handler.postDelayed(this::aiMove, AI_DELAY);
        }
    }

    private void highlightWin(String symbol) {
        for (int[] c : WIN_COMBOS) {
            if (symbol.equals(board[c[0]]) &&
                symbol.equals(board[c[1]]) &&
                symbol.equals(board[c[2]])) {
                for (int idx : c) {
                    cells[idx].setBackground(
                        ContextCompat.getDrawable(this, R.drawable.cell_win));
                    ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(cells[idx],
                        PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f));
                    pulse.setDuration(200);
                    pulse.setRepeatCount(3);
                    pulse.setRepeatMode(ValueAnimator.REVERSE);
                    pulse.start();
                }
                return;
            }
        }
    }

    private void updateScores() {
        tvMyScore.setText(myWins + "W " + myLosses + "L " + myDraws + "D");
        tvAiScore.setText(aiWins + "W " + aiLosses + "L " + aiDraws + "D");
    }

    private void saveStreak(boolean won) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int streak = prefs.getInt(KEY_STREAK, 0);
        int best   = prefs.getInt(KEY_BEST, 0);
        streak = won ? streak + 1 : 0;
        if (streak > best) best = streak;
        prefs.edit().putInt(KEY_STREAK, streak).putInt(KEY_BEST, best).apply();
    }

    // ── Vibration & Sound ────────────────────────────────────────────────────

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
        } else {
            vibrator.vibrate(ms);
        }
    }

    private void vibratePattern(long[] pattern) {
        if (!vibrationOn() || vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private void playTone(int tone, int durationMs) {
        if (!soundOn() || toneGen == null) return;
        try { toneGen.startTone(tone, durationMs); } catch (Exception ignored) {}
    }

    /** Короткий клик — ход сделан */
    private void playMove() {
        vibrate(30);
        playTone(ToneGenerator.TONE_PROP_BEEP, 60);
    }

    /** Победа — длинная вибрация + восходящий тон */
    private void playWin() {
        vibratePattern(new long[]{0, 80, 60, 120});
        playTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 500);
    }

    /** Поражение — двойной толчок + низкий тон */
    private void playLose() {
        vibratePattern(new long[]{0, 60, 60, 60});
        playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 400);
    }

    /** Ничья — мягкая вибрация */
    private void playDraw() {
        vibrate(80);
        playTone(ToneGenerator.TONE_PROP_NACK, 300);
    }

    // ── Difficulty & Minimax ────────────────────────────────────────────────

    private void setupDifficultyButtons() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_AI_DIFFICULTY, Difficulty.NORMAL.name());
        try {
            difficulty = Difficulty.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            difficulty = Difficulty.NORMAL;
        }

        GameButtonHelper.applyAll(btnDifficultyEasy, btnDifficultyNormal, btnDifficultyHard);
        btnDifficultyEasy.setOnClickListener(v -> setDifficulty(Difficulty.EASY));
        btnDifficultyNormal.setOnClickListener(v -> setDifficulty(Difficulty.NORMAL));
        btnDifficultyHard.setOnClickListener(v -> setDifficulty(Difficulty.HARD));
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
            .edit()
            .putString(KEY_AI_DIFFICULTY, difficulty.name())
            .apply();
        updateDifficultyUi();
        resetBoard();
    }

    private void updateDifficultyUi() {
        styleDifficultyButton(btnDifficultyEasy, difficulty == Difficulty.EASY);
        styleDifficultyButton(btnDifficultyNormal, difficulty == Difficulty.NORMAL);
        styleDifficultyButton(btnDifficultyHard, difficulty == Difficulty.HARD);
        tvDifficultyHint.setText(difficultyHint(difficulty));
        tvDifficultyHint.setTextColor(ContextCompat.getColor(this, difficultyColor(difficulty)));
        if (!gameOver && playerTurn) {
            tvStatus.setText("Твой ход! (" + difficultyLabel(difficulty) + ")");
        }
    }

    private void styleDifficultyButton(Button button, boolean selected) {
        button.setAlpha(selected ? 1f : 0.65f);
        button.setScaleX(selected ? 1.04f : 1f);
        button.setScaleY(selected ? 1.04f : 1f);
    }

    private String difficultyLabel(Difficulty d) {
        switch (d) {
            case EASY:
                return "Easy";
            case HARD:
                return "Hard";
            default:
                return "Normal";
        }
    }

    private String difficultyHint(Difficulty d) {
        switch (d) {
            case EASY:
                return "Easy: random and forgiving AI";
            case HARD:
                return "Hard: strongest minimax AI";
            default:
                return "Normal: balanced AI behavior";
        }
    }

    private int difficultyColor(Difficulty d) {
        switch (d) {
            case EASY:
                return R.color.difficulty_easy;
            case HARD:
                return R.color.difficulty_hard;
            default:
                return R.color.difficulty_normal;
        }
    }

    private int chooseAiMove() {
        switch (difficulty) {
            case EASY:
                return randomMove();
            case NORMAL:
                return random.nextFloat() < 0.55f ? bestMove() : randomMove();
            case HARD:
            default:
                return bestMove();
        }
    }

    private int randomMove() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                available.add(i);
            }
        }
        return available.get(random.nextInt(available.size()));
    }

    private void saveDifficultyStats(String outcome) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "stats_" + difficulty.name().toLowerCase() + "_" + outcome;
        int value = prefs.getInt(key, 0);
        prefs.edit().putInt(key, value + 1).apply();
    }

    private void updateOverallStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ew = prefs.getInt("stats_easy_W", 0);
        int el = prefs.getInt("stats_easy_L", 0);
        int ed = prefs.getInt("stats_easy_D", 0);
        int nw = prefs.getInt("stats_normal_W", 0);
        int nl = prefs.getInt("stats_normal_L", 0);
        int nd = prefs.getInt("stats_normal_D", 0);
        int hw = prefs.getInt("stats_hard_W", 0);
        int hl = prefs.getInt("stats_hard_L", 0);
        int hd = prefs.getInt("stats_hard_D", 0);

        int totalWins = ew + nw + hw;
        int totalGames = totalWins + (el + nl + hl) + (ed + nd + hd);
        int winRate = totalGames == 0 ? 0 : Math.round((totalWins * 100f) / totalGames);
        int rating = ew * 10 + nw * 20 + hw * 30 + ed * 5 + nd * 10 + hd * 15;
        int medals = hw / 100;

        String stats = "Rating: " + rating +
            "  |  Winrate: " + winRate + "%" +
            "\nEasy " + ew + "-" + el + "-" + ed +
            "  ·  Normal " + nw + "-" + nl + "-" + nd +
            "  ·  Hard " + hw + "-" + hl + "-" + hd;
        tvOverallStats.setText(stats);
        tvHardMedals.setText("🏅 x" + medals);
    }

    private void showQuickSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quick_settings);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        CompoundButton switchSound = dialog.findViewById(R.id.switchDialogSound);
        CompoundButton switchBgm = dialog.findViewById(R.id.switchDialogBgm);
        CompoundButton switchVibration = dialog.findViewById(R.id.switchDialogVibration);
        Button btnReplay = dialog.findViewById(R.id.btnDialogReplay);
        Button btnHome = dialog.findViewById(R.id.btnDialogHome);

        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchBgm.setChecked(prefs.getBoolean(KEY_BGM, false));
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply());
        switchBgm.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(KEY_BGM, isChecked).apply());
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply());

        GameButtonHelper.applyAll(btnReplay, btnHome);
        btnReplay.setOnClickListener(v -> {
            dialog.dismiss();
            resetBoard();
        });
        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private int bestMove() {
        int bestScore = Integer.MIN_VALUE, move = 0;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = AI;
                int score = minimax(board, false, 0);
                board[i] = null;
                if (score > bestScore) { bestScore = score; move = i; }
            }
        }
        return move;
    }

    private int minimax(String[] b, boolean isMax, int depth) {
        if (checkWinOn(b, AI))     return 10 - depth;
        if (checkWinOn(b, PLAYER)) return depth - 10;
        if (isBoardFullOn(b))      return 0;

        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (b[i] == null) {
                    b[i] = AI;
                    best = Math.max(best, minimax(b, false, depth + 1));
                    b[i] = null;
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (b[i] == null) {
                    b[i] = PLAYER;
                    best = Math.min(best, minimax(b, true, depth + 1));
                    b[i] = null;
                }
            }
            return best;
        }
    }

    private boolean checkWin(String s)                  { return checkWinOn(board, s); }
    private boolean checkWinOn(String[] b, String s) {
        for (int[] c : WIN_COMBOS)
            if (s.equals(b[c[0]]) && s.equals(b[c[1]]) && s.equals(b[c[2]])) return true;
        return false;
    }
    private boolean isBoardFull()              { return isBoardFullOn(board); }
    private boolean isBoardFullOn(String[] b) {
        for (String c : b) if (c == null) return false;
        return true;
    }
}
