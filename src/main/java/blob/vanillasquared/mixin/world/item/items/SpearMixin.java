package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.builder.durability.Durability;
import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder;
import blob.vanillasquared.util.api.combat.VSQCombatPresets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.Weapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Item.Properties.class)
public abstract class SpearMixin {
    @Inject(
            method = "spear(Lnet/minecraft/world/item/ToolMaterial;FFFFFFFFF)Lnet/minecraft/world/item/Item$Properties;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vsq$applyPreset(
            ToolMaterial material,
            float attackDuration,
            float damageMultiplier,
            float delay,
            float dismountTime,
            float dismountThreshold,
            float knockbackTime,
            float knockbackThreshold,
            float damageTime,
            float damageThreshold,
            CallbackInfoReturnable<Item.Properties> cir
    ) {
        WeaponAttributeBuilder generalWeapon = VSQCombatPresets.spearAttributes(material);
        if (generalWeapon == null) {
            return;
        }

        Durability durability = VSQCombatPresets.toolDurability(material);

        Item.Properties properties = (Item.Properties) (Object) this;
        cir.setReturnValue(
                properties.durability(durability.durability())
                        .repairable(material.repairItems())
                        .enchantable(material.enchantmentValue())
                        .delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR)
                        .component(
                                DataComponents.KINETIC_WEAPON,
                                new KineticWeapon(
                                        10,
                                        (int) (delay * 20.0F),
                                        KineticWeapon.Condition.ofAttackerSpeed((int) (dismountTime * 20.0F), dismountThreshold),
                                        KineticWeapon.Condition.ofAttackerSpeed((int) (knockbackTime * 20.0F), knockbackThreshold),
                                        KineticWeapon.Condition.ofRelativeSpeed((int) (damageTime * 20.0F), damageThreshold),
                                        0.38F,
                                        damageMultiplier,
                                        Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                                        Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
                                )
                        )
                        .component(
                                DataComponents.PIERCING_WEAPON,
                                new PiercingWeapon(
                                        true,
                                        false,
                                        Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_ATTACK : SoundEvents.SPEAR_ATTACK),
                                        Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
                                )
                        )
                        .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
                        .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F)
                        .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int) (attackDuration * 20.0F)))
                        .attributes(generalWeapon.build())
                        .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F))
                        .component(DataComponents.WEAPON, new Weapon(1))
        );
    }
}
