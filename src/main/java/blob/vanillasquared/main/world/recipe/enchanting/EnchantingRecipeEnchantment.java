package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public record EnchantingRecipeEnchantment(Identifier enchantment) {
    public static final Codec<EnchantingRecipeEnchantment> CODEC = Identifier.CODEC.xmap(
            EnchantingRecipeEnchantment::new,
            EnchantingRecipeEnchantment::enchantment
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipeEnchantment> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnchantingRecipeEnchantment decode(RegistryFriendlyByteBuf buf) {
            return new EnchantingRecipeEnchantment(Identifier.STREAM_CODEC.decode(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantingRecipeEnchantment value) {
            Identifier.STREAM_CODEC.encode(buf, value.enchantment());
        }
    };

    public ItemStack apply(ItemStack originalStack, HolderLookup.Provider registries) {
        Holder.Reference<Enchantment> enchantment = this.vsq$enchantmentHolder(registries);
        int nextLevel = this.nextLevel(originalStack, registries);
        if (!this.canApplyNextLevel(originalStack, registries) || !VSQEnchantments.canApply(originalStack, enchantment, nextLevel)) {
            return originalStack.copy();
        }
        return VSQEnchantments.apply(originalStack, enchantment, nextLevel);
    }

    public Ingredient supportedItemsIngredient(HolderLookup.Provider registries) {
        return Ingredient.of(this.vsq$enchantmentHolder(registries).value().definition().supportedItems());
    }

    public ItemStack previewInputStack(HolderLookup.Provider registries) {
        Iterator<Holder<Item>> iterator = this.vsq$previewItems(registries).iterator();
        return iterator.hasNext() ? new ItemStack(iterator.next().value()) : ItemStack.EMPTY;
    }

    public SlotDisplay previewInputDisplay(HolderLookup.Provider registries, int displayCount) {
        List<SlotDisplay> displays = new ArrayList<>();
        for (Holder<Item> holder : this.vsq$previewItems(registries)) {
            ItemStack stack = new ItemStack(holder.value());
            stack.setCount(displayCount);
            displays.add(new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack)));
        }

        if (displays.isEmpty()) {
            return Empty.INSTANCE;
        }
        if (displays.size() == 1) {
            return displays.getFirst();
        }
        return new SlotDisplay.Composite(List.copyOf(displays));
    }

    public boolean modifies(ItemStack originalStack, HolderLookup.Provider registries) {
        return !ItemStack.matches(originalStack, this.apply(originalStack, registries));
    }

    public int currentLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        Holder.Reference<Enchantment> enchantment = this.vsq$enchantmentHolder(registries);
        return Math.max(VSQEnchantments.currentLevel(originalStack, enchantment), 0);
    }

    public int nextLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        int currentLevel = this.currentLevel(originalStack, registries);
        int maxLevel = this.maxLevel(originalStack, registries);
        if (currentLevel >= maxLevel) {
            return Math.max(currentLevel, 1);
        }
        return Math.max(currentLevel + 1, 1);
    }

    public int maxLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        return Math.max(VSQEnchantments.maxLevel(originalStack, this.vsq$enchantmentHolder(registries)), 1);
    }

    public Component displayName(ItemStack originalStack, HolderLookup.Provider registries) {
        MutableComponent name = this.vsq$enchantmentHolder(registries).value().description().copy();
        if (this.maxLevel(originalStack, registries) == 1) {
            return name;
        }
        int level = this.nextLevel(originalStack, registries);
        return Component.translatable("vsq.enchantment.recipe.name", name, Component.translatable("enchantment.level." + level));
    }

    public Component recipeBookDisplayName(HolderLookup.Provider registries) {
        Enchantment enchantment = this.vsq$enchantmentHolder(registries).value();
        Component name = enchantment.description().copy();
        return enchantment.getMaxLevel() == 1
                ? name
                : Component.translatable("vsq.enchantment.recipe.name", name, Component.translatable("enchantment.level.1"));
    }

    public boolean isBelowMaximumLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        return this.currentLevel(originalStack, registries) < this.maxLevel(originalStack, registries);
    }

    public boolean canApplyNextLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        return this.isBelowMaximumLevel(originalStack, registries);
    }

    public boolean respectsVanillaEnchantmentIncompatibilities(ItemStack originalStack, HolderLookup.Provider registries) {
        var entries = VSQEnchantments.aggregate(this.apply(originalStack, registries)).entrySet().stream().toList();
        for (int leftIndex = 0; leftIndex < entries.size(); leftIndex++) {
            var left = entries.get(leftIndex).getKey();
            for (int rightIndex = leftIndex + 1; rightIndex < entries.size(); rightIndex++) {
                var right = entries.get(rightIndex).getKey();
                if (!VSQEnchantments.areCompatible(originalStack, left, right)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Holder.Reference<Enchantment> vsq$enchantmentHolder(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, this.enchantment));
    }

    private Iterable<Holder<Item>> vsq$previewItems(HolderLookup.Provider registries) {
        Enchantment.EnchantmentDefinition definition = this.vsq$enchantmentHolder(registries).value().definition();
        if (definition.primaryItems().isPresent()) {
            HolderSet<Item> primaryItems = definition.primaryItems().get();
            if (primaryItems.iterator().hasNext()) {
                return primaryItems;
            }
        }
        return definition.supportedItems();
    }

}
