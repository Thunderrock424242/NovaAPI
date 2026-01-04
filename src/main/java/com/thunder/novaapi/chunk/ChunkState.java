package com.thunder.novaapi.chunk;

/**
 * Lifecycle state for a chunk managed by the streaming system.
 */
public enum ChunkState {
    UNLOADED,
    QUEUED,
    LOADING,
    READY,
    ACTIVE,
    UNLOADING
}
