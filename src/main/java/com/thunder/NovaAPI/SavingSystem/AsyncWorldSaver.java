package com.thunder.NovaAPI.SavingSystem;

import com.thunder.NovaAPI.Core.NovaAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AsyncWorldSaver {

    private final ScheduledExecutorService saveExecutor;

    private final MinecraftServer server;
    private int adaptiveInterval = 300; // Initial interval (5 minutes)
    private static final int MAX_INTERVAL = 600; // 10 min max
    private static final int MIN_INTERVAL = 120; // 2 min min

    public AsyncWorldSaver(MinecraftServer server) {
        this.server = server;
        this.saveExecutor = Executors.newScheduledThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
        );
    }

    public void start() {
        scheduleNextSave();
        NovaAPI.LOGGER.info("AsyncWorldSaver started with interval: " + adaptiveInterval + " seconds.");
    }

    private void scheduleNextSave() {
        saveExecutor.schedule(this::performWorldSave, adaptiveInterval, TimeUnit.SECONDS);
    }

    private void performWorldSave() {
        long startTime = System.currentTimeMillis();

        try {
            saveWorldAsync();
            saveCustomDataAsync();
        } catch (Exception e) {
            NovaAPI.LOGGER.error("Async save error:", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        calculateAdaptiveInterval(duration);
        NovaAPI.LOGGER.info("Async save completed in " + duration + "ms. Next save in " + adaptiveInterval + " seconds.");

        scheduleNextSave();
    }

    private void saveWorldAsync() {
        server.getAllLevels().forEach(level -> {
            level.getChunkSource().save(false);
            level.save(null, false, false);
        });
    }

    private void saveCustomDataAsync() {
        Path customDataPath = server.getWorldPath(LevelResource.ROOT).resolve("nova_custom_data.dat");

        try (FileChannel channel = FileChannel.open(customDataPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024);
            buffer.flip();
            channel.write(buffer);

        } catch (IOException e) {
            NovaAPI.LOGGER.error("Failed to save custom data asynchronously.", e);
        }
    }

    private void calculateAdaptiveInterval(long duration) {
        double tickMs = server.getTickCount() > 0 ? (server.getNextTickTime() - System.nanoTime()) / 1_000_000.0 : 50.0;

        if (tickMs > 50.0 || duration > 1000) {
            adaptiveInterval = Math.min(adaptiveInterval + 30, MAX_INTERVAL);
        } else {
            adaptiveInterval = Math.max(adaptiveInterval - 15, MIN_INTERVAL);
        }
    }

    public void stop() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                NovaAPI.LOGGER.warn("Save executor did not terminate in time; forcing shutdown.");
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            NovaAPI.LOGGER.error("Interrupted during save executor shutdown:", ex);
            saveExecutor.shutdownNow();
        }
    }

    // NeoForge Event handlers:
    private static AsyncWorldSaver INSTANCE;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        INSTANCE = new AsyncWorldSaver(event.getServer());
        INSTANCE.start();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (INSTANCE != null) {
            INSTANCE.stop();
        }
    }
}