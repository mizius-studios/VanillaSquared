package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.loot.LootingFortuneBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BonusLevelTableCondition.class)
public abstract class BonusLevelTableConditionMixin {
    @Shadow @Final private Holder<Enchantment> enchantment;
    @Shadow @Final private List<Float> values;

    @Inject(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At("HEAD"), cancellable = true)
    private void vsq$useLootingForFortune(LootContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!this.enchantment.is(Enchantments.FORTUNE)) {
            return;
        }
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        int level = tool != null
                ? EnchantmentHelper.getItemEnchantmentLevel(LootingFortuneBridge.remapFortuneToLooting(this.enchantment, context), tool)
                : 0;
        if (this.values.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        float chance = this.values.get(Math.min(level, this.values.size() - 1));
        cir.setReturnValue(context.getRandom().nextFloat() < chance);
    }
}
