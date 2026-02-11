package blob.vanillasquared.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedAttribute.class)
public class RangedAttributeMixin {
    @Inject(method = "<init>", at = @At("HEAD"))
    private static void init(String string, double d, double e, double f, CallbackInfo ci) {
        switch (string) {
            case "attribute.name.armor": {
                f = 1024.0;
            }
            case "attribute.name.armor_toughness": {
                f = 1024.0;
            }
        }
    }
}
