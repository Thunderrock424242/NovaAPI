package com.thunder.novaapi.RenderEngine.particles;

import com.thunder.novaapi.RenderEngine.RenderEngineConfig;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ParticleCullingManager {
    private static final List<ParticleRenderRequest> QUEUED_REQUESTS = new ArrayList<>();

    private ParticleCullingManager() {
    }

    public static void queue(ParticleRenderRequest request) {
        Objects.requireNonNull(request, "request");
        synchronized (QUEUED_REQUESTS) {
            QUEUED_REQUESTS.add(request);
        }
    }

    public static void render(Frustum frustum, Vec3 cameraPosition) {
        List<ParticleRenderRequest> requests;
        synchronized (QUEUED_REQUESTS) {
            if (QUEUED_REQUESTS.isEmpty()) {
                return;
            }
            requests = new ArrayList<>(QUEUED_REQUESTS);
            QUEUED_REQUESTS.clear();
        }

        if (!RenderEngineConfig.isParticleCullingEnabled()) {
            for (ParticleRenderRequest request : requests) {
                request.submit().run();
            }
            return;
        }

        int maxDistance = RenderEngineConfig.getParticleCullingDistance();
        double maxDistanceSquared = maxDistance > 0 ? maxDistance * (double) maxDistance : Double.POSITIVE_INFINITY;

        for (ParticleRenderRequest request : requests) {
            if (cameraPosition != null && cameraPosition.distanceToSqr(request.position()) > maxDistanceSquared) {
                continue;
            }
            if (frustum != null && !frustum.isVisible(request.bounds())) {
                continue;
            }
            request.submit().run();
        }
    }

    public record ParticleRenderRequest(Vec3 position, float radius, Runnable submit) {
        public ParticleRenderRequest {
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(submit, "submit");
        }

        public AABB bounds() {
            double r = Math.max(0.0D, radius);
            return new AABB(position.x - r, position.y - r, position.z - r,
                    position.x + r, position.y + r, position.z + r);
        }
    }
}
