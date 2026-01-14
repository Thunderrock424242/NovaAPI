package com.thunder.novaapi.optimizations;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncWorldGenHandler {

    // Executor service to handle asynchronous tasks
    private static final int ASYNC_WORKER_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    private static final ExecutorService worldGenExecutor = Executors.newFixedThreadPool(ASYNC_WORKER_COUNT, new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "novaapi-worldgen-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });

    // Queue to ensure thread-safe operations
    private static final LinkedBlockingQueue<ChunkLoadRequest> mainThreadTasks = new LinkedBlockingQueue<>();
    private static final LinkedBlockingQueue<Runnable> mainThreadRunnables = new LinkedBlockingQueue<>();
    private static final int BASE_MAIN_THREAD_TASKS_PER_TICK = 8;
    private static final int MAX_MAIN_THREAD_TASKS_PER_TICK = 48;
    private static final int MAX_MAIN_THREAD_RUNNABLES_PER_TICK = 4;

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (hasInitializedWorld(level)) {
            return;
        }

        CompletableFuture.runAsync(() -> generateChunksAsync(level), worldGenExecutor)
                .thenRun(() -> scheduleMainThreadTask(() -> {
                    markWorldInitialized(level);
                    finalizeGeneration(level);
                }));
    }

    private static void generateChunksAsync(ServerLevel level) {
        // Example chunk generation logic
        // Heavy operations performed asynchronously
        for (int chunkX = -10; chunkX <= 10; chunkX++) {
            for (int chunkZ = -10; chunkZ <= 10; chunkZ++) {
                // Custom chunk generation or modification
                performHeavyGeneration(chunkX, chunkZ, level);
            }
        }
    }

    private static void performHeavyGeneration(int chunkX, int chunkZ, ServerLevel level) {
        scheduleMainThreadChunkLoad(level, new ChunkPos(chunkX, chunkZ));
    }

    private static void finalizeGeneration(ServerLevel level) {
        // Tasks that must run on the main thread after async generation
        level.players().forEach(player -> player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Async World Generation Complete!")));
    }

    private static boolean hasInitializedWorld(ServerLevel level) {
        return Files.exists(getInitializationMarkerPath(level));
    }

    private static void markWorldInitialized(ServerLevel level) {
        Path markerPath = getInitializationMarkerPath(level);
        try {
            Files.createDirectories(markerPath.getParent());
            if (!Files.exists(markerPath)) {
                Files.createFile(markerPath);
            }
        } catch (IOException e) {
            System.out.println("[Nova API] Failed to write world initialization marker: " + e.getMessage());
        }
    }

    private static Path getInitializationMarkerPath(ServerLevel level) {
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("novaapi")
                .resolve("async_worldgen.marker");
    }

    // Called from tick event or similar main-thread context
    public static void executeMainThreadTasks() {
        int processed = 0;
        int queueDepth = mainThreadTasks.size();
        int budget = Math.min(MAX_MAIN_THREAD_TASKS_PER_TICK,
                BASE_MAIN_THREAD_TASKS_PER_TICK + Math.max(0, queueDepth / 16));
        ChunkLoadRequest request;
        while (processed < budget && (request = mainThreadTasks.poll()) != null) {
            request.level().getChunk(request.pos().x, request.pos().z);
            processed++;
        }

        int runnableProcessed = 0;
        Runnable runnable;
        while (runnableProcessed < MAX_MAIN_THREAD_RUNNABLES_PER_TICK
                && (runnable = mainThreadRunnables.poll()) != null) {
            runnable.run();
            runnableProcessed++;
        }
    }

    private static void scheduleMainThreadChunkLoad(ServerLevel level, ChunkPos pos) {
        mainThreadTasks.offer(new ChunkLoadRequest(level, pos));
    }

    private static void scheduleMainThreadTask(Runnable task) {
        mainThreadRunnables.offer(task);
    }

    private record ChunkLoadRequest(ServerLevel level, ChunkPos pos) {
    }

    // Shutdown executor service when server closes
    public static void shutdownExecutor() {
        worldGenExecutor.shutdownNow();
    }

    public static void enable() {
        // Register this class to listen for events (e.g., LevelEvent.Load)
        NeoForge.EVENT_BUS.register(AsyncWorldGenHandler.class);

        // Optionally, run main thread tasks every tick
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            executeMainThreadTasks();
        });

        System.out.println("[Nova API] AsyncWorldGenHandler enabled and listening for world events.");
    }

}
