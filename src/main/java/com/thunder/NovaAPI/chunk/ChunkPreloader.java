package com.thunder.NovaAPI.chunk;

import com.thunder.NovaAPI.NovaAPI;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChunkPreloader {
    private static final int THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final ExecutorService CHUNK_LOADING_EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ConcurrentLinkedQueue<ChunkLoadRequest> chunkQueue = new ConcurrentLinkedQueue<>();

    public static void requestChunkLoad(ServerLevel world, int chunkX, int chunkZ) {
        chunkQueue.add(new ChunkLoadRequest(world, chunkX, chunkZ));
    }

    public static void processChunkQueue() {
        while (!chunkQueue.isEmpty()) {
            ChunkLoadRequest request = chunkQueue.poll();
            if (request != null) {
                CHUNK_LOADING_EXECUTOR.execute(() -> loadChunk(request));
            }
        }
    }

    private static void loadChunk(ChunkLoadRequest request) {
        ServerLevel world = request.world();
        ChunkPos pos = new ChunkPos(request.chunkX(), request.chunkZ());

        try {
            ServerChunkCache chunkCache = world.getChunkSource();
            LevelChunk chunk = chunkCache.getChunkNow(pos.x, pos.z);

            if (chunk == null) {
                // Load the chunk asynchronously
                LevelChunk loadedChunk = chunkCache.getChunk(pos.x, pos.z, true);

                if (loadedChunk != null) {
                    NovaAPI.LOGGER.info("[NovaAPI] Preloaded chunk at: " + pos);
                }
            }
        } catch (Exception e) {
            NovaAPI.LOGGER.error("[NovaAPI] Error loading chunk at " + pos, e);
        }
    }

    public static void shutdown() {
        CHUNK_LOADING_EXECUTOR.shutdown();
    }

    private record ChunkLoadRequest(ServerLevel world, int chunkX, int chunkZ) {}
}