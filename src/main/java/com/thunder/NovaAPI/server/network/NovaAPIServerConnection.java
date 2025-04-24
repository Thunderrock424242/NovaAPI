package com.thunder.NovaAPI.server.network;

/**
 * Tracks the Nova API client connection status to the dedicated server.
 */
public class NovaAPIServerConnection {
    private static boolean connected = false;
    private static long lastPing = -1;

    public static void setConnected(boolean value) {
        connected = value;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static void setLastPing(long ping) {
        lastPing = ping;
    }

    public static long getLastPing() {
        return lastPing;
    }
}
