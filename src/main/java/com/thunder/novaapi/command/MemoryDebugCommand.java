package com.thunder.novaapi.command;

import com.thunder.NovaAPI.Core.NovaAPI;
import com.thunder.novaapi.cache.RegionScopedCache;
import com.thunder.novaapi.task.BackgroundTaskScheduler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Dev-only command to report heap usage and cache pressure. Helps track memory like TPS.
 */
public final class MemoryDebugCommand {
    private MemoryDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("novaapi")
                .then(Commands.literal("memory")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            sendMemoryReport(ctx.getSource(), NovaAPI.REGION_CACHE);
                            return 1;
                        }))
                .then(Commands.literal("tasks")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            BackgroundTaskScheduler.submit(ctx.getSource().getLevel().dimension(), 0L, () -> {
                                // no-op sanity check; keeps dimension usage lightweight
                            });
                            ctx.getSource().sendSystemMessage(Component.literal("Background task executor is active (no Level retention)."));
                            return 1;
                        })));
    }

    private static void sendMemoryReport(CommandSourceStack source, RegionScopedCache<?> cache) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        RegionScopedCache.Metrics metrics = cache.metrics();

        source.sendSystemMessage(Component.literal("NovaAPI Memory Report"));
        source.sendSystemMessage(Component.literal("- Heap used: " + humanReadableBytes(used)));
        source.sendSystemMessage(Component.literal("- Heap max:  " + humanReadableBytes(max)));
        source.sendSystemMessage(Component.literal("- Cache dimensions: " + metrics.trackedDimensions()));
        source.sendSystemMessage(Component.literal("- Cache entries: " + metrics.cachedEntries()));
        source.sendSystemMessage(Component.literal("- Cache TTL (ms): " + metrics.ttlMillis()));
        source.sendSystemMessage(Component.literal("- Cache cap per dimension: " + metrics.maxEntriesPerDimension()));
    }

    private static String humanReadableBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
