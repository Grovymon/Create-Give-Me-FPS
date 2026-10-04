package dev.creategmf.diagnostics;

import dev.creategmf.optimization.animations.VisualPopulation;
import dev.creategmf.optimization.animations.VisualWorkCounters;
import dev.creategmf.optimization.belts.TransportedItemCounters;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;

/** Counter values are MEASURED; subsystem cost is UNAVAILABLE, not fabricated from counts. */
public record RenderEvidence(VisualPopulation.Snapshot population, VisualWorkCounters.Snapshot animation,
        int occluded, int allFlywheelBlockEntityVisuals, long particleRequests, long particlesSuppressed,
        long transportedAttempts, long transportedSkipped) {
    public static RenderEvidence capture() {
        var particles = CreateParticleOptimizer.counters();
        var transported = TransportedItemCounters.snapshot();
        var level = Minecraft.getInstance().level;
        var manager = level == null ? null : VisualizationManager.get(level);
        var population = VisualPopulation.snapshot();
        return new RenderEvidence(population, VisualWorkCounters.snapshot(),
                Math.max(population.occluded(), CreateOcclusionController.snapshot().occluded()),
                manager == null ? -1 : manager.blockEntities().visualCount(),
                particles.requests(), particles.suppressed() + particles.directFluidEffectsSuppressed(),
                transported.attempted(), transported.skipped());
    }
    public RenderEvidence since(RenderEvidence start) {
        return new RenderEvidence(population, animation.subtract(start.animation), occluded, allFlywheelBlockEntityVisuals,
                particleRequests - start.particleRequests, particlesSuppressed - start.particlesSuppressed,
                transportedAttempts - start.transportedAttempts, transportedSkipped - start.transportedSkipped);
    }
}
