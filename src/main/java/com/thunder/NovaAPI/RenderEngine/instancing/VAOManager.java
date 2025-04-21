package com.thunder.NovaAPI.RenderEngine.instancing;

import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and manages VAOs for entity models.
 * If loading fails, logs the error and disables instancing for that model.
 */
public class VAOManager {
    private static final Map<ResourceLocation, Integer> loadedVAOs = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> disabledModels = new ConcurrentHashMap<>();

    /**
     * Attempts to load a VAO for the given model path.
     * If the model is already loaded, returns the cached VAO.
     */
    public static int getOrLoadVAO(ResourceLocation modelPath) {
        if (disabledModels.getOrDefault(modelPath, false)) {
            return -1; // Disabled due to prior failure
        }

        return loadedVAOs.computeIfAbsent(modelPath, path -> {
            try {
                return loadVAOFromModel(path);
            } catch (Exception e) {
                System.err.println("[VAOManager] Failed to load model: " + path + " — Falling back to vanilla.");
                disabledModels.put(path, true);
                return -1;
            }
        });
    }

    /**
     * Tries to load a VAO from the given model file.
     * Replace this stub with actual model parsing logic (OBJ, JSON, etc).
     */
    private static int loadVAOFromModel(ResourceLocation modelPath) throws Exception {
        // TODO: Actually parse and upload vertex data
        // Placeholder stub: generate dummy VAO
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Simulate failure for missing models
        if (modelPath.getPath().contains("default")) {
            throw new RuntimeException("Model not found or unsupported.");
        }

        // Setup buffers, attributes here...
        System.out.println("[VAOManager] Loaded VAO for model: " + modelPath);
        return vao;
    }

    /**
     * Checks if instanced rendering is enabled for a given model.
     */
    public static boolean isModelEnabled(ResourceLocation path) {
        return !disabledModels.getOrDefault(path, false);
    }
}