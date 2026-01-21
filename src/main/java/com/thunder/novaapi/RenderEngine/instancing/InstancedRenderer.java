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
    /**
     * Renders all visible entities using instanced rendering, grouped by model.
     */
    public static void renderAll(Iterable<Entity> entities, Frustum frustum) {
        if (!com.thunder.novaapi.RenderEngine.RenderEngineConfig.isInstancedRenderingEnabled()) {
            return;
        }

        Map<ResourceLocation, List<Entity>> batched = new HashMap<>();
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        LodThresholds thresholds = resolveLodThresholds(
                com.thunder.novaapi.RenderEngine.RenderEngineConfig.getInstancedLod0Distance(),
                com.thunder.novaapi.RenderEngine.RenderEngineConfig.getInstancedLod1Distance()
        );

        for (Entity entity : entities) {
            if (frustum != null && !frustum.isVisible(entity.getBoundingBox())) {
                continue;
            }

            ResourceLocation modelPath = ModelRegistryHelper.getModelPath(entity.getType());
            if (modelPath == null || !VAOManager.isModelEnabled(modelPath)) {
                continue;
            }
            ResourceLocation lodPath = ModelRegistryHelper.getLodModelPath(
                    modelPath,
                    selectLod(entity, cameraPos, thresholds)
            );
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

    private static int selectLod(Entity entity, Vec3 cameraPos, LodThresholds thresholds) {
        double distanceSqr = entity.position().distanceToSqr(cameraPos);
        if (distanceSqr <= thresholds.lod0MaxDistanceSq()) {
            return 0;
        }
        if (distanceSqr <= thresholds.lod1MaxDistanceSq()) {
            return 1;
        }
        return 2;
    }

    private static LodThresholds resolveLodThresholds(double lod0Distance, double lod1Distance) {
        double clampedLod0 = Math.max(0.0D, lod0Distance);
        double clampedLod1 = Math.max(clampedLod0, lod1Distance);
        return new LodThresholds(clampedLod0 * clampedLod0, clampedLod1 * clampedLod1);
    }

    private record LodThresholds(double lod0MaxDistanceSq, double lod1MaxDistanceSq) {
    }
}
