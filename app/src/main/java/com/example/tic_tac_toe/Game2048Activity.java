package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game2048Activity extends BaseGameActivity {

    private static final String PREFS_NAME    = "tictactoe_prefs";
    private static final String KEY_BEST_2048 = "best_score_2048";
    private static final int    SIZE          = 4;
    private static final float  SWIPE_MIN     = 80f;

    private final int[][] board = new int[SIZE][SIZE];
    private final TextView[][] cells = new TextView[SIZE][SIZE];
    private int score = 0;
    private int bestScore = 0;
    private boolean gameOver = false;
    private boolean won = false;

    private TextView tvScore, tvBest;
    private GestureDetector gestureDetector;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2048);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bestScore = prefs.getInt(KEY_BEST_2048, 0);

        tvScore = findViewById(R.id.tv2048Score);
        tvBest  = findViewById(R.id.tv2048Best);
        tvBest.setText(String.valueOf(bestScore));

        Button btnBack = findViewById(R.id.btn2048Back);
        Button btnNew  = findViewById(R.id.btn2048New);
        GameButtonHelper.apply(btnBack);
        GameButtonHelper.apply(btnNew);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });
        btnNew.setOnClickListener(v -> startNewGame());

        buildGrid();
        setupSwipe();
        startNewGame();
    }

    private void buildGrid() {
        GridLayout grid = findViewById(R.id.grid2048);
        grid.setRowCount(SIZE);
        grid.setColumnCount(SIZE);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                TextView tv = new TextView(this);
                tv.setGravity(Gravity.CENTER);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(26f);
                tv.setBackgroundColor(0xFF3C3A5E);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1, GridLayout.FILL, 1f),
                        GridLayout.spec(c, 1, GridLayout.FILL, 1f)
                );
                lp.width  = 0;
                lp.height = 0;
                lp.setMargins(5, 5, 5, 5);
                tv.setLayoutParams(lp);

                grid.addView(tv);
                cells[r][c] = tv;
            }
        }
    }

    private void setupSwipe() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) { return true; }

            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2,
                                   float vx, float vy) {
                if (e1 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (Math.abs(dx) > SWIPE_MIN) {
                        doSwipe(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                        return true;
                    }
                } else {
                    if (Math.abs(dy) > SWIPE_MIN) {
                        doSwipe(dy > 0 ? Direction.DOWN : Direction.UP);
                        return true;
                    }
                }
                return false;
            }
        });

        View touch = findViewById(R.id.touch2048);
        touch.setOnTouchListener((v, e) -> gestureDetector.onTouchEvent(e));
    }

    // ── Game Logic ────────────────────────────────────────────────────────────

    private void startNewGame() {
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) board[r][c] = 0;
        score = 0;
        gameOver = false;
        won = false;
        tvScore.setText("0");
        spawnTile();
        spawnTile();
        renderBoard();
    }

    private void spawnTile() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] == 0) empty.add(new int[]{r, c});
        if (empty.isEmpty()) return;
        int[] pos = empty.get(new Random().nextInt(empty.size()));
        board[pos[0]][pos[1]] = (Math.random() < 0.9) ? 2 : 4;
    }

    enum Direction { UP, DOWN, LEFT, RIGHT }

    private void doSwipe(Direction dir) {
        if (gameOver) return;
        int[][] snap = copyBoard();
        switch (dir) {
            case LEFT:  moveLeft();  break;
            case RIGHT: moveRight(); break;
            case UP:    moveUp();    break;
            case DOWN:  moveDown();  break;
        }
        if (!boardsEqual(snap, board)) {
            spawnTile();
            renderBoard();
            if (!won && checkWin()) {
                won = true;
                showWinDialog();
            } else if (checkLose()) {
                gameOver = true;
                showLoseDialog();
            }
        }
    }

    private void moveLeft()  { for (int r=0; r<SIZE; r++) setRow(r, slide(getRow(r))); }
    private void moveRight() { for (int r=0; r<SIZE; r++) setRow(r, rev(slide(rev(getRow(r))))); }
    private void moveUp()    { for (int c=0; c<SIZE; c++) setCol(c, slide(getCol(c))); }
    private void moveDown()  { for (int c=0; c<SIZE; c++) setCol(c, rev(slide(rev(getCol(c))))); }

    private int[] slide(int[] line) {
        int[] t = new int[SIZE];
        int idx = 0;
        for (int v : line) if (v != 0) t[idx++] = v;
        for (int i = 0; i < SIZE - 1; i++) {
            if (t[i] != 0 && t[i] == t[i+1]) {
                t[i] *= 2;
                score += t[i];
                t[i+1] = 0;
            }
        }
        int[] r = new int[SIZE];
        idx = 0;
        for (int v : t) if (v != 0) r[idx++] = v;
        return r;
    }

    private int[] getRow(int r) {
        int[] row = new int[SIZE];
        for (int c = 0; c < SIZE; c++) row[c] = board[r][c];
        return row;
    }
    private void setRow(int r, int[] row) { for (int c=0;c<SIZE;c++) board[r][c]=row[c]; }
    private int[] getCol(int c) {
        int[] col = new int[SIZE];
        for (int r = 0; r < SIZE; r++) col[r] = board[r][c];
        return col;
    }
    private void setCol(int c, int[] col) { for (int r=0;r<SIZE;r++) board[r][c]=col[r]; }
    private int[] rev(int[] a) {
        int[] r = new int[a.length];
        for (int i=0;i<a.length;i++) r[i]=a[a.length-1-i];
        return r;
    }
    private int[][] copyBoard() {
        int[][] c = new int[SIZE][SIZE];
        for (int r=0;r<SIZE;r++) c[r]=board[r].clone();
        return c;
    }
    private boolean boardsEqual(int[][] a, int[][] b) {
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) if(a[r][c]!=b[r][c]) return false;
        return true;
    }
    private boolean checkWin() {
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) if(board[r][c]==2048) return true;
        return false;
    }
    private boolean checkLose() {
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) if(board[r][c]==0) return false;
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) {
            if (r<SIZE-1 && board[r][c]==board[r+1][c]) return false;
            if (c<SIZE-1 && board[r][c]==board[r][c+1]) return false;
        }
        return true;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderBoard() {
        tvScore.setText(String.valueOf(score));
        if (score > bestScore) {
            bestScore = score;
            tvBest.setText(String.valueOf(bestScore));
            prefs.edit().putInt(KEY_BEST_2048, bestScore).apply();
        }
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int val = board[r][c];
                TextView tv = cells[r][c];
                tv.setText(val == 0 ? "" : String.valueOf(val));
                tv.setBackgroundColor(tileColor(val));
                tv.setTextColor(val > 4 ? 0xFFF9F6F2 : 0xFF776E65);
                float ts = val >= 1000 ? 18f : val >= 100 ? 22f : 26f;
                tv.setTextSize(ts);

                // pop animation on non-zero tiles
                if (val != 0) {
                    tv.setScaleX(0.85f); tv.setScaleY(0.85f);
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                }
            }
        }
    }

    private int tileColor(int v) {
        switch (v) {
            case    2: return 0xFFEEE4DA;
            case    4: return 0xFFEDE0C8;
            case    8: return 0xFFF2B179;
            case   16: return 0xFFF59563;
            case   32: return 0xFFF67C5F;
            case   64: return 0xFFF65E3B;
            case  128: return 0xFFEDCF72;
            case  256: return 0xFFEDCC61;
            case  512: return 0xFFEDC850;
            case 1024: return 0xFFEDC53F;
            case 2048: return 0xFFEDC22E;
            default:   return v > 2048 ? 0xFF3C3066 : 0xFF3C3A5E;
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showWinDialog() {
        new AlertDialog.Builder(this)
            .setTitle("You reached 2048!")
            .setMessage("Score: " + score + "\n\nKeep going for a higher score?")
            .setPositiveButton("Keep Going", null)
            .setNegativeButton("New Game", (d, w) -> startNewGame())
            .show();
    }

    private void showLoseDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("No more moves!\nScore: " + score)
            .setPositiveButton("Try Again", (d, w) -> startNewGame())
            .setNegativeButton("Exit", (d, w) -> finish())
            .show();
    }
}
