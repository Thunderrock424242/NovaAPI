package com.thunder.novaapi.RenderEngine.culling;

import com.thunder.novaapi.config.NovaAPIConfig;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EntityCullingManager {
    private static final Int2BooleanOpenHashMap VISIBILITY_CACHE = new Int2BooleanOpenHashMap();

    private EntityCullingManager() {
    }

    public static void beginFrame() {
        VISIBILITY_CACHE.clear();
    }

    public static List<Entity> cullEntities(Iterable<Entity> entities, Level level, Camera camera, double maxDistanceSq) {
        Vec3 cameraPos = camera.getPosition();
        boolean occlusionEnabled = NovaAPIConfig.ENABLE_OCCLUSION_CULLING.get();
        List<Entity> visibleEntities = new ArrayList<>();

        for (Entity entity : entities) {
            if (isEntityVisible(entity, level, cameraPos, maxDistanceSq, occlusionEnabled)) {
                visibleEntities.add(entity);
            }
        }

        return visibleEntities;
    }

    private static boolean isEntityVisible(Entity entity, Level level, Vec3 cameraPos, double maxDistanceSq, boolean occlusionEnabled) {
        int entityId = entity.getId();
        if (VISIBILITY_CACHE.containsKey(entityId)) {
            return VISIBILITY_CACHE.get(entityId);
        }

        boolean visible = entity.distanceToSqr(cameraPos) <= maxDistanceSq;

        if (visible && occlusionEnabled) {
            Vec3 target = entity.getBoundingBox().getCenter();
            HitResult hit = level.clip(new ClipContext(cameraPos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            visible = hit.getType() == HitResult.Type.MISS;
        }

        VISIBILITY_CACHE.put(entityId, visible);
        return visible;
    }
}
