package com.example.tic_tac_toe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tic_tac_toe.network.SocketClient;
import com.example.tic_tac_toe.network.SocketHolder;

public class JoinRoomActivity extends AppCompatActivity implements SocketClient.SocketListener {

    private static final String PREFS_NAME = "tictactoe_prefs";
    private static final String KEY_USERNAME = "last_username";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1200L;

    private EditText etUsername;
    private EditText etRoomCode;
    private Button btnJoin;
    private TextView tvJoinStatus;
    private ProgressBar progressJoin;

    private SocketClient socketClient;
    private String username;
    private String roomCode;
    private String mySymbol = "";
    private boolean openingGame = false;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private int connectAttempts = 0;
    private boolean waitingRetry = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        etUsername = findViewById(R.id.etJoinUsername);
        etRoomCode = findViewById(R.id.etRoomCode);
        btnJoin = findViewById(R.id.btnJoinRoomNow);
        tvJoinStatus = findViewById(R.id.tvJoinStatus);
        progressJoin = findViewById(R.id.progressJoin);

        Button btnBack = findViewById(R.id.btnBackJoin);
        btnBack.setOnClickListener(v -> finish());
        GameButtonHelper.apply(btnBack);

        btnJoin.setOnClickListener(v -> joinRoom());
        GameButtonHelper.apply(btnJoin);

        loadSavedUsername();
        animateEntrance();
    }

    private void loadSavedUsername() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_USERNAME, "");
        if (!saved.isEmpty()) {
            etUsername.setText(saved);
            etUsername.setSelection(saved.length());
        }
    }

    private void saveUsername(String name) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, name)
            .apply();
    }

    private void animateEntrance() {
        View formCard = (View) btnJoin.getParent().getParent();
        formCard.setAlpha(0f);
        formCard.setTranslationY(80f);
        formCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();

        tvJoinStatus.setAlpha(0f);
        tvJoinStatus.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(200)
            .start();
    }

    private void joinRoom() {
        username = etUsername.getText().toString().trim();
        roomCode = etRoomCode.getText().toString().trim().toUpperCase();

        if (username.isEmpty() || roomCode.isEmpty()) {
            Toast.makeText(this, "Enter your name and room code", Toast.LENGTH_SHORT).show();
            return;
        }
        saveUsername(username);
        connectAttempts = 0;
        waitingRetry = false;
        setLoading(true);
        connectToServer();
    }

    private void connectToServer() {
        connectAttempts++;
        tvJoinStatus.setText("Connecting... (" + connectAttempts + "/" + MAX_CONNECT_ATTEMPTS + ")");
        socketClient = SocketHolder.create(this);
        socketClient.connect();
    }

    private void setLoading(boolean loading) {
        btnJoin.setEnabled(!loading);
        btnJoin.setText(loading ? "" : "JOIN ROOM");
        progressJoin.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onConnected() {
        tvJoinStatus.setText("Joining room...");
        socketClient.sendMessage("JOIN_ROOM|" + roomCode + "|" + username);
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("JOIN_SUCCESS|")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 2) {
                mySymbol = parts[1];
                tvJoinStatus.setText("Joined! Starting...");
                openGame();
            }
        } else if (message.startsWith("ERROR|")) {
            setLoading(false);
            tvJoinStatus.setText("Error — try again");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onError(String errorMessage) {
        if (tryReconnect(errorMessage)) {
            return;
        }
        setLoading(false);
        tvJoinStatus.setText("Connection failed");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected() {
        if (!openingGame) {
            if (waitingRetry) return;
            setLoading(false);
            tvJoinStatus.setText("Disconnected");
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean tryReconnect(String errorMessage) {
        if (openingGame) return false;
        if (connectAttempts >= MAX_CONNECT_ATTEMPTS) return false;
        if (errorMessage == null) return false;
        String lower = errorMessage.toLowerCase();
        boolean networkIssue = lower.contains("connection reset")
            || lower.contains("connect error")
            || lower.contains("read error")
            || lower.contains("timeout");
        if (!networkIssue) return false;

        waitingRetry = true;
        tvJoinStatus.setText("Retrying...");
        retryHandler.postDelayed(() -> {
            waitingRetry = false;
            connectToServer();
        }, RETRY_DELAY_MS);
        return true;
    }

    private void openGame() {
        openingGame = true;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("mySymbol", mySymbol);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        retryHandler.removeCallbacksAndMessages(null);
        if (!openingGame) {
            SocketClient client = SocketHolder.get();
            if (client != null) client.disconnect();
            SocketHolder.clear();
        }
    }
}
