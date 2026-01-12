package com.thunder.novaapi.RenderEngine.instancing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.util.*;

public class InstancedRenderer {
    private static final double LOD0_MAX_DISTANCE_SQR = 16.0 * 16.0;
    private static final double LOD1_MAX_DISTANCE_SQR = 48.0 * 48.0;

    /**
     * Renders all visible entities using instanced rendering, grouped by model.
     */
    public static void renderAll(Iterable<Entity> entities, Frustum frustum) {
        Map<ResourceLocation, List<Entity>> batched = new HashMap<>();
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        for (Entity entity : entities) {
            if (!frustum.isVisible(entity.getBoundingBox())) continue;

            ResourceLocation modelPath = ModelRegistryHelper.getModelPath(entity.getType());
            ResourceLocation lodPath = ModelRegistryHelper.getLodModelPath(modelPath, selectLod(entity, cameraPos));
            batched.computeIfAbsent(lodPath, k -> new ArrayList<>()).add(entity);
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

    private static int selectLod(Entity entity, Vec3 cameraPos) {
        double distanceSqr = entity.position().distanceToSqr(cameraPos);
        if (distanceSqr <= LOD0_MAX_DISTANCE_SQR) {
            return 0;
        }
        if (distanceSqr <= LOD1_MAX_DISTANCE_SQR) {
            return 1;
        }
        return 2;
    }
}
