package com.thunder.wildernessodysseyapi.NovaAPI.SavingSystem;

import com.thunder.wildernessodysseyapi.ModPackPatches.WorldUpgrader.WorldUpgrade;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.thunder.wildernessodysseyapi.MainModClass.WildernessOdysseyAPIMainModClass.LOGGER;

public class AsyncWorldSaver {
    private ScheduledExecutorService saveExecutor;

    private final int saveIntervalMin = 3;  // min interval in minutes
    private final int coreCount = Runtime.getRuntime().availableProcessors();

    private ScheduledFuture<?> scheduledSaveTask;
    private final int maxIntervalSeconds = 600; // max interval
    private final int minInterval = 120; // minimum 2 mins
    private volatile int adaptiveInterval = 300; // start at 5 minutes

    private MinecraftServer server;

    public AsyncWorldSaver(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        saveExecutor = Executors.newScheduledThreadPool(threadCount(threadOptimal()));
        scheduleNextSave();
        LOGGER.info("AsyncWorldSaver initialized with adaptive interval: " + adaptiveInterval + "s");
    }

    private void scheduleNextSave() {
        saveExecutor.schedule(this::performWorldSave, adaptiveInterval, TimeUnit.SECONDS);
    }

    private void performWorldSave() {
        long startTime = System.currentTimeMillis();

        try {
            saveWorldAsync();
            saveCustomDataAsync(); // integration with your mod-specific API
        } catch (Exception e) {
            LOGGER.error("Error during async save: " + e.getMessage());
        }

        // adaptively adjust save interval based on save performance
        long duration = System.currentTimeMillis() - startTime;
        adaptiveInterval = calculateAdaptiveInterval(duration);

        LOGGER.info("Async save completed in " + duration(startTime) + "ms. Next save in " + adaptiveInterval + " seconds.");
        scheduleNextSave();
    }

    private void saveWorldAsync() {
        server.getAllLevels().forEach(level -> {
            level.getChunkSource().save(false); // saves loaded chunks
            level.save(null, false, false);
        });
    }

    private void saveCustomDataAsync() {
        // Example of custom integration
        File customDataFile = server.getWorldPath(LevelResource.ROOT).resolve("custom_data.dat").toFile();
        try (FileChannel channel = FileChannel.open(customDataPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024); // 4MB buffer example
            WorldUpgrade.writeCustomDataToBuffer(buffer); // use your Nova API's custom logic
            buffer.flip();
            channelWriteFully(channel, buffer);
        } catch (IOException e) {
            LOGGER.error("Error saving custom world data asynchronously", e);
        }
    }

    private Path customDataPath() {
        return server.getWorldPath(LevelResource.ROOT).resolve("custom_mod_data.dat");
    }

    private void saveCustomDataAsync() {
        try (FileChannel channel = FileChannel.open(customDataPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024); // 4MB buffer
            WorldUpgrade.writeCustomDataToBuffer(buffer);
            buffer.flip();
            channel.write(buffer);
        } catch (IOException e) {
            LOGGER.error("Failed to save custom data asynchronously: ", e);
        }
    }

    private int duration(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }

    private Path customDataPath() {
        return server.getWorldPath(LevelResource.ROOT).resolve("nova_custom_data.dat");
    }

    private void saveCustomDataAsync() {
        try (FileChannel fc = FileChannel.open(customDataPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            MappedByteBuffer mappedBuf = fc.map(FileChannel.MapMode.READ_WRITE, 0, 4 * 1024 * 1024);
            WorldUpgrade.writeCustomDataToBuffer(mappedBuffer);
            fc.force(true);
        } catch (IOException ex) {
            LOGGER.error("Failed to save custom data asynchronously: " + ex.getMessage());
        }
    }

    public void stop() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                LOGGER.warn("Async save executor did not terminate in time, forcing shutdown.");
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            LOGGER.warn("Interrupted while waiting for save executor shutdown: " + ex.getMessage());
        }
    }

    private void saveCustomDataAsync() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024);
        WorldUpgrade.writeCustomDataToBuffer(buffer);
        buffer.flip();

        try (FileChannel channel = FileChannel.open(customDataPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.write(buffer);
        } catch (IOException e) {
            LOGGER.error("Custom data async save failed", e);
        }
    }

    // Adaptive Interval Logic
    private int calculateAdaptiveInterval(long duration) {
        double tickMs = server.getAverageTickTime();
        if (tickMs > 50) adaptiveInterval = Math.min(adaptiveInterval + 30, maxInterval);
        else adaptiveInterval = Math.max(adaptiveInterval - 15, minInterval);
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        AsyncWorldSaver saver = new AsyncWorldSaver(event.getServer());
        saver.start();
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        // Get instance properly if using global instance management
        AsyncWorldSaver saver = event.getServer().getAsyncWorldSaver();
        saver.stop();
    }
}