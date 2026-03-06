package blob.vanillasquared.main.world.item.components.specialeffect;

import blob.vanillasquared.util.api.combat.cooldown.CooldownGroupUtil;
import blob.vanillasquared.util.api.modules.components.DataComponents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public final class SpecialEffectEvents {
    private SpecialEffectEvents() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            SpecialEffectComponent specialEffect = stack.get(DataComponents.SPECIAL_EFFECT);
            if (specialEffect == null) {
                return InteractionResult.PASS;
            }

            Identifier vanillaGroup = CooldownGroupUtil.stackGroup(stack);
            Identifier cooldownGroup = SpecialEffectCooldownKeyUtil.cooldownGroup(stack, specialEffect);
            boolean vanillaCooldownActive = SpecialEffectCooldownKeyUtil.isOnCooldown(player.getCooldowns(), vanillaGroup);
            boolean secondaryCooldownActive = SpecialEffectCooldownKeyUtil.isOnCooldown(player.getCooldowns(), cooldownGroup);

            if (vanillaCooldownActive && secondaryCooldownActive) {
                return InteractionResult.FAIL;
            }

            if (vanillaCooldownActive && !secondaryCooldownActive && specialEffect.cooldown() > 0) {
                player.getCooldowns().addCooldown(cooldownGroup, specialEffect.cooldown());
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}
