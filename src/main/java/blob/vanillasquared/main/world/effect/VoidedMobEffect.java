package blob.vanillasquared.main.world.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

public final class VoidedMobEffect extends MobEffect {
    private static final int VOIDED_COLOR = 0xFF2B2B;

    public VoidedMobEffect() {
        super(MobEffectCategory.HARMFUL, VOIDED_COLOR);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        // Suppress vanilla effect placeholder particles; Voided uses custom particles.
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(0, VOIDED_COLOR));
    }
}
