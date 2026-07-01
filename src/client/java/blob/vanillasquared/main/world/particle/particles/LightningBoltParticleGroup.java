package blob.vanillasquared.main.world.particle.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

import java.util.ArrayList;
import java.util.List;

public class LightningBoltParticleGroup extends ParticleGroup<LightningBoltParticle> {
    public LightningBoltParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float tickDelta) {
        List<ParticleGroupRenderState> states = new ArrayList<>();
        for (LightningBoltParticle particle : this.particles) {
            if (frustum.isVisible(particle.getBoundingBox())) {
                states.add(particle.createRenderState(camera, tickDelta));
            }
        }
        return (submitNodeCollector, cameraRenderState) -> {
            for (ParticleGroupRenderState state : states) {
                state.submit(submitNodeCollector, cameraRenderState);
            }
        };
    }
}
