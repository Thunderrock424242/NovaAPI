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
    private static final List<OverlayCall> RENDER_CALLS = new ArrayList<>();
    private static final Map<ResourceLocation, List<OverlayCall>> GROUPED_CALLS = new LinkedHashMap<>();
    private static final List<List<OverlayCall>> CALL_LIST_POOL = new ArrayList<>();

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
        synchronized (QUEUED_CALLS) {
            if (QUEUED_CALLS.isEmpty()) {
                return;
            }
            RENDER_CALLS.clear();
            RENDER_CALLS.addAll(QUEUED_CALLS);
            QUEUED_CALLS.clear();
        }

        if (!RenderEngineConfig.isOverlayBatchingEnabled()) {
            renderSequential(RENDER_CALLS, graphics);
            return;
        }

        GROUPED_CALLS.clear();
        for (OverlayCall call : RENDER_CALLS) {
            List<OverlayCall> bucket = GROUPED_CALLS.get(call.texture());
            if (bucket == null) {
                bucket = acquireCallList();
                GROUPED_CALLS.put(call.texture(), bucket);
            }
            bucket.add(call);
        }

        for (Map.Entry<ResourceLocation, List<OverlayCall>> entry : GROUPED_CALLS.entrySet()) {
            ResourceLocation texture = entry.getKey();
            if (texture != null) {
                RenderSystem.setShaderTexture(0, texture);
            }
            for (OverlayCall call : entry.getValue()) {
                call.drawCall().accept(graphics);
            }
        }

        for (List<OverlayCall> list : GROUPED_CALLS.values()) {
            list.clear();
            CALL_LIST_POOL.add(list);
        }
        GROUPED_CALLS.clear();
    }

    private static void renderSequential(List<OverlayCall> calls, GuiGraphics graphics) {
        for (OverlayCall call : calls) {
            if (call.texture() != null) {
                RenderSystem.setShaderTexture(0, call.texture());
            }
            call.drawCall().accept(graphics);
        }
    }

    private static List<OverlayCall> acquireCallList() {
        int lastIndex = CALL_LIST_POOL.size() - 1;
        if (lastIndex >= 0) {
            return CALL_LIST_POOL.remove(lastIndex);
        }
        return new ArrayList<>();
    }

    private record OverlayCall(ResourceLocation texture, Consumer<GuiGraphics> drawCall) {
    }
}
