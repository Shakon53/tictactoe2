package com.example.tic_tac_toe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;
import java.util.Random;

public class SnakeGameView extends View {

    public enum Dir { UP, DOWN, LEFT, RIGHT }

    public interface Callback {
        void onScore(int score);
        void onGameOver(int score);
    }

    private static final int COLS = 18;
    private static final int ROWS = 24;
    private static final int SPEED_MS = 150;

    private final LinkedList<int[]> snake = new LinkedList<>();
    private int[] food;
    private Dir dir     = Dir.RIGHT;
    private Dir nextDir = Dir.RIGHT;
    private boolean running  = false;
    private boolean gameOver = false;
    private int score = 0;

    private Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    // Drawing
    private float cellW, cellH;
    private final Paint bgPaint      = new Paint();
    private final Paint gridPaint    = new Paint();
    private final Paint snakePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint foodPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    public SnakeGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnakeGameView(Context context) {
        super(context);
        init();
    }

    private void init() {
        bgPaint.setColor(0xFF0D0B22);

        gridPaint.setColor(0x0DFFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.8f);

        snakePaint.setColor(0xFF10B981);
        snakePaint.setStyle(Paint.Style.FILL);

        headPaint.setColor(0xFF34D399);
        headPaint.setStyle(Paint.Style.FILL);

        foodPaint.setColor(0xFFFF4757);
        foodPaint.setStyle(Paint.Style.FILL);

        overPaint.setColor(0xBB000000);

        overTextPaint.setColor(0xFFFFFFFF);
        overTextPaint.setTextSize(48f);
        overTextPaint.setTextAlign(Paint.Align.CENTER);
        overTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellW = (float) w / COLS;
        cellH = (float) h / ROWS;
    }

    public void setCallback(Callback cb) { this.callback = cb; }

    public void startGame() {
        handler.removeCallbacksAndMessages(null);
        snake.clear();
        snake.addFirst(new int[]{COLS / 2,     ROWS / 2});
        snake.addFirst(new int[]{COLS / 2 + 1, ROWS / 2});
        snake.addFirst(new int[]{COLS / 2 + 2, ROWS / 2});
        dir     = Dir.RIGHT;
        nextDir = Dir.RIGHT;
        score   = 0;
        gameOver = false;
        running  = true;
        placeFood();
        handler.postDelayed(loop, SPEED_MS);
        invalidate();
    }

    public void stopGame() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    public void changeDir(Dir d) {
        if (dir == Dir.UP    && d == Dir.DOWN)  return;
        if (dir == Dir.DOWN  && d == Dir.UP)    return;
        if (dir == Dir.LEFT  && d == Dir.RIGHT) return;
        if (dir == Dir.RIGHT && d == Dir.LEFT)  return;
        nextDir = d;
    }

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            tick();
            invalidate();
            handler.postDelayed(this, SPEED_MS);
        }
    };

    private void tick() {
        dir = nextDir;
        int[] head = snake.getFirst();
        int nx = head[0], ny = head[1];
        switch (dir) {
            case UP:    ny--; break;
            case DOWN:  ny++; break;
            case LEFT:  nx--; break;
            case RIGHT: nx++; break;
        }
        // Wall collision
        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) { end(); return; }
        // Self collision
        for (int[] seg : snake) {
            if (seg[0] == nx && seg[1] == ny) { end(); return; }
        }
        snake.addFirst(new int[]{nx, ny});
        if (food != null && nx == food[0] && ny == food[1]) {
            score++;
            if (callback != null) callback.onScore(score);
            placeFood();
        } else {
            snake.removeLast();
        }
    }

    private void placeFood() {
        int x, y;
        do {
            x = random.nextInt(COLS);
            y = random.nextInt(ROWS);
        } while (isOnSnake(x, y));
        food = new int[]{x, y};
    }

    private boolean isOnSnake(int x, int y) {
        for (int[] seg : snake) if (seg[0] == x && seg[1] == y) return true;
        return false;
    }

    private void end() {
        running  = false;
        gameOver = true;
        handler.removeCallbacksAndMessages(null);
        invalidate();
        if (callback != null) callback.onGameOver(score);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Background
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        // Grid
        for (int c = 0; c <= COLS; c++)
            canvas.drawLine(c * cellW, 0, c * cellW, getHeight(), gridPaint);
        for (int r = 0; r <= ROWS; r++)
            canvas.drawLine(0, r * cellH, getWidth(), r * cellH, gridPaint);

        // Food
        if (food != null) {
            float fx = food[0] * cellW + cellW * 0.15f;
            float fy = food[1] * cellH + cellH * 0.15f;
            float fw = cellW * 0.7f, fh = cellH * 0.7f;
            rectF.set(fx, fy, fx + fw, fy + fh);
            canvas.drawRoundRect(rectF, cellW * 0.35f, cellH * 0.35f, foodPaint);
        }

        // Snake
        boolean isHead = true;
        for (int[] seg : snake) {
            float sx = seg[0] * cellW + cellW * 0.08f;
            float sy = seg[1] * cellH + cellH * 0.08f;
            float sw = cellW * 0.84f, sh = cellH * 0.84f;
            rectF.set(sx, sy, sx + sw, sy + sh);
            canvas.drawRoundRect(rectF, cellW * 0.3f, cellH * 0.3f, isHead ? headPaint : snakePaint);
            isHead = false;
        }

        // Game over overlay
        if (gameOver) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), overPaint);
            canvas.drawText("GAME OVER",
                getWidth() / 2f, getHeight() / 2f - 20f, overTextPaint);
            overTextPaint.setTextSize(32f);
            canvas.drawText("Score: " + score,
                getWidth() / 2f, getHeight() / 2f + 30f, overTextPaint);
            overTextPaint.setTextSize(48f);
        }
    }
}
