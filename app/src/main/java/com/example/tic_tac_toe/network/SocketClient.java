package com.example.tic_tac_toe.network;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class SocketClient {

    public interface SocketListener {
        void onConnected();
        void onMessageReceived(String message);
        void onError(String errorMessage);
        void onDisconnected();
    }

    private static final String SERVER_HOST = "trolley.proxy.rlwy.net";
    private static final int SERVER_PORT = 17667;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private SocketListener listener;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean isConnected = false;

    public SocketClient(SocketListener listener) {
        this.listener = listener;
    }

    public synchronized void setListener(SocketListener listener) {
        this.listener = listener;
    }

    public void connect() {
        executor.execute(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                isConnected = true;
                postConnected();
                startReading();
            } catch (IOException e) {
                postError("Connect error: " + e.getMessage());
                disconnectInternal(false);
            }
        });
    }

    public void sendMessage(String message) {
        try {
            executor.execute(() -> {
                if (!isConnected || writer == null) {
                    postError("Not connected");
                    return;
                }
                try {
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    postError("Send error: " + e.getMessage());
                    disconnectInternal(true);
                }
            });
        } catch (RejectedExecutionException ignored) {
            postError("Connection is closed");
        }
    }

    public void disconnect() {
        try {
            executor.execute(() -> {
                disconnectInternal(true);
                executor.shutdownNow();
            });
        } catch (RejectedExecutionException ignored) {
            disconnectInternal(false);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void startReading() {
        executor.execute(() -> {
            try {
                String line;
                while (isConnected && (line = reader.readLine()) != null) {
                    postMessage(line);
                }
            } catch (IOException e) {
                if (isConnected) {
                    postError("Read error: " + e.getMessage());
                }
            } finally {
                disconnectInternal(true);
            }
        });
    }

    private synchronized void disconnectInternal(boolean notify) {
        if (!isConnected && socket == null) return;

        isConnected = false;

        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        try { if (writer != null) writer.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        reader = null;
        writer = null;
        socket = null;

        if (notify) postDisconnected();
    }

    private void postConnected() {
        mainHandler.post(() -> { if (listener != null) listener.onConnected(); });
    }

    private void postMessage(String message) {
        mainHandler.post(() -> { if (listener != null) listener.onMessageReceived(message); });
    }

    private void postError(String error) {
        mainHandler.post(() -> { if (listener != null) listener.onError(error); });
    }

    private void postDisconnected() {
        mainHandler.post(() -> { if (listener != null) listener.onDisconnected(); });
    }
}
