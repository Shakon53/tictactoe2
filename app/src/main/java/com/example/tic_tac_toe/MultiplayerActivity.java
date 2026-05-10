package com.example.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MultiplayerActivity extends BaseGameActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        Button btnBack          = findViewById(R.id.btnBack);
        Button btnCreateRoom    = findViewById(R.id.btnCreateRoom);
        Button btnJoinRoom      = findViewById(R.id.btnJoinRoom);
        Button btnInviteFriends = findViewById(R.id.btnInviteFriends);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.page_in_left, R.anim.page_out_right);
        });

        btnCreateRoom.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateRoomActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });

        btnJoinRoom.setOnClickListener(v -> {
            startActivity(new Intent(this, JoinRoomActivity.class));
            overridePendingTransition(R.anim.page_in_right, R.anim.page_out_left);
        });

        btnInviteFriends.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Let's play Tic Tac Toe Online! 🎮");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                "🎮 Hey! Let's play Tic Tac Toe Online together!\n\n" +
                "➡ Download the game and I'll create a room —\n" +
                "just send me your name and I'll share the Room Code with you.\n\n" +
                "🔥 Tic Tac Toe Online — fast, fun multiplayer!");
            startActivity(Intent.createChooser(shareIntent, "Invite a Friend"));
        });
    }
}
