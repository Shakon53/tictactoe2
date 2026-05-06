package com.example.tic_tac_toe;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

/**
 * GameButtonHelper — adds the "Block Blast" style physical press effect to any button.
 *
 * Usage:
 *   GameButtonHelper.apply(myButton);
 *   GameButtonHelper.apply(myButton, runnable); // with click action
 */
public class GameButtonHelper {

    private static final float PRESS_TRANSLATE_Y = 6f; // dp shift when pressed
    private static final long PRESS_DURATION = 80;     // ms going down
    private static final long RELEASE_DURATION = 200;  // ms bouncing back

    @SuppressLint("ClickableViewAccessibility")
    public static void apply(View view) {
        apply(view, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void apply(View view, Runnable onClick) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Press down: shift view down, scale slightly smaller
                    v.animate()
                        .translationY(PRESS_TRANSLATE_Y)
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(PRESS_DURATION)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    return false; // allow click to propagate

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Release: spring back up with overshoot bounce
                    v.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(RELEASE_DURATION)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start();

                    if (event.getAction() == MotionEvent.ACTION_UP && onClick != null) {
                        // Run action after the animation starts
                        v.postDelayed(onClick, 60);
                    }
                    return false;
            }
            return false;
        });
    }

    /**
     * Apply the effect to all buttons in an array
     */
    public static void applyAll(View... views) {
        for (View v : views) {
            apply(v);
        }
    }
}
