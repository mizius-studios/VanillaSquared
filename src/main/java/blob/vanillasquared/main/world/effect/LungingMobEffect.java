package blob.vanillasquared.main.world.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

public final class LungingMobEffect extends MobEffect {
    private static final int LUNGING_COLOR = 0xFFFFFF;

    public LungingMobEffect() {
        super(MobEffectCategory.NEUTRAL, LUNGING_COLOR);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(0, LUNGING_COLOR));
    }
}
