package com.thunder.novaapi.chunk;

/**
 * High-level orchestration stages for NovaAPI's chunk pipeline backend.
 */
public enum ChunkLifecycleStage {
    TICKET_CREATED,
    IO_READ_QUEUED,
    IO_READ_COMPLETED,
    COMPRESSION_DECODED,
    GENERATION_PREPARED,
    LIGHTING_PREPARED,
    DELTA_SYNC_QUEUED,
    IO_WRITE_COMPLETED,
    FLUSHED,
    EVICTED
}
