package com.thunder.novaapi.RenderEngine.instancing;

import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final ResourceLocation DEFAULT_MODEL_PATH =
            ResourceLocation.tryParse("minecraft:models/entity/default.json");

    /**
     * Scans registered EntityTypes and stores their model paths
     * for use with instanced rendering.
     */
    public static void initialize() {
        entityModelMap.clear();
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation modelPath = resolveModelPath(entityType);
            entityModelMap.put(entityType, modelPath);
        }
    }


    /**
     * Returns a model path like "modid:models/entity/zombie.json"
     */
    private static ResourceLocation resolveModelPath(EntityType<?> entityType) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (id == null) {
            return DEFAULT_MODEL_PATH;
        }
        ResourceLocation resolved = ResourceLocation.tryParse(id.getNamespace() + ":models/entity/" + id.getPath() + ".json");
        return resolved != null ? resolved : DEFAULT_MODEL_PATH;
    }

    /**
     * Retrieves the model path for a given EntityType.
     * Defaults to a placeholder if no path is registered.
     */
    public static ResourceLocation getModelPath(EntityType<?> type) {
        return entityModelMap.getOrDefault(type, DEFAULT_MODEL_PATH);
    }

    /**
     * Registers a model override for a specific entity type.
     */
    public static void registerModelPath(EntityType<?> type, ResourceLocation modelPath) {
        if (type == null || modelPath == null) {
            return;
        }
        entityModelMap.put(type, modelPath);
    }

    public static boolean isDefaultModel(ResourceLocation modelPath) {
        return DEFAULT_MODEL_PATH != null && DEFAULT_MODEL_PATH.equals(modelPath);
    }

    /**
     * Returns a LOD-specific model path for the given EntityType.
     * LOD 0 is the base model path, while higher LODs append "#lodX".
     */
    public static ResourceLocation getLodModelPath(EntityType<?> type, int lod) {
        return getLodModelPath(getModelPath(type), lod);
    }

    /**
     * Returns a LOD-specific model path for the given base model path.
     * LOD 0 is the base model path, while higher LODs append "#lodX".
     */
    public static ResourceLocation getLodModelPath(ResourceLocation basePath, int lod) {
        if (lod <= 0 || basePath == null) {
            return basePath;
        }

        ResourceLocation lodPath = ResourceLocation.tryParse(basePath + "#lod" + lod);
        return lodPath != null ? lodPath : basePath;
    }
}
