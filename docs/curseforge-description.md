# Nova API — CurseForge Description (NeoForge 1.21.1)

## Overview
Nova API is a server-first performance and infrastructure library for NeoForge mods on Minecraft 1.21.1. It ships an async task pipeline, chunk streaming cache, and tooling to keep heavily modded servers smooth while giving developers battle-tested building blocks.

## Key Features
- **Chunk streaming & caching** – Hot/warm chunk caches with configurable TTLs, per-dimension executors, and batched flushes to reduce churn during exploration or automation. Compression level and codec are configurable for disk and network payloads. 【F:src/main/java/com/thunder/NovaAPI/chunk/ChunkStreamingConfig.java†L14-L196】
- **Async task system** – A configurable worker pool (cores − 1 by default) with per-tick application caps and optional timeouts to offload CPU-heavy work without starving the main thread. 【F:src/main/java/com/thunder/NovaAPI/async/AsyncThreadingConfig.java†L12-L101】
- **Performance advisor loop** – Observes tick spikes, generates mitigation actions, and logs guidance when the tick budget is exceeded. 【F:src/main/java/com/thunder/NovaAPI/Core/NovaAPI.java†L28-L146】
- **Memory-safe region cache** – Dimension-aware, TTL-governed cache with eviction to prevent chunk retention leaks. 【F:src/main/java/com/thunder/novaapi/cache/RegionScopedCache.java†L13-L97】
- **Admin/Dev commands** – `/novaapi memory` reports heap usage and cache pressure; `/novaapi tasks` sanity-checks the background scheduler. 【F:src/main/java/com/thunder/novaapi/command/MemoryDebugCommand.java†L18-L72】
- **Analytics & background services** – Initializes analytics tracking, async chunk streaming, background task scheduler, and safe shutdown hooks on server lifecycle events. 【F:src/main/java/com/thunder/NovaAPI/Core/NovaAPI.java†L63-L160】

## Requirements
- **Loader:** NeoForge `21.1.217` or newer. 【F:build.gradle†L26-L47】
- **Minecraft:** 1.21.1. 【F:build.gradle†L86-L102】
- **Java:** 21 (matches the runtime Mojang ships from 1.20.5+). 【F:build.gradle†L20-L24】

## Configuration
Nova API ships multiple TOML configs (generated on first run):
- `novaapi-common.toml` – Core toggles, dedicated server target. 【F:src/main/java/com/thunder/NovaAPI/config/NovaAPIConfig.java†L13-L63】
- `novaapi-async.toml` – Async worker pool size, queue limits, tick-application budget, optional timeouts and debug logging. 【F:src/main/java/com/thunder/NovaAPI/async/AsyncThreadingConfig.java†L12-L101】
- `novaapi-chunk-streaming.toml` – Hot/warm cache sizes, ticket TTLs, flush cadence, compression codec/level, IO thread counts, throttles for redstone/fluids, and random-tick scaling. 【F:src/main/java/com/thunder/NovaAPI/chunk/ChunkStreamingConfig.java†L14-L196】

### Recommended starting points
- Keep the defaults if you are unsure—they target mid-size modded servers.
- Lower `chunkStreaming.maxParallelIo` or `chunkStreaming.ioThreads` if your disk is slow.
- Reduce `asyncThreading.maxThreads` if you share hardware with other server processes.

## Commands
- `/novaapi memory` – Shows heap usage and region cache metrics (requires permission level 2).
- `/novaapi tasks` – Verifies the background executor is alive without holding level references.

## Installation
1. Install NeoForge 21.1.x on your 1.21.1 instance.
2. Drop the Nova API jar into the `mods` folder on both client and server.
3. Launch once to generate configs, then tune `novaapi-async.toml` and `novaapi-chunk-streaming.toml` to match your hardware.

## For Mod Developers
- Hook into the async task system to keep main-thread work lean; gate long jobs behind `AsyncTaskManager` and apply results via its main-thread queue.
- Use the chunk streaming cache for cross-dimensional data that must be reload-safe and leak-free.
- When integrating analytics or background workers, follow Nova API’s lifecycle hooks to start/stop services cleanly (see `NovaAPI` entrypoint).
- Follow the [Memory and Performance Guidelines](./memory-and-performance-guidelines.md) to avoid hidden retention issues.

## Support & Feedback
- NeoForge Discord: https://discord.neoforged.net/
- Issues/feature requests: include logs with Nova API’s advisor output and your `novaapi-*.toml` files for best results.
