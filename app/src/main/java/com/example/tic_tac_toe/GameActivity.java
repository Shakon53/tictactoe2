package com.example.tic_tac_toe;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.tic_tac_toe.network.SocketClient;
import com.example.tic_tac_toe.network.SocketHolder;

public class GameActivity extends BaseGameActivity implements SocketClient.SocketListener {

    private static final int TURN_SECONDS = 30;
    private static final String PREFS_NAME = "tictactoe_prefs";
    private static final String KEY_TIMER = "pref_timer";

    private static final int[][] WIN_COMBOS = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    private TextView tvRoomCode;
    private TextView tvGameStatus;
    private TextView tvMyName;
    private TextView tvMySymbol;
    private TextView tvMyScore;
    private TextView tvMyStats;
    private TextView tvOppName;
    private TextView tvOppSymbol;
    private TextView tvOppScore;
    private TextView tvOppStats;
    private TextView tvTimer;
    private FrameLayout timerPanel;
    private CircularProgressIndicator circularTimer;
    private Button[] boardButtons;

    private SocketClient socketClient;
    private String mySymbol = "X";
    private String opponentSymbol = "O";
    private String myName = "You";
    private String opponentName = "Opponent";
    private boolean myTurn = false;
    private boolean gameFinished = false;
    private ObjectAnimator turnPulseAnimator;
    private CountDownTimer countDownTimer;
    private boolean timerEnabled = true;

    private final String[] boardState = new String[9];

    private int myWins = 0, myLosses = 0, myDraws = 0;
    private int oppWins = 0, oppLosses = 0, oppDraws = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvGameStatus = findViewById(R.id.tvGameStatus);
        tvMyName = findViewById(R.id.tvMyName);
        tvMySymbol = findViewById(R.id.tvMySymbol);
        tvMyScore = findViewById(R.id.tvMyScore);
        tvMyStats = findViewById(R.id.tvMyStats);
        tvOppName = findViewById(R.id.tvOppName);
        tvOppSymbol = findViewById(R.id.tvOppSymbol);
        tvOppScore = findViewById(R.id.tvOppScore);
        tvOppStats = findViewById(R.id.tvOppStats);
        tvTimer = findViewById(R.id.tvTimer);
        timerPanel = findViewById(R.id.timerPanel);
        circularTimer = findViewById(R.id.circularTimer);

