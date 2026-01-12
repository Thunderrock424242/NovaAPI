package com.thunder.novaapi.RenderEngine.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thunder.novaapi.RenderEngine.RenderEngineConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class OverlayBatcher {
    private static final List<OverlayCall> QUEUED_CALLS = new ArrayList<>();

    private OverlayBatcher() {
    }

    public static void queue(ResourceLocation texture, Consumer<GuiGraphics> drawCall) {
        Objects.requireNonNull(drawCall, "drawCall");
        synchronized (QUEUED_CALLS) {
            QUEUED_CALLS.add(new OverlayCall(texture, drawCall));
        }
    }

    public static void queue(Consumer<GuiGraphics> drawCall) {
        queue(null, drawCall);
    }

    public static void render(GuiGraphics graphics) {
        List<OverlayCall> calls;
        synchronized (QUEUED_CALLS) {
            if (QUEUED_CALLS.isEmpty()) {
                return;
            }
            calls = new ArrayList<>(QUEUED_CALLS);
            QUEUED_CALLS.clear();
        }

        if (!RenderEngineConfig.isOverlayBatchingEnabled()) {
            renderSequential(calls, graphics);
            return;
        }

        Map<ResourceLocation, List<OverlayCall>> grouped = new LinkedHashMap<>();
        for (OverlayCall call : calls) {
            grouped.computeIfAbsent(call.texture(), key -> new ArrayList<>()).add(call);
        }

        for (Map.Entry<ResourceLocation, List<OverlayCall>> entry : grouped.entrySet()) {
            ResourceLocation texture = entry.getKey();
            if (texture != null) {
                RenderSystem.setShaderTexture(0, texture);
            }
            for (OverlayCall call : entry.getValue()) {
                call.drawCall().accept(graphics);
            }
        }
    }

    private static void renderSequential(List<OverlayCall> calls, GuiGraphics graphics) {
        for (OverlayCall call : calls) {
            if (call.texture() != null) {
                RenderSystem.setShaderTexture(0, call.texture());
            }
            call.drawCall().accept(graphics);
        }
    }

    private record OverlayCall(ResourceLocation texture, Consumer<GuiGraphics> drawCall) {
    }
}
