package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class SnakeActivity extends BaseGameActivity {

    private static final String PREFS_NAME   = "tictactoe_prefs";
    private static final String KEY_SNAKE_BEST = "snake_best_score";

    private SnakeGameView snakeView;
    private TextView tvScore, tvBest, tvStatus;
    private Button btnStart;
    private int bestScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake);

        snakeView = findViewById(R.id.snakeGameView);
        tvScore   = findViewById(R.id.tvSnakeScore);
        tvBest    = findViewById(R.id.tvSnakeBest);
        tvStatus  = findViewById(R.id.tvSnakeStatus);
        btnStart  = findViewById(R.id.btnSnakeStart);

        Button btnBack  = findViewById(R.id.btnSnakeBack);
        Button btnUp    = findViewById(R.id.btnSnakeUp);
        Button btnDown  = findViewById(R.id.btnSnakeDown);
        Button btnLeft  = findViewById(R.id.btnSnakeLeft);
        Button btnRight = findViewById(R.id.btnSnakeRight);

        GameButtonHelper.apply(btnBack);
        GameButtonHelper.applyAll(btnUp, btnDown, btnLeft, btnRight, btnStart);
        btnBack.setOnClickListener(v -> {
            snakeView.stopGame();
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        loadBest();

        snakeView.setCallback(new SnakeGameView.Callback() {
            @Override public void onScore(int score) {
                runOnUiThread(() -> tvScore.setText(String.valueOf(score)));
            }

            @Override public void onGameOver(int score) {
                runOnUiThread(() -> {
                    if (score > bestScore) {
                        bestScore = score;
                        saveBest();
                        tvBest.setText("Best: " + bestScore);
                    }
                    btnStart.setText("RESTART");
                    tvStatus.setText("Game Over! Score: " + score);
                    tvStatus.setAlpha(1f);
                });
            }
        });

        btnStart.setOnClickListener(v -> {
            tvScore.setText("0");
            tvStatus.setText("");
            tvStatus.setAlpha(0f);
            btnStart.setText("RESTART");
            snakeView.startGame();
        });

        btnUp.setOnClickListener(v    -> snakeView.changeDir(SnakeGameView.Dir.UP));
        btnDown.setOnClickListener(v  -> snakeView.changeDir(SnakeGameView.Dir.DOWN));
        btnLeft.setOnClickListener(v  -> snakeView.changeDir(SnakeGameView.Dir.LEFT));
        btnRight.setOnClickListener(v -> snakeView.changeDir(SnakeGameView.Dir.RIGHT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.stopGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        snakeView.stopGame();
    }

    private void loadBest() {
        bestScore = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SNAKE_BEST, 0);
        tvBest.setText("Best: " + bestScore);
    }

    private void saveBest() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SNAKE_BEST, bestScore).apply();
    }
}
