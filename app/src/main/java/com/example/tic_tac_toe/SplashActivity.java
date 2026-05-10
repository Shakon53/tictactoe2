package com.example.tic_tac_toe;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends BaseGameActivity {

    private static final long DURATION_MS = 2800L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearProgressIndicator progressBar = findViewById(R.id.splashProgressBar);
        TextView tvLoading  = findViewById(R.id.tvSplashLoading);
        TextView tvPercent  = findViewById(R.id.tvSplashPercent);
        TextView tvLogo     = null;
        android.widget.ImageView ivLogo = findViewById(R.id.ivSplashLogo);

        // Logo bounce animation
        ivLogo.setScaleX(0.6f);
        ivLogo.setScaleY(0.6f);
        ivLogo.setAlpha(0f);
        ivLogo.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(600)
            .setInterpolator(new OvershootInterpolator(1.5f))
            .start();

        // Progress bar animation 0 → 100
        ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
        progressAnim.setDuration(DURATION_MS);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.addUpdateListener(anim -> {
            int val = (int) anim.getAnimatedValue();
            progressBar.setProgressCompat(val, true);
            tvPercent.setText(val + "%");
        });
        progressAnim.start();

        // Animated loading dots
        handler.post(new Runnable() {
            int dots = 0;
            final String[] states = {"Loading", "Loading.", "Loading..", "Loading..."};
            @Override public void run() {
                dots = (dots + 1) % states.length;
                tvLoading.setText(states[dots]);
                handler.postDelayed(this, 420);
            }
        });

        // After loading, decide where to go
        handler.postDelayed(() -> {
            handler.removeCallbacksAndMessages(null);
            navigateNext();
        }, DURATION_MS + 100);
    }

    private void navigateNext() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Only skip login if signed in with a real (non-anonymous) account
        if (user != null && !user.isAnonymous()) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
