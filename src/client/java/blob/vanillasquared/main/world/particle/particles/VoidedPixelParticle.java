package blob.vanillasquared.main.world.particle.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class VoidedPixelParticle extends SingleQuadParticle {
    private static final float BASE_ALPHA = 0.36F;

    private VoidedPixelParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd * 0.12, yd * 0.04 + 0.001, zd * 0.12, sprites.first());
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.992F;
        this.lifetime = Mth.randomBetweenInclusive(this.random, 50, 80);
        this.quadSize = Mth.randomBetween(this.random, 0.018F, 0.03F);
        this.setAlpha(BASE_ALPHA);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            float ageProgress = this.age / (float) this.lifetime;
            this.setAlpha(BASE_ALPHA * Mth.clamp((1.0F - ageProgress) * 2.0F, 0.0F, 1.0F));
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                RandomSource random
        ) {
            return new VoidedPixelParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
