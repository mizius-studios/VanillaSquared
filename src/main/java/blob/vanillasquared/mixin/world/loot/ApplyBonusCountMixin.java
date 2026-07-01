package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.loot.LootingFortuneBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {
    // `stack` is captured first so `context` stays in the correct local slot for the redirect.
    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I"
            )
    )
    private int vsq$useLootingForFortune(Holder<Enchantment> enchantment, ItemInstance piece, ItemStack itemStack, LootContext context) {
        return EnchantmentHelper.getItemEnchantmentLevel(LootingFortuneBridge.remapFortuneToLooting(enchantment, context), piece);
    }
}
