package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

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
        if (!this.canApplyNextLevel(originalStack, registries) || !VSQEnchantmentSlots.canApplyInSlots(originalStack, enchantment, nextLevel)) {
            return originalStack.copy();
        }
        return VSQEnchantmentSlots.applyEnchant(originalStack, enchantment, nextLevel);
    }

    public Ingredient supportedItemsIngredient(HolderLookup.Provider registries) {
        return Ingredient.of(this.vsq$enchantmentHolder(registries).value().definition().supportedItems());
    }

    public ItemStack previewInputStack(HolderLookup.Provider registries) {
        return this.vsq$enchantmentHolder(registries).value().definition().supportedItems().stream()
                .findFirst()
                .map(holder -> new ItemStack(holder.value()))
                .orElse(ItemStack.EMPTY);
    }

    public SlotDisplay iconDisplay(Component name, HolderLookup.Provider registries) {
        List<SlotDisplay> displays = new ArrayList<>();
        for (Holder<Item> holder : this.vsq$enchantmentHolder(registries).value().definition().supportedItems()) {
            ItemStack stack = new ItemStack(holder.value());
            stack.set(DataComponents.ITEM_NAME, name.copy());
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            displays.add(new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack)));
        }

        if (displays.isEmpty()) {
            ItemStack fallback = new ItemStack(Items.ENCHANTED_BOOK);
            fallback.set(DataComponents.ITEM_NAME, name.copy());
            fallback.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(fallback));
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
        return Math.max(VSQEnchantmentSlots.currentLevel(originalStack, enchantment), 0);
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
        return Math.max(VSQEnchantmentSlots.maxLevel(originalStack, this.vsq$enchantmentHolder(registries)), 1);
    }

    public int xpCost(ItemStack originalStack, HolderLookup.Provider registries) {
        if (!this.canApplyNextLevel(originalStack, registries)) {
            return 0;
        }
        return this.nextLevel(originalStack, registries) * 3;
    }

    public Component displayName(ItemStack originalStack, HolderLookup.Provider registries) {
        int level = this.nextLevel(originalStack, registries);
        return this.vsq$enchantmentHolder(registries).value().description().copy()
                .append(Component.literal(" "))
                .append(Component.translatable("enchantment.level." + level));
    }

    public boolean isBelowMaximumLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        return this.currentLevel(originalStack, registries) < this.maxLevel(originalStack, registries);
    }

    public boolean canApplyNextLevel(ItemStack originalStack, HolderLookup.Provider registries) {
        return this.isBelowMaximumLevel(originalStack, registries);
    }

    public boolean respectsVanillaEnchantmentIncompatibilities(ItemStack originalStack, HolderLookup.Provider registries) {
        var entries = VSQEnchantmentSlots.aggregate(this.apply(originalStack, registries)).entrySet().stream().toList();
        for (int leftIndex = 0; leftIndex < entries.size(); leftIndex++) {
            var left = entries.get(leftIndex).getKey();
            for (int rightIndex = leftIndex + 1; rightIndex < entries.size(); rightIndex++) {
                var right = entries.get(rightIndex).getKey();
                if (!VSQEnchantmentSlots.areCompatible(originalStack, left, right)) {
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

}
