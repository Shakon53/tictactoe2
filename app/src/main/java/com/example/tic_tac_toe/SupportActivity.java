package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SupportActivity extends BaseGameActivity {

    private static final String PREFS_NAME = "tictactoe_prefs";

    // Category IDs in same order as admin panel expects
    private static final String CAT_BUG     = "Bug Report";
    private static final String CAT_SUPPORT = "Support Request";
    private static final String CAT_CHEAT   = "Cheating";
    private static final String CAT_OTHER   = "Other";

    private String selectedCategory = CAT_SUPPORT;

    private TextView  tvUsername;
    private EditText  etSubject, etMessage;
    private Button    btnSubmit;
    private LinearLayout layoutForm, layoutSuccess;

    private TextView catBug, catSupport, catCheat, catOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvUsername    = findViewById(R.id.tvSupportUsername);
        etSubject     = findViewById(R.id.etSupportSubject);
        etMessage     = findViewById(R.id.etSupportMessage);
        btnSubmit     = findViewById(R.id.btnSupportSubmit);
        layoutForm    = findViewById(R.id.layoutSupportForm);
        layoutSuccess = findViewById(R.id.layoutSuccess);

        catBug     = findViewById(R.id.catBug);
        catSupport = findViewById(R.id.catSupport);
        catCheat   = findViewById(R.id.catCheat);
        catOther   = findViewById(R.id.catOther);

        // Auto-fill username from profile prefs
        String username = prefs.getString("profile_username",
                          prefs.getString("last_username", "Player"));
        tvUsername.setText(username);

        // Back button
        Button btnBack = findViewById(R.id.btnSupportBack);
        GameButtonHelper.apply(btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        // Category selector
        selectCategory(CAT_SUPPORT);
        catBug.setOnClickListener(v     -> selectCategory(CAT_BUG));
        catSupport.setOnClickListener(v -> selectCategory(CAT_SUPPORT));
        catCheat.setOnClickListener(v   -> selectCategory(CAT_CHEAT));
        catOther.setOnClickListener(v   -> selectCategory(CAT_OTHER));

        // Submit
        GameButtonHelper.apply(btnSubmit);
        btnSubmit.setOnClickListener(v -> submitReport(username));

        // Success close
        Button btnClose = findViewById(R.id.btnSupportClose);
        btnClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        // Entrance animation
        View card = layoutForm;
        card.setAlpha(0f);
        card.setTranslationY(50f);
        card.animate().alpha(1f).translationY(0f)
            .setDuration(450)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .setStartDelay(100)
            .start();
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        styleCategory(catBug,     category.equals(CAT_BUG));
        styleCategory(catSupport, category.equals(CAT_SUPPORT));
        styleCategory(catCheat,   category.equals(CAT_CHEAT));
        styleCategory(catOther,   category.equals(CAT_OTHER));
    }

    private void styleCategory(TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(10));
        if (selected) {
            bg.setColor(0xFF6D28D9);
            bg.setStroke(dpToPx(2), 0xFFA78BFA);
            tv.setTextColor(Color.WHITE);
            tv.setScaleX(1.06f);
            tv.setScaleY(1.06f);
        } else {
            bg.setColor(0x1AFFFFFF);
            bg.setStroke(dpToPx(1), 0x33FFFFFF);
            tv.setTextColor(0xFFAAAAAA);
            tv.setScaleX(1f);
            tv.setScaleY(1f);
        }
        tv.setBackground(bg);
    }

    private void submitReport(String username) {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(subject)) {
            etSubject.setError("Укажи тему");
            etSubject.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Напиши сообщение");
            etMessage.requestFocus();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Отправка…");

        Map<String, Object> data = new HashMap<>();
        data.put("reporter",  username);
        data.put("subject",   subject);
        data.put("category",  selectedCategory);
        data.put("message",   message);
        data.put("status",    "open");
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
            .collection("reports")
            .add(data)
            .addOnSuccessListener(ref -> showSuccess())
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Отправить обращение");
                Toast.makeText(this, "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void showSuccess() {
        layoutForm.animate().alpha(0f).translationY(-30f).setDuration(280)
            .withEndAction(() -> {
                layoutForm.setVisibility(View.GONE);
                layoutSuccess.setVisibility(View.VISIBLE);
                layoutSuccess.setAlpha(0f);
                layoutSuccess.setScaleX(0.85f);
                layoutSuccess.setScaleY(0.85f);
                layoutSuccess.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
            }).start();
    }

    // The form LinearLayout needs an ID — we reuse the inner form card with id layoutSupportForm
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
