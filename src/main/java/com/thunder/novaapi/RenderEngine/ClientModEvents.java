package com.thunder.novaapi.RenderEngine;


import com.thunder.novaapi.Core.NovaAPI;
import com.thunder.novaapi.RenderEngine.Threading.ModdedRenderInterceptor;
import com.thunder.novaapi.RenderEngine.culling.EntityCullingManager;
import com.thunder.novaapi.RenderEngine.instancing.InstancedRenderer;
import com.thunder.novaapi.RenderEngine.overlay.OverlayBatcher;
import com.thunder.novaapi.RenderEngine.particles.ParticleCullingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

import static com.thunder.novaapi.Core.NovaAPI.MOD_ID;


@EventBusSubscriber(modid = MOD_ID)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        int renderDistanceChunks = minecraft.options.renderDistance().get();
        double maxDistance = Math.max(16.0D, renderDistanceChunks * 16.0D);
        double maxDistanceSq = maxDistance * maxDistance;

        EntityCullingManager.beginFrame();
        List<Entity> culledEntities = EntityCullingManager.cullEntities(
                minecraft.level.entitiesForRendering(),
                minecraft.level,
                event.getCamera(),
                maxDistanceSq
        );

        ParticleCullingManager.render(
                event.getFrustum(),
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
        );
        InstancedRenderer.renderAll(
                culledEntities,
                event.getFrustum()
        );
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        try {
            ModdedRenderInterceptor.executeModRender(() -> {
                OverlayBatcher.render(graphics);
            });
        } catch (Exception e) {
            NovaAPI.LOGGER.error("RenderInterceptor threw during overlay rendering", e);
        } finally {
            graphics.pose().popPose();
        }
    }
}
