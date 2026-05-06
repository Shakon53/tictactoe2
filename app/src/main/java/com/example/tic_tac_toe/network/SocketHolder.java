package com.example.tic_tac_toe.network;

public class SocketHolder {
    private static SocketClient socketClient;

    private SocketHolder() {
    }

    public static synchronized SocketClient create(SocketClient.SocketListener listener) {
        if (socketClient != null) {
            socketClient.disconnect();
        }
        socketClient = new SocketClient(listener);
        return socketClient;
    }

    public static synchronized SocketClient get() {
        return socketClient;
    }

    public static synchronized void clear() {
        socketClient = null;
    }
}
