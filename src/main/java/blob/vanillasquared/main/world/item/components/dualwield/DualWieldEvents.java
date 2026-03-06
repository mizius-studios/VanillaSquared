package blob.vanillasquared.main.world.item.components.dualwield;

import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class DualWieldEvents {

    private DualWieldEvents() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.OFF_HAND) {
                return InteractionResult.PASS;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            Optional<DualWieldUtil.ActiveDualWield> active = DualWieldUtil.getActiveDualWield(mainHand, offHand);
            if (active.isEmpty()) {
                return InteractionResult.PASS;
            }

            Optional<String> matchingIdentifier = DualWieldUtil.findFirstMatchingIdentifier(
                    active.get().mainHand().identifiers(),
                    active.get().offHand().identifiers()
            );
            if (matchingIdentifier.isEmpty()) {
                return InteractionResult.PASS;
            }

            Identifier cooldownGroup = DualWieldCooldownKeyUtil.offhandGroupFromDualIdentifier(matchingIdentifier.get());
            if (DualWieldCooldownKeyUtil.isOnCooldown(player.getCooldowns(), cooldownGroup)) {
                return InteractionResult.FAIL;
            }

            DualWieldUtil.ActiveDualWield config = active.get();
            if (config.offHand().cooldown() > 0) {
                player.getCooldowns().addCooldown(cooldownGroup, config.offHand().cooldown());
            }

            if (!level.isClientSide()) {
                ((DualWieldPlayerData) player).vsq$setDualWieldCritCharges(config.offHand().criticalHits());
            }

            return InteractionResult.SUCCESS;
        });
    }
}
