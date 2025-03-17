package com.thunder.NovaAPI.RenderEngine.instancing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.opengl.GL45;

public class InstancedRenderer {
    private final ResourceLocation modelPath;
    private ModelData model;

    public InstancedRenderer(ResourceLocation modelPath, ResourceLocation modelPath1) {
        this.modelPath = modelPath1;
        this.model = ModelData.builder().build(); // NeoForge ModelData

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
        if (!isInFrustum(entity)) return; // Skip rendering if off-screen
        if (model == null) return; // Ensure model is loaded

        renderModel(model);
    }

    private void renderModel(ModelData model) {
        try {
            IModeledDataExtensions extensions = (IModeledDataExtensions) (Object) model;

            GL45.glBindVertexArray(extensions.getVAO());
            GL45.glDrawElementsInstanced(GL45.GL_TRIANGLES, extensions.getIndexCount(), GL45.GL_UNSIGNED_INT, 0, 1);
        } catch (ClassCastException e) {
            System.err.println("[InstancedRenderer] Failed to cast ModelData for instanced rendering!");
        }
    }
    // TODO: Implement instanced rendering for the 3D model
}
