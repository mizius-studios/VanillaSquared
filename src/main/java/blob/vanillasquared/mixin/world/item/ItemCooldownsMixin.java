package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.util.api.combat.cooldown.CooldownGroupUtil;
import blob.vanillasquared.main.world.item.components.dualwield.DualWieldCooldownKeyUtil;
import blob.vanillasquared.main.world.item.components.dualwield.DualWieldComponent;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectComponent;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectCooldownKeyUtil;
import blob.vanillasquared.util.api.modules.components.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldowns.class)
public class ItemCooldownsMixin {
    @Unique
    private boolean vsq$normalizingCooldownKey;

    @Inject(method = "getCooldownGroup", at = @At("HEAD"), cancellable = true)
    private void vsq$getCooldownGroup(ItemStack stack, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(CooldownGroupUtil.stackGroup(stack));
    }

    @Inject(method = "getCooldownPercent(Lnet/minecraft/world/item/ItemStack;F)F", at = @At("RETURN"), cancellable = true)
    private void vsq$getDualWieldCooldownPercent(ItemStack stack, float partialTick, CallbackInfoReturnable<Float> cir) {
        DualWieldComponent dualWield = stack.get(DataComponents.DUAL_WIELD);
        ItemCooldowns cooldowns = (ItemCooldowns) (Object) this;
        float cooldownPercent = cir.getReturnValue();

        if (dualWield != null && !dualWield.identifiers().isEmpty()) {
            for (String identifierToken : dualWield.identifiers()) {
                Identifier cooldownGroup = DualWieldCooldownKeyUtil.offhandGroupFromDualIdentifier(identifierToken);
                cooldownPercent = Math.max(
                        cooldownPercent,
                        DualWieldCooldownKeyUtil.getCooldownPercent(cooldowns, cooldownGroup, partialTick)
                );
                if (cooldownPercent >= 1.0F) {
                    break;
                }
            }
        }

        SpecialEffectComponent specialEffect = stack.get(DataComponents.SPECIAL_EFFECT);
        if (specialEffect != null) {
            Identifier secondaryGroup = SpecialEffectCooldownKeyUtil.cooldownGroup(stack, specialEffect);
            cooldownPercent = Math.max(
                    cooldownPercent,
                    SpecialEffectCooldownKeyUtil.getCooldownPercent(cooldowns, secondaryGroup, partialTick)
            );
        }

        cir.setReturnValue(cooldownPercent);
    }

    @Inject(method = "isOnCooldown", at = @At("RETURN"), cancellable = true)
    private void vsq$requireSecondaryCooldownForSpecialEffectStacks(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        SpecialEffectComponent specialEffect = stack.get(DataComponents.SPECIAL_EFFECT);
        if (specialEffect == null) {
            return;
        }

        ItemCooldowns cooldowns = (ItemCooldowns) (Object) this;
        Identifier secondaryGroup = SpecialEffectCooldownKeyUtil.cooldownGroup(stack, specialEffect);
        cir.setReturnValue(SpecialEffectCooldownKeyUtil.isOnCooldown(cooldowns, secondaryGroup));
    }

    @Inject(method = "addCooldown(Lnet/minecraft/resources/Identifier;I)V", cancellable = true, at = @At("HEAD"))
    public void addCooldown(Identifier identifier, int cooldown, CallbackInfo ci) {
        if (this.vsq$normalizingCooldownKey) {
            return;
        }
        this.vsq$normalizingCooldownKey = true;
        try {
            Identifier normalized = CooldownGroupUtil.normalize(identifier);
            ((ItemCooldowns) (Object) this).addCooldown(normalized, cooldown);
            ci.cancel();
        } finally {
            this.vsq$normalizingCooldownKey = false;
        }
    }
}
