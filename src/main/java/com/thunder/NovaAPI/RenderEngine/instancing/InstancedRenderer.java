package com.thunder.NovaAPI.RenderEngine.instancing;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.util.*;

public class InstancedRenderer {

    /**
     * Renders all visible entities using instanced rendering, grouped by model.
     */
    public static void renderAll(Iterable<Entity> entities, Frustum frustum) {
        Map<ResourceLocation, List<Entity>> batched = new HashMap<>();

        for (Entity entity : entities) {
            if (!frustum.isVisible(entity.getBoundingBox())) continue;

            ResourceLocation modelPath = ModelRegistryHelper.getModelPath(entity.getType());
            batched.computeIfAbsent(modelPath, k -> new ArrayList<>()).add(entity);
        }

        for (Map.Entry<ResourceLocation, List<Entity>> entry : batched.entrySet()) {
            ResourceLocation modelPath = entry.getKey();
            List<Entity> entityList = entry.getValue();

            int vao = VAOManager.getOrLoadVAO(modelPath);
            if (!VAOManager.isModelEnabled(modelPath) || vao <= 0) continue;

            GL30.glBindVertexArray(vao);
            GL31.glDrawElementsInstanced(GL30.GL_TRIANGLES, 36, GL30.GL_UNSIGNED_INT, 0, entityList.size());
        }

        GL30.glBindVertexArray(0);





        for (Entity entity : entities) {
            if (!frustum.isVisible(entity.getBoundingBox())) continue;

            ResourceLocation modelPath = ModelRegistryHelper.getModelPath(entity.getType());
            batched.computeIfAbsent(modelPath, k -> new ArrayList<>()).add(entity);
        }

        for (Map.Entry<ResourceLocation, List<Entity>> entry : batched.entrySet()) {
            ResourceLocation modelPath = entry.getKey();
            List<Entity> entityList = entry.getValue();

            int vao = VAOManager.getOrLoadVAO(modelPath);
            if (!VAOManager.isModelEnabled(modelPath) || vao <= 0) continue;

            GL30.glBindVertexArray(vao);
            GL31.glDrawElementsInstanced(GL30.GL_TRIANGLES, 36, GL30.GL_UNSIGNED_INT, 0, entityList.size());
        }

        GL30.glBindVertexArray(0); // clean unbind
    }
}
