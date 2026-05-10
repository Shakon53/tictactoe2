package com.example.tic_tac_toe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends BaseGameActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth      mAuth;
    private GoogleSignInClient googleSignInClient;
    private Button btnGoogle, btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Skip login only if signed in with a real (non-anonymous) Google account
        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null && !current.isAnonymous()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogleSignIn);
        btnGuest  = findViewById(R.id.btnGuestPlay);

        GameButtonHelper.apply(btnGuest);

        // Entrance animation
        View card = findViewById(R.id.loginCard);
        card.setAlpha(0f);
        card.setTranslationY(60f);
        card.animate().alpha(1f).translationY(0f)
            .setDuration(500)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .setStartDelay(200)
            .start();

        // Try to configure Google Sign-In
        setupGoogleSignIn();

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnGuest.setOnClickListener(v -> signInAsGuest());
    }

    private void setupGoogleSignIn() {
        try {
            // This requires SHA-1 added to Firebase + Google enabled
            String webClientId = getString(R.string.default_web_client_id);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            // Google Sign-In not configured yet (no SHA-1)
            btnGoogle.setAlpha(0.5f);
            btnGoogle.setOnClickListener(v -> showSha1Dialog());
        }
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            showSha1Dialog();
            return;
        }
        btnGoogle.setEnabled(false);
        btnGoogle.setText("Connecting...");
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SIGN_IN) return;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            btnGoogle.setEnabled(true);
            btnGoogle.setText("G   Continue with Google");
            Toast.makeText(this, "Sign in failed. Check SHA-1 setup.", Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        initUserProfileIfNew(user);
                    }
                    goToMain();
                } else {
                    btnGoogle.setEnabled(true);
                    btnGoogle.setText("G   Continue with Google");
                    Toast.makeText(this, "Auth failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    /** Creates a Firestore profile document on first Google login (if it doesn't exist yet). */
    private void initUserProfileIfNew(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    // First time logging in — create profile
                    String joinDate = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
                        .format(new Date());

                    // Also save to SharedPreferences so ProfileActivity shows it immediately
                    SharedPreferences prefs = getSharedPreferences("tictactoe_prefs", MODE_PRIVATE);
                    String googleName = user.getDisplayName();
                    if (googleName != null && !googleName.isEmpty()) {
                        prefs.edit().putString("profile_username", googleName).apply();
                    }
                    if (prefs.getString("profile_joined", null) == null) {
                        prefs.edit().putString("profile_joined", joinDate).apply();
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("username",   googleName != null ? googleName : "Player");
                    data.put("avatar",     "🎮");
                    data.put("email",      user.getEmail() != null ? user.getEmail() : "");
                    data.put("googleName", googleName != null ? googleName : "");
                    data.put("joinedDate", joinDate);
                    data.put("wins",       0);
                    data.put("losses",     0);
                    data.put("draws",      0);
                    data.put("bestStreak", 0);
                    data.put("totalGames", 0);
                    data.put("createdAt",  new Date().toString());

                    db.collection("users").document(uid)
                        .set(data, SetOptions.merge());
                }
            });
    }

    private void signInAsGuest() {
        btnGuest.setEnabled(false);
        btnGuest.setText("Entering...");
        mAuth.signInAnonymously()
            .addOnCompleteListener(task -> {
                // Go to main regardless (guest or anon)
                goToMain();
            });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        finish();
    }

    private void showSha1Dialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Setup Required")
            .setMessage(
                "To enable Google Sign-In:\n\n" +
                "1. In Android Studio:\n" +
                "   Gradle → :app → Tasks → android → signingReport\n" +
                "   Copy the SHA-1 fingerprint\n\n" +
                "2. Firebase Console:\n" +
                "   Project Settings → Your apps → Android app\n" +
                "   → Add fingerprint → paste SHA-1\n\n" +
                "3. Enable Google in:\n" +
                "   Authentication → Sign-in method → Google\n\n" +
                "4. Download new google-services.json\n\n" +
                "For now — use Guest mode!"
            )
            .setPositiveButton("Use Guest", (d, w) -> signInAsGuest())
            .setNegativeButton("Cancel", null)
            .show();
    }
}
