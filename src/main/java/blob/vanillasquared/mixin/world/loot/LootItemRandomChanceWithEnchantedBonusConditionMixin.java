package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.loot.LootingFortuneBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootItemRandomChanceWithEnchantedBonusCondition.class)
public abstract class LootItemRandomChanceWithEnchantedBonusConditionMixin {
    @Shadow @Final private float unenchantedChance;
    @Shadow @Final private LevelBasedValue enchantedChance;
    @Shadow @Final private Holder<Enchantment> enchantment;

    @Inject(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At("HEAD"), cancellable = true)
    private void vsq$useLootingForFortune(LootContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!this.enchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)) {
            return;
        }
        Entity attacker = context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY);
        int level = attacker instanceof LivingEntity living
                ? EnchantmentHelper.getEnchantmentLevel(LootingFortuneBridge.remapFortuneToLooting(this.enchantment, context), living)
                : 0;
        float chance = level > 0 ? this.enchantedChance.calculate(level) : this.unenchantedChance;
        cir.setReturnValue(context.getRandom().nextFloat() < chance);
    }
}
