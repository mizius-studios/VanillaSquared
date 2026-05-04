package blob.vanillasquared.main.sound;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class VSQSoundEvents {
    public static final Holder<SoundEvent> VOIDED_PASSIVE = register("voided_passive");
    public static final Holder<SoundEvent> VOIDED_MULTIPLIER_INCREASE = register("voided_multiplier_increase");
    public static final Holder<SoundEvent> DASH = register("dash");

    private VSQSoundEvents() {
    }

    private static Holder<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, name);
        return Registry.registerForHolder(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void initialize() {
    }
}
