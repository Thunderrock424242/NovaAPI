package com.thunder.NovaAPI.server;

import com.thunder.NovaAPI.NovaAPI;
import com.thunder.NovaAPI.config.NovaAPIConfig;
import com.thunder.NovaAPI.optimizations.AsyncWorldGenHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class NovaAPIServerManager {
    private static boolean isDedicatedMode = false;
    private static boolean isServerRunning = false;

    public static void initialize(FMLCommonSetupEvent event) {
        if (isServerRunning) {
            NovaAPI.LOGGER.warn("[Nova API] Another instance is already running. Preventing conflict.");
            return;
        }

        if (NovaAPIConfig.ENABLE_DEDICATED_SERVER.get()) {
            isDedicatedMode = true;
            String serverIP = NovaAPIConfig.DEDICATED_SERVER_IP.get();
        } else {
            startLocalServer();
        }

        isServerRunning = true;
    }

    public static void startLocalServer() {
        NovaAPI.LOGGER.info("[Nova API] Starting in Local Mode...");

        // Chunk Optimization
        if (NovaAPIConfig.ENABLE_CHUNK_OPTIMIZATIONS.get()) {
            ChunkOptimizer.initialize(); // Make sure this is your static init method
            NovaAPI.LOGGER.info("[Nova API] Chunk Optimization initialized.");
        }

        // AI Pathfinding Optimization
        if (NovaAPIConfig.ENABLE_AI_OPTIMIZATIONS.get()) {
            int threadCount = NovaAPIConfig.PATHFINDING_THREAD_COUNT.get();
            PathfindingOptimizer.initialize(threadCount); // Ensure you have this method
            NovaAPI.LOGGER.info("[Nova API] AI Pathfinding initialized with " + threadCount + " thread(s).");
        }

        // Async Chunk Loading
        if (NovaAPIConfig.ASYNC_CHUNK_LOADING.get()) {
            AsyncWorldGenHandler.enable(); // Optional if you’re using async support
            NovaAPI.LOGGER.info("[Nova API] Async Chunk Loading enabled.");
        }

        NovaAPI.LOGGER.info("[Nova API] Local Mode fully initialized.");
    }


    public static void connectToDedicatedServer(String serverIP, MinecraftServer server) {
        NovaAPI.LOGGER.info("[Nova API] Connecting to Dedicated Nova API Server at " + serverIP + "...");

        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverIP, 8443)) {
                socket.startHandshake();

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Use IP address as base for a consistent UUID
                String identity = server.getLocalIp(); // You could use server.getServerModName() or something else unique
                UUID serverUUID = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
                String authToken = generateAuthToken(serverUUID);

                out.println("AUTH " + serverUUID + " " + authToken);
                String response = in.readLine();

                if ("OK".equalsIgnoreCase(response)) {
                    NovaAPI.LOGGER.info("[Nova API] Successfully authenticated with Nova API Server.");
                } else {
                    NovaAPI.LOGGER.warn("[Nova API] Authentication failed: " + response);
                }

            } catch (Exception e) {
                NovaAPI.LOGGER.error("[Nova API] Secure connection to API server failed.", e);
            }

        } catch (Exception e) {
            NovaAPI.LOGGER.error("[Nova API] Unable to initialize secure socket.", e);
        }
    }

    private static String generateAuthToken(UUID serverUUID) {
        String raw = "nova_secret:" + serverUUID;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isDedicatedMode() {
        return isDedicatedMode;
    }
}