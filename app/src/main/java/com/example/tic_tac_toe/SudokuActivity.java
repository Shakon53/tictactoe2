package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class SudokuActivity extends BaseGameActivity {

    private static final String PREFS_NAME  = "tictactoe_prefs";
    private static final String KEY_BEST_T  = "sudoku_best_time";
    private static final int    N           = 9;

    // Puzzle bank: 0 = empty cell
    private static final int[][][] PUZZLES = {
        // ── EASY ──
        {{5,3,0,0,7,0,0,0,0},
         {6,0,0,1,9,5,0,0,0},
         {0,9,8,0,0,0,0,6,0},
         {8,0,0,0,6,0,0,0,3},
         {4,0,0,8,0,3,0,0,1},
         {7,0,0,0,2,0,0,0,6},
         {0,6,0,0,0,0,2,8,0},
         {0,0,0,4,1,9,0,0,5},
         {0,0,0,0,8,0,0,7,9}},
        // ── MEDIUM ──
        {{0,0,0,2,6,0,7,0,1},
         {6,8,0,0,7,0,0,9,0},
         {1,9,0,0,0,4,5,0,0},
         {8,2,0,1,0,0,0,4,0},
         {0,0,4,6,0,2,9,0,0},
         {0,5,0,0,0,3,0,2,8},
         {0,0,9,3,0,0,0,7,4},
         {0,4,0,0,5,0,0,3,6},
         {7,0,3,0,1,8,0,0,0}},
        // ── HARD ──
        {{8,0,0,0,0,0,0,0,0},
         {0,0,3,6,0,0,0,0,0},
         {0,7,0,0,9,0,2,0,0},
         {0,5,0,0,0,7,0,0,0},
         {0,0,0,0,4,5,7,0,0},
         {0,0,0,1,0,0,0,3,0},
         {0,0,1,0,0,0,0,6,8},
         {0,0,8,5,0,0,0,1,0},
         {0,9,0,0,0,0,4,0,0}}
    };

    private static final int[][][] SOLUTIONS = {
        // ── EASY solution ──
        {{5,3,4,6,7,8,9,1,2},
         {6,7,2,1,9,5,3,4,8},
         {1,9,8,3,4,2,5,6,7},
         {8,5,9,7,6,1,4,2,3},
         {4,2,6,8,5,3,7,9,1},
         {7,1,3,9,2,4,8,5,6},
         {9,6,1,5,3,7,2,8,4},
         {2,8,7,4,1,9,6,3,5},
         {3,4,5,2,8,6,1,7,9}},
        // ── MEDIUM solution ──
        {{4,3,5,2,6,9,7,8,1},
         {6,8,2,5,7,1,4,9,3},
         {1,9,7,8,3,4,5,6,2},
         {8,2,6,1,9,5,3,4,7},
         {3,7,4,6,8,2,9,1,5},
         {9,5,1,7,4,3,6,2,8},
         {5,1,9,3,2,6,8,7,4},
         {2,4,8,9,5,7,1,3,6},
         {7,6,3,4,1,8,2,5,9}},
        // ── HARD solution ──
        {{8,1,2,7,5,3,6,4,9},
         {9,4,3,6,8,2,1,7,5},
         {6,7,5,4,9,1,2,8,3},
         {1,5,4,2,3,7,8,9,6},
         {3,6,9,8,4,5,7,2,1},
         {2,8,7,1,6,9,5,3,4},
         {5,2,1,9,7,4,3,6,8},
         {4,3,8,5,2,6,9,1,7},
         {7,9,6,3,1,8,4,5,2}}
    };

    private TextView[][] cells = new TextView[N][N];
    private boolean[][] givens = new boolean[N][N];
    private int[][] board     = new int[N][N];
    private int selectedRow = -1, selectedCol = -1;
    private int puzzleIndex = 0;

    private TextView tvTimer, tvPuzzleName;
    private int elapsedSec = 0;
    private boolean timerRunning = false;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerTick;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvTimer      = findViewById(R.id.tvSudokuTimer);
        tvPuzzleName = findViewById(R.id.tvSudokuTitle);

        Button btnBack = findViewById(R.id.btnSudokuBack);
        GameButtonHelper.apply(btnBack);
        btnBack.setOnClickListener(v -> {
            stopTimer();
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        // Difficulty buttons
        Button btnEasy   = findViewById(R.id.btnSudokuEasy);
        Button btnMedium = findViewById(R.id.btnSudokuMedium);
        Button btnHard   = findViewById(R.id.btnSudokuHard);
        btnEasy  .setOnClickListener(v -> loadPuzzle(0));
        btnMedium.setOnClickListener(v -> loadPuzzle(1));
        btnHard  .setOnClickListener(v -> loadPuzzle(2));

        // Number pad
        int[] padIds = {
            R.id.btnSudoku1, R.id.btnSudoku2, R.id.btnSudoku3,
            R.id.btnSudoku4, R.id.btnSudoku5, R.id.btnSudoku6,
            R.id.btnSudoku7, R.id.btnSudoku8, R.id.btnSudoku9,
            R.id.btnSudokuErase
        };
        for (int i = 0; i < padIds.length; i++) {
            final int num = (i < 9) ? (i + 1) : 0;
            Button b = findViewById(padIds[i]);
            b.setOnClickListener(v -> inputNumber(num));
        }

        buildGrid();
        loadPuzzle(0);
    }

    private void buildGrid() {
        GridLayout grid = findViewById(R.id.gridSudoku);
        grid.setRowCount(N);
        grid.setColumnCount(N);

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                TextView tv = new TextView(this);
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(17f);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextColor(0xFFBBBBCC);

                int mL = (c % 3 == 0 && c != 0) ? 4 : 1;
                int mT = (r % 3 == 0 && r != 0) ? 4 : 1;

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1, GridLayout.FILL, 1f),
                        GridLayout.spec(c, 1, GridLayout.FILL, 1f)
                );
                lp.width  = 0;
                lp.height = 0;
                lp.setMargins(mL, mT, 1, 1);
                tv.setLayoutParams(lp);
                setCellBg(tv, false, false, false);

                final int fr = r, fc = c;
                tv.setOnClickListener(v -> selectCell(fr, fc));

                grid.addView(tv);
                cells[r][c] = tv;
            }
        }
    }

    private void loadPuzzle(int index) {
        puzzleIndex = index;
        stopTimer();
        elapsedSec = 0;
        tvTimer.setText("0:00");
        selectedRow = -1; selectedCol = -1;

        int[][] puzzle = PUZZLES[index];
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                board[r][c]  = puzzle[r][c];
                givens[r][c] = (puzzle[r][c] != 0);
                TextView tv = cells[r][c];
                if (givens[r][c]) {
                    tv.setText(String.valueOf(puzzle[r][c]));
                    tv.setTextColor(0xFFFFFFFF);
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setText("");
                    tv.setTextColor(0xFFABB4FF);
                    tv.setTypeface(null, Typeface.NORMAL);
                }
                setCellBg(tv, false, false, false);
            }
        }

        String[] labels = {"Easy", "Medium", "Hard"};
        tvPuzzleName.setText("Sudoku — " + labels[index]);

        startTimer();
    }

    private void selectCell(int r, int c) {
        if (givens[r][c]) return;
        selectedRow = r;
        selectedCol = c;
        refreshHighlight();
    }

    private void inputNumber(int num) {
        if (selectedRow < 0 || selectedCol < 0) return;
        if (givens[selectedRow][selectedCol]) return;

        board[selectedRow][selectedCol] = num;
        TextView tv = cells[selectedRow][selectedCol];
        tv.setText(num == 0 ? "" : String.valueOf(num));
        tv.setTextColor(0xFFABB4FF);

        refreshHighlight();

        if (isBoardFull()) {
            if (checkSolution()) {
                stopTimer();
                int best = prefs.getInt(KEY_BEST_T + "_" + puzzleIndex, Integer.MAX_VALUE);
                if (elapsedSec < best) {
                    prefs.edit().putInt(KEY_BEST_T + "_" + puzzleIndex, elapsedSec).apply();
                }
                showWinDialog();
            } else {
                markErrors();
                Toast.makeText(this, "Some cells are incorrect!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshHighlight() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                boolean sel   = (r == selectedRow && c == selectedCol);
                boolean same  = !sel && selectedRow >= 0 &&
                                (r == selectedRow || c == selectedCol ||
                                 (r/3 == selectedRow/3 && c/3 == selectedCol/3));
                boolean error = !givens[r][c] && board[r][c] != 0 &&
                                board[r][c] != SOLUTIONS[puzzleIndex][r][c];
                setCellBg(cells[r][c], sel, same, error);
            }
        }
    }

    private void markErrors() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                boolean error = !givens[r][c] && board[r][c] != 0 &&
                                board[r][c] != SOLUTIONS[puzzleIndex][r][c];
                if (error) cells[r][c].setTextColor(0xFFFF6B6B);
            }
        }
    }

    private void setCellBg(TextView tv, boolean selected, boolean sameGroup, boolean error) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(6f);
        if (selected) {
            gd.setColor(0xFF5E5BDF);
            gd.setStroke(2, 0xFF9B99FF);
        } else if (error) {
            gd.setColor(0x33FF4444);
            gd.setStroke(1, 0x66FF4444);
        } else if (sameGroup) {
            gd.setColor(0x22A0A0FF);
            gd.setStroke(1, 0x33FFFFFF);
        } else {
            gd.setColor(0x1AFFFFFF);
            gd.setStroke(1, 0x22FFFFFF);
        }
        tv.setBackground(gd);
    }

    private boolean isBoardFull() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (board[r][c] == 0) return false;
        return true;
    }

    private boolean checkSolution() {
        int[][] sol = SOLUTIONS[puzzleIndex];
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (board[r][c] != sol[r][c]) return false;
        return true;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void startTimer() {
        timerRunning = true;
        timerTick = new Runnable() {
            @Override public void run() {
                if (!timerRunning) return;
                elapsedSec++;
                int m = elapsedSec / 60, s = elapsedSec % 60;
                tvTimer.setText(m + ":" + String.format("%02d", s));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerTick, 1000);
    }

    private void stopTimer() {
        timerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    // ── Win Dialog ────────────────────────────────────────────────────────────

    private void showWinDialog() {
        int m = elapsedSec / 60, s = elapsedSec % 60;
        String time = m + ":" + String.format("%02d", s);
        String[] labels = {"Easy", "Medium", "Hard"};

        new AlertDialog.Builder(this)
            .setTitle("Puzzle Solved!")
            .setMessage("Congratulations!\n\nDifficulty: " + labels[puzzleIndex]
                + "\nTime: " + time)
            .setPositiveButton("New Puzzle", (d, w) -> showDifficultyPicker())
            .setNegativeButton("Exit", (d, w) -> finish())
            .show();
    }

    private void showDifficultyPicker() {
        new AlertDialog.Builder(this)
            .setTitle("Select Difficulty")
            .setItems(new String[]{"Easy", "Medium", "Hard"}, (d, which) -> loadPuzzle(which))
            .show();
    }
}
