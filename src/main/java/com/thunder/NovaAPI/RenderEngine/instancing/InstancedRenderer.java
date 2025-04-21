package com.thunder.NovaAPI.RenderEngine.instancing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class InstancedRenderer {

    public InstancedRenderer(ResourceLocation modelPath, ResourceLocation modelPath1) {
        ModelData model = ModelData.builder().build(); // NeoForge ModelData

        // Correct way to access Mixin-injected methods
        IModeledDataExtensions extensions = (IModeledDataExtensions) (Object) model;
        extensions.setVAO(loadVAO(modelPath));
        extensions.setIndexCount(loadIndexCount(modelPath));
    }

    private int loadVAO(ResourceLocation modelPath) {
        // TODO: Implement actual VAO loading
        return 0; // Placeholder
    }

    private int loadIndexCount(ResourceLocation modelPath) {
        // TODO: Implement actual index count loading
        return 0; // Placeholder
    }

    private boolean isInFrustum(Entity entity) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        return entity.getBoundingBox().intersects(
                cameraPos.x - 50, cameraPos.y - 50, cameraPos.z - 50,
                cameraPos.x + 50, cameraPos.y + 50, cameraPos.z + 50
        );
    }

    public void render(Entity entity) {
        if (!isInFrustum(entity)) return;

        ResourceLocation modelPath = ModelRegistryHelper.getModelPath(entity.getType());
        if (!VAOManager.isModelEnabled(modelPath)) return; // Let vanilla handle it if failed

        int vao = VAOManager.getOrLoadVAO(modelPath);
        if (vao <= 0) return; // Skip rendering if VAO is invalid

        GL30.glBindVertexArray(vao);
        GL31.glDrawElementsInstanced(GL30.GL_TRIANGLES, 36, GL30.GL_UNSIGNED_INT, 0, 1);
    }
}
