package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.VSQExperiments;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagRegistry;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FeatureFlags.class)
public abstract class FeatureFlagsMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/flag/FeatureFlagRegistry$Builder;build()Lnet/minecraft/world/flag/FeatureFlagRegistry;")
    )
    private static FeatureFlagRegistry vsq$addExperimentFlagsBeforeBuild(FeatureFlagRegistry.Builder builder) {
        for (var featureId : VSQExperiments.builtinFeatureIds()) {
            FeatureFlag featureFlag = builder.create(featureId);
            VSQExperiments.vsq$setFeatureFlag(featureId, featureFlag);
        }

        return builder.build();
    }
}
