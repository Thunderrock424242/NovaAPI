# Chunk Pipeline Backend (Staged Lifecycle)

NovaAPI now exposes a staged chunk lifecycle backend so large NeoForge modpacks can treat chunk orchestration as a first-class performance surface.

## Goals

- Keep server TPS stable under high chunk churn.
- Decouple ticketing, I/O, compression/decompression, generation, lighting, and delta persistence.
- Expose stage transitions so mod authors can instrument pressure and implement pack-specific backoff policies.

## Lifecycle Stages

`ChunkLifecycleStage` models the backend lifecycle:

1. `TICKET_CREATED`
2. `IO_READ_QUEUED`
3. `IO_READ_COMPLETED`
4. `COMPRESSION_DECODED`
5. `GENERATION_PREPARED`
6. `LIGHTING_PREPARED`
7. `DELTA_SYNC_QUEUED`
8. `IO_WRITE_COMPLETED`
9. `FLUSHED`
10. `EVICTED`

These are emitted through `ChunkStreamListener#onChunkLifecycleStage(...)`.

## Integration Pattern (NeoForge)

- Use `ChunkStreamAPI.requestChunk(...)` with explicit ticket type and game time.
- Register a `ChunkStreamListener` and collect per-stage timing histograms.
- Use the existing async/task telemetry to correlate chunk queue pressure with CPU and I/O saturation.
- Back off non-critical chunk activity when I/O stages dominate tick budget.

## Operational Recommendations for Large Modpacks

- Prefer dimension-scoped dashboards (Overworld/Nether/End behave differently).
- Alert on rising `IO_READ_QUEUED -> IO_READ_COMPLETED` latency.
- Alert on high `DELTA_SYNC_QUEUED` backlog during autosaves.
- Keep warm/hot cache limits tuned to your memory budget and region-travel profile.

## Implementation Notes

Current staged signals are emitted by `ChunkStreamManager` without changing existing API behavior for mods that only consume request/load/save hooks.
