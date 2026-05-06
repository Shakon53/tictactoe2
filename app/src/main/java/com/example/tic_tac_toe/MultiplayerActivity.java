package com.example.tic_tac_toe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MultiplayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        Button btnBack       = findViewById(R.id.btnBack);
        Button btnCreateRoom = findViewById(R.id.btnCreateRoom);
        Button btnJoinRoom   = findViewById(R.id.btnJoinRoom);

        btnBack.setOnClickListener(v -> finish());

        btnCreateRoom.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateRoomActivity.class));
        });

        btnJoinRoom.setOnClickListener(v -> {
            startActivity(new Intent(this, JoinRoomActivity.class));
        });
    }
}
