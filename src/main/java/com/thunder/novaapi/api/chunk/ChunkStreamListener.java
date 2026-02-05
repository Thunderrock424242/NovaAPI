package com.thunder.novaapi.api.chunk;

import com.thunder.novaapi.chunk.ChunkLifecycleStage;
import com.thunder.novaapi.chunk.ChunkTicketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Receives chunk streaming callbacks from NovaAPI.
 */
public interface ChunkStreamListener {
    /**
     * Called when a chunk is requested through the streaming API.
     * The dimension may be null when the request originates from a non-dimensioned call.
     */
    default void onChunkRequested(ResourceKey<Level> dimension, ChunkPos pos, ChunkTicketType ticketType, long gameTime) {
    }

    /**
     * Called after a chunk is loaded or resolved from cache.
     * The dimension may be null when the request originates from a non-dimensioned call.
     */
    default void onChunkLoaded(ResourceKey<Level> dimension, ChunkPos pos, boolean warmCacheHit) {
    }

    /**
     * Called when a chunk save is queued through the streaming API.
     * The dimension may be null when the request originates from a non-dimensioned call.
     */
    default void onChunkSaveQueued(ResourceKey<Level> dimension, ChunkPos pos, long gameTime) {
    }

    /**
     * Called when a chunk advances through a high-level pipeline stage.
     * The dimension may be null when the event is emitted from non-dimensioned internals.
     */
    default void onChunkLifecycleStage(ResourceKey<Level> dimension, ChunkPos pos, ChunkLifecycleStage stage, long gameTime) {
    }

    /**
     * Called when a chunk is flushed from the streaming cache.
     */
    default void onChunkFlushed(ChunkPos pos) {
    }

    /**
     * Called when all pending chunk saves are flushed.
     */
    default void onFlushAll(long gameTime) {
    }
}
