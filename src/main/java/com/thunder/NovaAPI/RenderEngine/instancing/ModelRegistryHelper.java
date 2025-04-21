package com.thunder.NovaAPI.RenderEngine.instancing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps modded and vanilla EntityTypes to model paths
 * used for instanced rendering. These paths can later
 * be used to load VAOs or custom metadata.
 */
public class ModelRegistryHelper {
    private static final Map<EntityType<?>, ResourceLocation> entityModelMap = new ConcurrentHashMap<>();

    /**
     * Scans registered EntityTypes and stores their model paths
     * for use with instanced rendering.
     */
    public static void initialize() {
        for (EntityType<?> entityType : EntityType.values()) {
            if (EntityRenderers.getRenderer(entityType) != null) {
                ResourceLocation modelPath = resolveModelPath(entityType);
                entityModelMap.put(entityType, modelPath);
            }
        }
    }

    /**
     * Returns a model path like "modid:models/entity/zombie.json"
     */
    private static ResourceLocation resolveModelPath(EntityType<?> entityType) {
        ResourceLocation id = EntityType.getKey(entityType); // More stable than getRegistryName()
        return new ResourceLocation(id.getNamespace(), "models/entity/" + id.getPath() + ".json");
    }

    /**
     * Retrieves the model path for a given EntityType.
     * Defaults to a placeholder if no path is registered.
     */
    public static ResourceLocation getModelPath(EntityType<?> type) {
        return entityModelMap.getOrDefault(type,
                new ResourceLocation("minecraft", "models/entity/default.json"));
    }
}