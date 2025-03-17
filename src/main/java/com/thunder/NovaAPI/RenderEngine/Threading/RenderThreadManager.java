package com.thunder.NovaAPI.RenderEngine.Threading;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.HashSet;
import java.util.Set;

public class RenderThreadManager {
    private static final ExecutorService RENDER_THREAD = Executors.newFixedThreadPool(2);
    private static final Set<String> incompatibleMods = new HashSet<>();

    /**
     * Submits a task to the dedicated render thread.
     * If the mod has been marked as incompatible, it runs the task on the main thread.
     *
     * @param modName Name of the mod submitting the render task.
     * @param task The render task to execute.
     */
    public static void executeWithSafeGuard(String modName, Runnable task) {
        if (incompatibleMods.contains(modName)) {
            task.run(); // Run on the main thread if the mod is marked as incompatible
            return;
        }

        RENDER_THREAD.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                markModAsIncompatible(modName, e);
                task.run(); // Retry on main thread
            }
        });
    }

    /**
     * Marks a mod as incompatible if it fails on the render thread.
     * Future tasks from this mod will be executed on the main thread.
     *
     * @param modName The name of the mod.
     * @param e The exception thrown by the mod.
     */
    private static void markModAsIncompatible(String modName, Exception e) {
        if (incompatibleMods.add(modName)) { // Avoid duplicate logs
            System.err.println("[NovaRenderThreadManager] Mod " + modName + " caused a render crash! Moving it to the main thread.");
            logIncompatibleMod(modName, e);
        }
    }

    /**
     * Logs incompatible mods to a file for debugging.
     *
     * @param modName The mod that failed.
     * @param e The error that occurred.
     */
    private static void logIncompatibleMod(String modName, Exception e) {
        try (FileWriter writer = new FileWriter("novaapi_render_thread_incompatibility.log", true)) {
            writer.write("Mod: " + modName + " is incompatible with the render thread.\n");
            writer.write("Error: " + e.getMessage() + "\n\n");
        } catch (IOException ex) {
            System.err.println("[NovaRenderThreadManager] Failed to write incompatibility log.");
            ex.printStackTrace();
        }
    }

    /**
     * Executes an OpenGL render task on the correct thread.
     *
     * @param task The OpenGL rendering task.
     */
    public static void executeRenderTask(Runnable task) {
        RenderSystem.recordRenderCall((RenderCall) task);
    }

    /**
     * Shuts down the render thread safely.
     */
    public static void shutdown() {
        RENDER_THREAD.shutdown();
    }
}
