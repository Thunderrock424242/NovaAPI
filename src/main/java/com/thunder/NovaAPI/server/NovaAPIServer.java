package com.thunder.NovaAPI.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NovaAPIServer {
    private static final int PORT = 25565;
    private static final Set<String> whitelistedServers = new HashSet<>();
    private static final Map<Socket, String> connectedServers = new HashMap<>();
    private static final Map<String, String> syncStore = new HashMap<>();
    private static final java.util.concurrent.ConcurrentLinkedQueue<ChunkRequest> preloadQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private record ChunkRequest(int x, int z) { }

    public static void main(String[] args) {
        System.out.println("[Nova API Server] Starting...");
        loadWhitelist();
        startServer();
    }

    private static void loadWhitelist() {
        File whitelistFile = new File("whitelist.txt");
        if (!whitelistFile.exists()) {
            try (FileWriter writer = new FileWriter(whitelistFile)) {
                writer.write("# Add trusted server IPs here, one per line\n");
            } catch (IOException e) {
                System.err.println("[Nova API Server] Failed to create whitelist.txt");
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(whitelistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    whitelistedServers.add(line.trim());
                }
            }
            System.out.println("[Nova API Server] Loaded " + whitelistedServers.size() + " whitelisted servers.");
        } catch (IOException e) {
            System.err.println("[Nova API Server] Error loading whitelist.");
        }
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Nova API Server] Listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Nova API Server] Failed to start server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        String serverIP = socket.getInetAddress().getHostAddress();

        if (!whitelistedServers.contains(serverIP)) {
            System.err.println("[Nova API Server] Connection denied: " + serverIP + " is not whitelisted.");
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("[Nova API Server] Error closing connection: " + e.getMessage());
            }
            return;
        }

        System.out.println("[Nova API Server] Authorized connection from: " + serverIP);
        connectedServers.put(socket, serverIP);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            writer.write("AUTH_SUCCESS\n");
            writer.flush();

            while (true) {
                String message = reader.readLine();
                if (message == null) break;
                handleMessage(socket, message);
            }

        } catch (IOException e) {
            System.err.println("[Nova API Server] Connection error: " + e.getMessage());
        } finally {
            connectedServers.remove(socket);
        }
    }

    private static void handleMessage(Socket socket, String message) {
        System.out.println("[Nova API Server] Received from " + connectedServers.get(socket) + ": " + message);

        String[] parts = message.trim().split("\\s+");
        if (parts.length == 0) return;

        String cmd = parts[0].toUpperCase();
        switch (cmd) {
            case "PING" -> sendMessage(socket, "PONG");
            case "PRELOAD" -> handlePreload(socket, parts);
            case "PATHFIND" -> handlePathfind(socket, parts);
            case "SYNC" -> handleSync(socket, parts);
            default -> sendMessage(socket, "ERR Unknown command");
        }
    }

    private static void handlePreload(Socket socket, String[] parts) {
        if (parts.length < 3) {
            sendMessage(socket, "ERR Usage: PRELOAD <x> <z>");
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            preloadQueue.add(new ChunkRequest(x, z));
            sendMessage(socket, "OK");
        } catch (NumberFormatException e) {
            sendMessage(socket, "ERR Invalid coordinates");
        }
    }

    private static void handlePathfind(Socket socket, String[] parts) {
        if (parts.length < 5) {
            sendMessage(socket, "ERR Usage: PATHFIND <sx> <sz> <ex> <ez>");
            return;
        }
        try {
            int sx = Integer.parseInt(parts[1]);
            int sz = Integer.parseInt(parts[2]);
            int ex = Integer.parseInt(parts[3]);
            int ez = Integer.parseInt(parts[4]);

            java.util.List<String> nodes = new java.util.ArrayList<>();
            int x = sx;
            int z = sz;
            while (x != ex) {
                nodes.add(x + "," + z);
                x += Integer.signum(ex - x);
            }
            while (z != ez) {
                nodes.add(x + "," + z);
                z += Integer.signum(ez - z);
            }
            nodes.add(ex + "," + ez);
            sendMessage(socket, String.join(";", nodes));
        } catch (NumberFormatException e) {
            sendMessage(socket, "ERR Invalid coordinates");
        }
    }

    private static void handleSync(Socket socket, String[] parts) {
        if (parts.length == 2) {
            String val = syncStore.get(parts[1]);
            sendMessage(socket, val == null ? "NULL" : val);
        } else if (parts.length >= 3) {
            syncStore.put(parts[1], parts[2]);
            sendMessage(socket, "OK");
        } else {
            sendMessage(socket, "ERR Usage: SYNC <key> [value]");
        }
    }

    private static void sendMessage(Socket socket, String response) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(response);
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            System.err.println("[Nova API Server] Failed to send response: " + e.getMessage());
        }
    }
}