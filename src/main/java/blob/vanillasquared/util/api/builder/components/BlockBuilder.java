package blob.vanillasquared.util.api.builder.components;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
                Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
        );
    }
    public BlocksAttacks build() { return blockComponent; }
}
