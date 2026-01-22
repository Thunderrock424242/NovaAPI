# NovaAPI Mod Developer Guide

This document describes the public APIs that other NeoForge mods can call to integrate with NovaAPI.

## Setup (Gradle + NeoForge)

1. **Add NovaAPI as a dependency in Gradle** (use the version that matches your modpack):

   ```gradle
   dependencies {
       // Example coordinates - replace with the correct published version
       modImplementation "com.thunder:novaapi:<version>"
   }
   ```

2. **Declare NovaAPI as a dependency** in your `neoforge.mods.toml`:

   ```toml
   [[dependencies.yourmodid]]
       modId="novaapi"
       mandatory=true
       versionRange="[<version>,)"
       ordering="NONE"
       side="BOTH"
   ```

## Async Task API

Use `AsyncTaskAPI` to offload work to NovaAPI’s worker executors while safely handing results back to the
main server thread.

```java
import com.thunder.novaapi.api.async.AsyncTaskAPI;
import com.thunder.novaapi.async.MainThreadTask;

AsyncTaskAPI.submitCpuTask("my-task", () -> {
    // Heavy computation off-thread
    return java.util.Optional.of((MainThreadTask) server -> {
        // Safe main-thread apply
    });
});
```

### Async Task Hooks

Register an `AsyncTaskListener` to observe queue health and rejections:

```java
import com.thunder.novaapi.api.async.AsyncTaskAPI;
import com.thunder.novaapi.api.async.AsyncTaskListener;
import com.thunder.novaapi.api.async.AsyncTaskType;

AsyncTaskAPI.registerListener(new AsyncTaskListener() {
    @Override
    public void onTaskQueued(String label, AsyncTaskType type, int backlog, int queueSize) {
        // Track queue pressure for your mod
    }

    @Override
    public void onTaskRejected(String label, AsyncTaskType type, int backlog, int queueSize) {
        // React to pressure or back off
    }

    @Override
    public void onMainThreadQueueDrained(int processed, int backlog) {
        // Observe per-tick apply throughput
    }
});
```

## Chunk Streaming API

NovaAPI exposes its chunk streaming pipeline for mods that need to request, save, or observe chunk data.

```java
import com.thunder.novaapi.api.chunk.ChunkStreamAPI;
import com.thunder.novaapi.chunk.ChunkTicketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;

ResourceKey<Level> dimension = level.dimension();
ChunkPos pos = new ChunkPos(x, z);
ChunkStreamAPI.requestChunk(dimension, pos, ChunkTicketType.PLAYER, level.getGameTime());
```

### Chunk Streaming Hooks

Register a `ChunkStreamListener` to observe when NovaAPI loads, saves, or flushes chunks:

```java
import com.thunder.novaapi.api.chunk.ChunkStreamAPI;
import com.thunder.novaapi.api.chunk.ChunkStreamListener;
import com.thunder.novaapi.chunk.ChunkTicketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

ChunkStreamAPI.registerListener(new ChunkStreamListener() {
    @Override
    public void onChunkRequested(ResourceKey<Level> dimension, ChunkPos pos, ChunkTicketType ticketType, long gameTime) {
        // Track ticket usage
    }

    @Override
    public void onChunkLoaded(ResourceKey<Level> dimension, ChunkPos pos, boolean warmCacheHit) {
        // React to loaded chunk payloads
    }
});
```

## Render Engine APIs

NovaAPI’s render engine exposes several client-side helpers:

- **Overlay batching**: `RenderOverlayAPI.queueOverlay(...)`
- **Particle culling**: `ParticleCullingAPI.queueParticle(...)`
- **Render thread submission**: `RenderThreadAPI.submitRenderTask(...)`

These helpers are in `com.thunder.novaapi.RenderEngine.API` and are safe to call from client-side code.

## Best Practices

- Prefer NovaAPI’s async executors for heavy work to avoid server tick stalls.
- Use the listener hooks to surface metrics in your own debug UI or telemetry.
- Keep listener logic lightweight; hooks run on game or worker threads.
