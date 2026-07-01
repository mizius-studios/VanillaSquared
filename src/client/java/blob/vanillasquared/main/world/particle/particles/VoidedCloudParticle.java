package blob.vanillasquared.main.world.particle.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class VoidedCloudParticle extends SingleQuadParticle {
    private VoidedCloudParticle(ClientLevel level, double x, double y, double z, double yd, SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, sprites.get(random));
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.lifetime = Mth.randomBetweenInclusive(this.random, 44, 50);
        this.quadSize *= Mth.randomBetween(this.random, 2.58F, 2.76F);
        this.setParticleSpeed(0.0, Mth.clamp(Math.abs(yd) + 0.041, 0.046, 0.055), 0.0);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float ageProgress = (this.age + partialTick) / this.lifetime;
        float fadeIn = Mth.clamp(ageProgress * 8.0F, 0.0F, 1.0F);
        float sizeFalloff = 1.0F - (ageProgress * 0.22F);
        return this.quadSize * fadeIn * Mth.clamp(sizeFalloff, 0.0F, 1.0F);
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
            return new VoidedCloudParticle(level, x, y, z, yd, this.sprites, random);
        }
    }
}
