package com.example.tic_tac_toe;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tic_tac_toe.network.SocketClient;
import com.example.tic_tac_toe.network.SocketHolder;

public class CreateRoomActivity extends AppCompatActivity implements SocketClient.SocketListener {

    private static final String PREFS_NAME = "tictactoe_prefs";
    private static final String KEY_USERNAME = "last_username";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1200L;

    private EditText etUsername;
    private Button btnCreate;
    private TextView tvCreatedRoomCode;
    private TextView tvCreateStatus;
    private ImageView ivShareCode;
    private ProgressBar progressCreate;
    private View formCard;
    private View roomCard;

    private SocketClient socketClient;
    private String username;
    private boolean openingGame = false;
    private ObjectAnimator statusPulse;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private int connectAttempts = 0;
    private boolean waitingRetry = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        etUsername = findViewById(R.id.etUsername);
        btnCreate = findViewById(R.id.btnCreateRoomNow);
        tvCreatedRoomCode = findViewById(R.id.tvCreatedRoomCode);
        tvCreateStatus = findViewById(R.id.tvCreateStatus);
        ivShareCode = findViewById(R.id.ivShareCode);
        progressCreate = findViewById(R.id.progressCreate);
        formCard = ((View) btnCreate.getParent().getParent());
        roomCard = (View) tvCreatedRoomCode.getParent().getParent().getParent();

        Button btnBack = findViewById(R.id.btnBackCreate);
        btnBack.setOnClickListener(v -> finish());
        GameButtonHelper.apply(btnBack);

        btnCreate.setOnClickListener(v -> createRoom());
        ivShareCode.setOnClickListener(v -> shareRoomCode());
        tvCreatedRoomCode.setOnClickListener(v -> copyRoomCode());

        GameButtonHelper.apply(btnCreate);

        loadSavedUsername();
        animateEntranceCards();
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

    private void animateEntranceCards() {
        View topBar = findViewById(R.id.btnBackCreate).getParent() instanceof View
            ? (View) findViewById(R.id.btnBackCreate).getParent()
            : null;

        // Animate form card sliding up
        View[] cards = {formCard, roomCard};
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;
            cards[i].setAlpha(0f);
            cards[i].setTranslationY(80f);
            cards[i].animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(i * 120L)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
        }
    }

    private void createRoom() {
        username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show();
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
        tvCreateStatus.setText("Connecting... (" + connectAttempts + "/" + MAX_CONNECT_ATTEMPTS + ")");
        socketClient = SocketHolder.create(this);
        socketClient.connect();
    }

    private void setLoading(boolean loading) {
        btnCreate.setEnabled(!loading);
        btnCreate.setText(loading ? "" : "CREATE ROOM");
        progressCreate.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void startStatusPulse() {
        stopStatusPulse();
        statusPulse = ObjectAnimator.ofFloat(tvCreateStatus, "alpha", 1f, 0.3f);
        statusPulse.setDuration(800);
        statusPulse.setRepeatCount(ObjectAnimator.INFINITE);
        statusPulse.setRepeatMode(ObjectAnimator.REVERSE);
        statusPulse.start();
    }

    private void stopStatusPulse() {
        if (statusPulse != null) {
            statusPulse.cancel();
            tvCreateStatus.setAlpha(1f);
        }
    }

    private void copyRoomCode() {
        String code = tvCreatedRoomCode.getText().toString();
        if (code.equals("----") || code.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", code));
        Toast.makeText(this, "Code copied: " + code, Toast.LENGTH_SHORT).show();
        tvCreatedRoomCode.animate().scaleX(1.15f).scaleY(1.15f)
            .setDuration(100).withEndAction(() ->
                tvCreatedRoomCode.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            ).start();
    }

    private void shareRoomCode() {
        String code = tvCreatedRoomCode.getText().toString();
        if (code.equals("----") || code.isEmpty()) {
            Toast.makeText(this, "No room code yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Play Tic Tac Toe with me! Room Code: " + code);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Room Code"));
    }

    @Override
    public void onConnected() {
        tvCreateStatus.setText("Creating room...");
        socketClient.sendMessage("CREATE_ROOM|" + username);
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("ROOM_CREATED|")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 3) {
                String roomCode = parts[1];
                String mySymbol = parts[2];
                setLoading(false);
                tvCreatedRoomCode.setText(roomCode);
                tvCreateStatus.setText("Waiting for opponent...");
                startStatusPulse();
                openGame(roomCode, mySymbol);
            }
        } else if (message.startsWith("ERROR|")) {
            setLoading(false);
            stopStatusPulse();
            tvCreateStatus.setText("Error");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onError(String errorMessage) {
        if (tryReconnect(errorMessage)) {
            return;
        }
        setLoading(false);
        stopStatusPulse();
        tvCreateStatus.setText("Connection failed");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected() {
        if (!openingGame) {
            if (waitingRetry) return;
            setLoading(false);
            stopStatusPulse();
            tvCreateStatus.setText("Disconnected");
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
        tvCreateStatus.setText("Retrying...");
        retryHandler.postDelayed(() -> {
            waitingRetry = false;
            connectToServer();
        }, RETRY_DELAY_MS);
        return true;
    }

    private void openGame(String roomCode, String mySymbol) {
        openingGame = true;
        stopStatusPulse();
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
        stopStatusPulse();
        if (!openingGame) {
            SocketClient client = SocketHolder.get();
            if (client != null) client.disconnect();
            SocketHolder.clear();
        }
    }
}
