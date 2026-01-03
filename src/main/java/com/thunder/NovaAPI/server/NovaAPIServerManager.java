package com.thunder.NovaAPI.server;

import com.thunder.NovaAPI.Core.NovaAPI;
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

        // Async Chunk Loading
        if (NovaAPIConfig.ASYNC_CHUNK_LOADING.get()) {
            AsyncWorldGenHandler.enable(); // Optional if you’re using async support
            NovaAPI.LOGGER.info("[Nova API] Async Chunk Loading enabled.");
        }

        NovaAPI.LOGGER.info("[Nova API] Local Mode fully initialized.");
    }


    /**
     * Attempts to connect & authenticate to the dedicated Nova API server.
     * @return true if handshake succeeded, false otherwise.
     */
    public static boolean connectToDedicatedServer(String serverIP, MinecraftServer server) {
        NovaAPI.LOGGER.info("[Nova API] Connecting to Dedicated Server at " + serverIP + "...");
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(serverIP, 8443)) {
                socket.startHandshake();
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // generate and send auth
                String idSeed = server.getLocalIp();
                UUID uuid = UUID.nameUUIDFromBytes(idSeed.getBytes(StandardCharsets.UTF_8));
                String token = generateAuthToken(uuid);
                out.println("AUTH " + uuid + " " + token);

                String resp = in.readLine();
                if ("OK".equalsIgnoreCase(resp)) {
                    NovaAPI.LOGGER.info("[Nova API] Authenticated with Dedicated Server.");
                    return true;
                } else {
                    NovaAPI.LOGGER.warn("[Nova API] Dedicated Server denied access: {}", resp);
                    return false;
                }
            }
        } catch (Exception e) {
            NovaAPI.LOGGER.error("[Nova API] Failed to connect/authenticate to Dedicated Server.", e);
            return false;
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