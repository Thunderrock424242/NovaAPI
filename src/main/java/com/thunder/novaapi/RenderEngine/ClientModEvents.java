package com.thunder.novaapi.RenderEngine;


import com.thunder.novaapi.Core.NovaAPI;
import com.thunder.novaapi.RenderEngine.Threading.ModdedRenderInterceptor;
import com.thunder.novaapi.RenderEngine.instancing.InstancedRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static com.thunder.novaapi.Core.NovaAPI.MOD_ID;


@EventBusSubscriber(modid = MOD_ID)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        InstancedRenderer.renderAll(
                Minecraft.getInstance().level.entitiesForRendering(),
                event.getFrustum()
        );
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        try {
            ModdedRenderInterceptor.executeModRender(() -> {
                // Run actual GUI drawing here using graphics
            });
        } catch (Exception e) {
            NovaAPI.LOGGER.error("RenderInterceptor threw during overlay rendering", e);
        } finally {
            graphics.pose().popPose();
        }
    }
}