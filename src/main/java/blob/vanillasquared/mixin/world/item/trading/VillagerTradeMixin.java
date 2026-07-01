package blob.vanillasquared.mixin.world.item.trading;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @Shadow @Final private TradeCost wants;
    @Shadow @Final private Optional<TradeCost> additionalWants;
    @Shadow @Final private ItemStackTemplate gives;
    @Shadow @Final private Optional<LootItemCondition> merchantPredicate;
    @Shadow @Final private NumberProvider maxUses;
    @Shadow @Final private NumberProvider reputationDiscount;
    @Shadow @Final private NumberProvider xp;

    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void vsq$replaceEnchantedBookTradeBeforeVanillaFilters(LootContext lootContext, CallbackInfoReturnable<MerchantOffer> cir) {
        if (!this.gives.create().is(Items.ENCHANTED_BOOK)) {
            return;
        }

        if (this.merchantPredicate.isPresent() && !this.merchantPredicate.get().test(lootContext)) {
            cir.setReturnValue(null);
            return;
        }

        Identifier tagId = vsq$resolveLibrarianTag(lootContext);
        ItemStack replacement = EnchantingRecipeTags.createRandomStack(tagId, lootContext.getRandom());
        if (replacement.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        int additionalTradeCost = 5 + lootContext.getRandom().nextInt(15);
        ItemCost itemCost = this.wants.toItemCost(lootContext, additionalTradeCost);
        if (itemCost.count() < 1) {
            cir.setReturnValue(null);
            return;
        }

        Optional<ItemCost> additionalItemCost = this.additionalWants.map(tradeCost -> tradeCost.toItemCost(lootContext, 0));
        if (additionalItemCost.isPresent() && additionalItemCost.get().count() < 1) {
            cir.setReturnValue(null);
            return;
        }

        cir.setReturnValue(new MerchantOffer(
                itemCost,
                additionalItemCost,
                replacement,
                Math.max(this.maxUses.getInt(lootContext), 1),
                Math.max(this.xp.getInt(lootContext), 0),
                Math.max(this.reputationDiscount.getFloat(lootContext), 0.0F)
        ));
    }

    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceEnchantedBookOffer(LootContext lootContext, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        if (offer == null || !offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return;
        }

        Identifier tagId = vsq$resolveLibrarianTag(lootContext);
        ItemStack replacement = EnchantingRecipeTags.createRandomStack(tagId, lootContext.getRandom());
        if (replacement.isEmpty()) {
            return;
        }

        ((MerchantOfferAccessor) (Object) offer).vsq$setResult(replacement);
        cir.setReturnValue(offer);
    }

    @Unique
    private static Identifier vsq$resolveLibrarianTag(LootContext context) {
        Entity merchant = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (merchant instanceof Villager villager) {
            return villager.getVillagerData()
                    .type()
                    .unwrapKey()
                    .map(key -> EnchantingRecipeTags.librarianTagForVariant(key.identifier()))
                    .orElse(EnchantingRecipeTags.DEFAULT_LIBRARIAN_TAG);
        }
        return EnchantingRecipeTags.DEFAULT_LIBRARIAN_TAG;
    }
}
