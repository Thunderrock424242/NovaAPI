package com.thunder.novaapi.SavingSystem;

import com.thunder.novaapi.Core.NovaAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.thunder.novaapi.Core.NovaAPI.MOD_ID;


@EventBusSubscriber(modid = MOD_ID)
public class ChunkSaveManager {
    private static final ExecutorService CHUNK_SAVE_THREAD = Executors.newSingleThreadExecutor();
    private static final ConcurrentLinkedQueue<LevelChunk> chunkSaveQueue = new ConcurrentLinkedQueue<>();
    private static final Set<ChunkPos> queuedChunks = ConcurrentHashMap.newKeySet();

    public static void queueChunkSave(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        if (queuedChunks.add(pos)) {
            chunkSaveQueue.add(chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverWorld) {
            ChunkPos pos = event.getChunk().getPos();
            LevelChunk chunk = serverWorld.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                queueChunkSave(chunk);
            }
        }
    }

    public static void startProcessing() {
        CHUNK_SAVE_THREAD.execute(() -> {
            while (!chunkSaveQueue.isEmpty()) {
                LevelChunk chunk = chunkSaveQueue.poll();
                if (chunk != null) {
                    queuedChunks.remove(chunk.getPos());
                    saveChunk(chunk);
                }
            }
        });
    }

    private static void saveChunk(LevelChunk chunk) {
        try {
            ServerLevel world = (ServerLevel) chunk.getLevel();
            ServerChunkCache chunkCache = world.getChunkSource();
            ChunkMap chunkMap = chunkCache.chunkMap;
            ChunkPos chunkPos = chunk.getPos();

            chunkMap.write(chunkPos, chunkMap.read(chunkPos).get().orElseGet(() -> new CompoundTag()));
            // You can modify the data here if needed before writing

            // Reduce to debug or trace logging
            NovaAPI.LOGGER.debug("[NovaAPI] Async saved chunk at " + chunkPos);
        } catch (Exception e) {
            NovaAPI.LOGGER.error("[NovaAPI] Error saving chunk at " + chunk.getPos(), e);
        }
    }

    public static void shutdown() {
        CHUNK_SAVE_THREAD.shutdown();
    }
}