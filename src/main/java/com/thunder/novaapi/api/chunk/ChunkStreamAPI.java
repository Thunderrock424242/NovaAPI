package com.thunder.novaapi.api.chunk;

import com.thunder.novaapi.chunk.ChunkLoadResult;
import com.thunder.novaapi.chunk.ChunkStreamManager;
import com.thunder.novaapi.chunk.ChunkTicketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API surface for NovaAPI chunk streaming hooks and helpers.
 */
public final class ChunkStreamAPI {
    private static final List<ChunkStreamListener> LISTENERS = new CopyOnWriteArrayList<>();

    private ChunkStreamAPI() {
    }

    public static void registerListener(ChunkStreamListener listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void unregisterListener(ChunkStreamListener listener) {
        LISTENERS.remove(listener);
    }

    public static CompletableFuture<ChunkLoadResult> requestChunk(ResourceKey<Level> dimension, ChunkPos pos, ChunkTicketType ticketType, long gameTime) {
        return ChunkStreamManager.requestChunk(dimension, pos, ticketType, gameTime);
    }

    public static void scheduleSave(ResourceKey<Level> dimension, ChunkPos pos, CompoundTag payload, long gameTime) {
        ChunkStreamManager.scheduleSave(dimension, pos, payload, gameTime);
    }

    public static void markActive(ChunkPos pos, long gameTime) {
        ChunkStreamManager.markActive(pos, gameTime);
    }

    public static void flushChunk(ChunkPos pos) {
        ChunkStreamManager.flushChunk(pos);
    }

    public static void flushAll(long gameTime) {
        ChunkStreamManager.flushAll(gameTime);
    }

    public static void notifyChunkRequested(ResourceKey<Level> dimension, ChunkPos pos, ChunkTicketType ticketType, long gameTime) {
        for (ChunkStreamListener listener : LISTENERS) {
            listener.onChunkRequested(dimension, pos, ticketType, gameTime);
        }
    }

    public static void notifyChunkLoaded(ResourceKey<Level> dimension, ChunkPos pos, boolean warmCacheHit) {
        for (ChunkStreamListener listener : LISTENERS) {
            listener.onChunkLoaded(dimension, pos, warmCacheHit);
        }
    }

    public static void notifyChunkSaveQueued(ResourceKey<Level> dimension, ChunkPos pos, long gameTime) {
        for (ChunkStreamListener listener : LISTENERS) {
            listener.onChunkSaveQueued(dimension, pos, gameTime);
        }
    }

    public static void notifyChunkFlushed(ChunkPos pos) {
        for (ChunkStreamListener listener : LISTENERS) {
            listener.onChunkFlushed(pos);
        }
    }

    public static void notifyFlushAll(long gameTime) {
        for (ChunkStreamListener listener : LISTENERS) {
            listener.onFlushAll(gameTime);
        }
    }
}
