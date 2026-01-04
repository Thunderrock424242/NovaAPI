package com.thunder.novaapi.RenderEngine.instancing;

import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
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
        // Simulate failure for missing models
        if (modelPath.getPath().contains("default")) {
            throw new RuntimeException("Model not found or unsupported.");
        }

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        float[] vertices = {
                -0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f
        };
        int[] indices = {
                0,1,2,2,3,0,
                4,5,6,6,7,4,
                0,1,5,5,4,0,
                2,3,7,7,6,2,
                1,2,6,6,5,1,
                3,0,4,4,7,3
        };

        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        java.nio.FloatBuffer vertBuf = BufferUtils.createFloatBuffer(vertices.length);
        vertBuf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertBuf, GL15.GL_STATIC_DRAW);

        int ebo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        java.nio.IntBuffer indexBuf = BufferUtils.createIntBuffer(indices.length);
        indexBuf.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuf, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

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