        Button btnBack = findViewById(R.id.btnBackToMenu);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });
        GameButtonHelper.apply(btnBack);

        myName = getIntent().getStringExtra("username");
        String roomCode = getIntent().getStringExtra("roomCode");
        String incomingSymbol = getIntent().getStringExtra("mySymbol");

        if (incomingSymbol != null && !incomingSymbol.isEmpty()) {
            mySymbol = incomingSymbol;
            opponentSymbol = mySymbol.equals("X") ? "O" : "X";
        }
        if (roomCode != null) tvRoomCode.setText(roomCode);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        timerEnabled = prefs.getBoolean(KEY_TIMER, true);

        initBoard();
        updatePlayerUI();
        tvGameStatus.setText("Waiting for opponent...");

        animateEntrance();

        socketClient = SocketHolder.get();
        if (socketClient == null || !socketClient.isConnected()) {
            Toast.makeText(this, "No active connection", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        socketClient.setListener(this);
    }

    private void animateEntrance() {
        View playerPanel = findViewById(R.id.playerPanel);
        View statusRow = findViewById(R.id.statusRow);
        View gameBoard = findViewById(R.id.gameBoard);

        View[] views = {playerPanel, statusRow, gameBoard};
        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setAlpha(0f);
            views[i].setTranslationY(60f);
            views[i].animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(80L + i * 100L)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();
        }
    }

    private void initBoard() {
        boardButtons = new Button[9];
        int[] ids = {
            R.id.btn0, R.id.btn1, R.id.btn2,
            R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8
        };
        for (int i = 0; i < 9; i++) {
            boardState[i] = "";
            boardButtons[i] = findViewById(ids[i]);
            final int index = i;
            boardButtons[i].setOnClickListener(v -> onCellClicked(index));
        }
    }

    private void updatePlayerUI() {
        if (myName != null) tvMyName.setText(myName);
        tvMySymbol.setText(mySymbol);
        tvMySymbol.setTextColor(symbolColor(mySymbol));
        tvOppName.setText(opponentName);
        tvOppSymbol.setText(opponentSymbol);
        tvOppSymbol.setTextColor(symbolColor(opponentSymbol));
        refreshScores();
    }

    private void refreshScores() {
        tvMyScore.setText(String.valueOf(myWins));
        if (tvMyStats != null) tvMyStats.setText(myLosses + "L  " + myDraws + "D");
        tvOppScore.setText(String.valueOf(oppWins));
        if (tvOppStats != null) tvOppStats.setText(oppLosses + "L  " + oppDraws + "D");
    }

    private int symbolColor(String symbol) {
        return ContextCompat.getColor(this,
            "X".equals(symbol) ? R.color.game_symbol_x : R.color.game_symbol_o);
    }

    private void setCellSymbol(Button btn, String symbol) {
        btn.setText(symbol);
        btn.setTextColor(symbolColor(symbol));
        btn.setScaleX(0f);
        btn.setScaleY(0f);
        btn.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(350)
            .setInterpolator(new OvershootInterpolator(1.8f))
            .start();
        btn.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
    }

    private void onCellClicked(int index) {
        if (gameFinished || !myTurn || !boardState[index].isEmpty()) return;
        boardState[index] = mySymbol;
        setCellSymbol(boardButtons[index], mySymbol);
        setTurnState(false);
        tvGameStatus.setText("Opponent's turn...");
        socketClient.sendMessage("MOVE|" + index);
    }

    @Override
    public void onConnected() {}

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("START_GAME|")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 2 && !parts[1].isEmpty()) opponentName = parts[1];
            updatePlayerUI();
            tvGameStatus.setText("Game started!");
        } else if (message.equals("YOUR_TURN")) {
            setTurnState(true);
        } else if (message.startsWith("OPPONENT_MOVED|")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 2) {
                try {
                    int index = Integer.parseInt(parts[1]);
                    if (index >= 0 && index < 9) {
                        boardState[index] = opponentSymbol;
                        setCellSymbol(boardButtons[index], opponentSymbol);
                    } else {
                        onError("Invalid move index: " + index);
                    }
                } catch (NumberFormatException e) {
                    onError("Invalid move format from server");
                }
            }
            setTurnState(true);
        } else if (message.equals("WIN")) {
            myWins++; oppLosses++;
            saveStreak(true);
            highlightWinLine(mySymbol);
            finishGame("You Win!");
        } else if (message.equals("LOSE")) {
            myLosses++; oppWins++;
            saveStreak(false);
            highlightWinLine(opponentSymbol);
            finishGame("You Lose!");
        } else if (message.equals("DRAW")) {
            myDraws++; oppDraws++;
            finishGame("Draw!");
        } else if (message.startsWith("ERROR|")) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void highlightWinLine(String symbol) {
        for (int[] combo : WIN_COMBOS) {
            if (boardState[combo[0]].equals(symbol)
                    && boardState[combo[1]].equals(symbol)
                    && boardState[combo[2]].equals(symbol)) {
                for (int idx : combo) {
                    boardButtons[idx].setBackground(
                        ContextCompat.getDrawable(this, R.drawable.cell_win));
                    ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(
                        boardButtons[idx],
                        PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f));
                    anim.setDuration(180);
                    anim.setRepeatCount(4);
                    anim.setRepeatMode(ValueAnimator.REVERSE);
                    anim.start();
                }
                return;
            }
        }
    }

    @Override
    public void onError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected() {
        if (!gameFinished) {
            Toast.makeText(this, "Disconnected from server", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setTurnState(boolean isMyTurn) {
        myTurn = isMyTurn;
        stopTimer();
        if (isMyTurn) {
            tvGameStatus.setText("Your Turn!");
            tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.game_symbol_x));
            startPulse();
            if (timerEnabled) {
                startTimer();
            } else {
                timerPanel.setVisibility(View.GONE);
            }
        } else {
            tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            stopPulse();
            timerPanel.setVisibility(View.GONE);
        }
    }

    private void startTimer() {
        timerPanel.setVisibility(View.VISIBLE);
        tvTimer.setText(String.valueOf(TURN_SECONDS));
        tvTimer.setTextColor(ContextCompat.getColor(this, R.color.neon_orange));
        if (circularTimer != null) {
            circularTimer.setMax(TURN_SECONDS);
            circularTimer.setProgressCompat(TURN_SECONDS, false);
            circularTimer.setIndicatorColor(ContextCompat.getColor(this, R.color.neon_orange));
        }

        countDownTimer = new CountDownTimer(TURN_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secsLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText(String.valueOf(secsLeft));
                if (circularTimer != null) circularTimer.setProgressCompat(secsLeft, true);
                if (secsLeft <= 10) {
                    int danger = ContextCompat.getColor(GameActivity.this, R.color.accent_danger);
                    tvTimer.setTextColor(danger);
                    if (circularTimer != null) circularTimer.setIndicatorColor(danger);
                    tvTimer.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120)
                        .withEndAction(() -> tvTimer.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                        .start();
                }
            }

            @Override
            public void onFinish() {
                timerPanel.setVisibility(View.GONE);
                if (myTurn && !gameFinished) {
                    tvGameStatus.setText("Time's up!");
                    tvGameStatus.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.accent_danger));
                    stopPulse();
                    myTurn = false;
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void startPulse() {
        if (turnPulseAnimator == null) {
            turnPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                tvGameStatus,
                PropertyValuesHolder.ofFloat("scaleX", 1.08f),
                PropertyValuesHolder.ofFloat("scaleY", 1.08f));
            turnPulseAnimator.setDuration(550);
            turnPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            turnPulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        }
        if (!turnPulseAnimator.isStarted()) turnPulseAnimator.start();
    }

    private void stopPulse() {
        if (turnPulseAnimator != null) {
            turnPulseAnimator.cancel();
            tvGameStatus.setScaleX(1f);
            tvGameStatus.setScaleY(1f);
        }
    }

    private void finishGame(String resultText) {
        gameFinished = true;
        stopTimer();
        stopPulse();
        refreshScores();
        tvGameStatus.setText(resultText);
        timerPanel.setVisibility(View.GONE);
        for (Button btn : boardButtons) btn.setEnabled(false);
        vibrateResult(resultText);
        tvGameStatus.postDelayed(() -> showGameOverDialog(resultText), 700);
    }

    private void saveStreak(boolean won) {
        SharedPreferences prefs = getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE);
        int streak = prefs.getInt("win_streak", 0);
        int best = prefs.getInt("best_streak", 0);
        streak = won ? streak + 1 : 0;
        if (streak > best) best = streak;
        prefs.edit().putInt("win_streak", streak).putInt("best_streak", best).apply();
    }

    private void vibrateResult(String result) {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        if (result.contains("Win")) {
            v.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 60, 120}, -1));
        } else if (result.contains("Lose")) {
            v.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void showGameOverDialog(String resultText) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_over);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.75f);
        }

        boolean isWin = resultText.contains("Win");
        boolean isDraw = resultText.contains("Draw");

        TextView tvIcon = dialog.findViewById(R.id.tvGameOverIcon);
        TextView tvTitle = dialog.findViewById(R.id.tvGameOverTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvGameOverMessage);
        Button btnOk = dialog.findViewById(R.id.btnGameOverOk);

        tvIcon.setText(isWin ? "W" : isDraw ? "D" : "L");
        int resultColor = ContextCompat.getColor(this,
            isWin ? R.color.accent_green : isDraw ? R.color.game_symbol_x : R.color.accent_danger);
        tvIcon.setTextColor(resultColor);
        tvTitle.setText(isWin ? "VICTORY!" : isDraw ? "DRAW!" : "DEFEAT!");
        tvTitle.setTextColor(resultColor);
        tvMessage.setText(myWins + "W  ·  " + myLosses + "L  ·  " + myDraws + "D");

        GameButtonHelper.apply(btnOk);
        btnOk.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        dialog.setCancelable(false);
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View decorView = dialog.getWindow().getDecorView();
        decorView.setScaleX(0.6f);
        decorView.setScaleY(0.6f);
        decorView.setAlpha(0f);
        decorView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(350)
            .setInterpolator(new OvershootInterpolator(1.4f))
            .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (socketClient != null) {
            socketClient.setListener(null);
        }
        if (isFinishing()) {
            SocketClient client = SocketHolder.get();
            if (client != null) client.disconnect();
            SocketHolder.clear();
        }
    }
}
