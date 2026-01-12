package com.thunder.novaapi.chunk;

import com.thunder.novaapi.RenderEngine.Threading.RenderThreadManager;
import com.thunder.novaapi.task.BackgroundTaskScheduler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Queues chunk mesh rebuilds on background workers and schedules GPU uploads on the render thread.
 */
public final class ChunkMeshScheduler {
    private static final ConcurrentLinkedQueue<MeshRebuildRequest<?>> REBUILD_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Runnable> UPLOAD_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger IN_FLIGHT_REBUILDS = new AtomicInteger();
    private static ChunkStreamingConfig.ChunkConfigValues config = ChunkStreamingConfig.values();

    private ChunkMeshScheduler() {
    }

    public static void configure(ChunkStreamingConfig.ChunkConfigValues values) {
        config = values;
    }

    public static void shutdown() {
        REBUILD_QUEUE.clear();
        UPLOAD_QUEUE.clear();
        IN_FLIGHT_REBUILDS.set(0);
    }

    public static <T> void enqueue(ResourceKey<Level> dimension,
                                   ChunkPos pos,
                                   Supplier<T> rebuildWork,
                                   Consumer<T> uploadWork) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(rebuildWork, "rebuildWork");
        Objects.requireNonNull(uploadWork, "uploadWork");
        REBUILD_QUEUE.add(new MeshRebuildRequest<>(dimension, pos, rebuildWork, uploadWork));
    }

    public static void tick() {
        int rebuildBudget = Math.max(0, config.maxMeshRebuildsPerTick());
        for (int i = 0; i < rebuildBudget; i++) {
            MeshRebuildRequest<?> request = REBUILD_QUEUE.poll();
            if (request == null) {
                break;
            }
            dispatchRebuild(request);
        }

        int uploadBudget = Math.max(0, config.meshUploadBatchSize());
        for (int i = 0; i < uploadBudget; i++) {
            Runnable upload = UPLOAD_QUEUE.poll();
            if (upload == null) {
                break;
            }
            RenderThreadManager.executeRenderTask(upload);
        }
    }

    public static int inFlightRebuilds() {
        return IN_FLIGHT_REBUILDS.get();
    }

    public static int pendingRebuilds() {
        return REBUILD_QUEUE.size();
    }

    public static int pendingUploads() {
        return UPLOAD_QUEUE.size();
    }

    private static <T> void dispatchRebuild(MeshRebuildRequest<T> request) {
        IN_FLIGHT_REBUILDS.incrementAndGet();
        BackgroundTaskScheduler.submit(request.dimension(), request.pos().toLong(), () -> {
            T result = null;
            try {
                result = request.rebuildWork().get();
            } finally {
                IN_FLIGHT_REBUILDS.decrementAndGet();
            }
            enqueueUpload(() -> request.uploadWork().accept(result));
        });
    }

    private static void enqueueUpload(Runnable uploadTask) {
        if (uploadTask != null) {
            UPLOAD_QUEUE.add(uploadTask);
        }
    }

    private record MeshRebuildRequest<T>(
            ResourceKey<Level> dimension,
            ChunkPos pos,
            Supplier<T> rebuildWork,
            Consumer<T> uploadWork
    ) {
    }
}
