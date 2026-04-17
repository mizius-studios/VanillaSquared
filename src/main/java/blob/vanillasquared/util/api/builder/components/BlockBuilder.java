package blob.vanillasquared.util.api.builder.components;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.component.BlocksAttacks;

import java.util.List;
import java.util.Optional;

public class BlockBuilder {
    private final BlocksAttacks blockComponent;

    public BlockBuilder(float blockDelay, float shieldBreakCooldown, float dmgReduction, float duraDMG) {
        this.blockComponent = new BlocksAttacks(
                blockDelay,
                shieldBreakCooldown,
                List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, dmgReduction)),
                new BlocksAttacks.ItemDamageFunction(1.0F, 1.0F, duraDMG),
                bypassesShieldTag(),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
        );
    }

    private static Optional<net.minecraft.core.HolderSet<DamageType>> bypassesShieldTag() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                .lookup(Registries.DAMAGE_TYPE)
                .flatMap(registry -> registry.get(DamageTypeTags.BYPASSES_SHIELD))
                .map(holderSet -> (net.minecraft.core.HolderSet<DamageType>) holderSet);
    }

    public BlocksAttacks build() { return blockComponent; }
}
