package blob.vanillasquared.mixin;

import blob.vanillasquared.VanillaSquared;
import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float vsq$applyAttributeProtections(float amount, ServerLevel level, DamageSource source) {
        LivingEntity entity = (LivingEntity)(Object)this;

        float damage = amount;

        // Mace Protection
        Entity attacker = source.getDirectEntity();
        
        if (attacker instanceof LivingEntity livingAttacker) {
            ItemStack weapon = livingAttacker.getMainHandItem();
            
            if (weapon.is(Items.MACE)) {
                VanillaSquared.LOGGER.info("=== Mace attack on {} ===", entity.getName().getString());
                VanillaSquared.LOGGER.info("Original damage: {}", amount);
                
                // Manually calculate protection from equipment
                double totalProtection = 0.0;
                
                // Check all equipment slots
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack equipment = entity.getItemBySlot(slot);
                    if (!equipment.isEmpty()) {
                        ItemAttributeModifiers modifiers = equipment.get(DataComponents.ATTRIBUTE_MODIFIERS);
                        if (modifiers != null) {
                            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                                if (entry.attribute().equals(RegisterAttributes.maceProtection)) {
                                    double modifierAmount = entry.modifier().amount();
                                    totalProtection += modifierAmount;
                                    VanillaSquared.LOGGER.info("Found mace protection on {}: {} = {}", 
                                        slot, equipment.getItem(), modifierAmount);
                                }
                            }
                        }
                    }
                }
                
                // Clamp protection to 0.0 - 1.0 range
                totalProtection = Math.max(0.0, Math.min(1.0, totalProtection));
                
                VanillaSquared.LOGGER.info("Total mace protection: {}", totalProtection);
                
                // Apply protection
                damage *= (1.0F - (float) totalProtection);
                
                VanillaSquared.LOGGER.info("Damage after protection: {}", damage);
                VanillaSquared.LOGGER.info("=== End mace attack ===");
            }
        }

        return Math.max(damage, 0);
    }
}