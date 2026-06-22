package blob.vanillasquared.main.world.recipe.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

/** A fixed item and component patch used to represent an enchanting recipe. */
public record EnchantingRecipeIcon(Holder<Item> id, DataComponentPatch components) {
    public static final Codec<EnchantingRecipeIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Item.CODEC.fieldOf("id").forGetter(EnchantingRecipeIcon::id),
            DataComponentPatch.CODEC.fieldOf("components").forGetter(EnchantingRecipeIcon::components)
    ).apply(instance, EnchantingRecipeIcon::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipeIcon> STREAM_CODEC = StreamCodec.composite(
            Item.STREAM_CODEC, EnchantingRecipeIcon::id,
            DataComponentPatch.STREAM_CODEC, EnchantingRecipeIcon::components,
            EnchantingRecipeIcon::new
    );

    public ItemStack itemStack() {
        return new ItemStack(this.id, 1, this.components);
    }

    public SlotDisplay display() {
        return new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(this.id, 1, this.components));
    }

    public Component name() {
        Component configuredName = this.components.get(DataComponentMap.EMPTY, DataComponents.ITEM_NAME);
        return configuredName != null ? configuredName.copy() : Component.translatable(this.id.value().getDescriptionId());
    }

    public Component description() {
        ItemLore lore = this.components.get(DataComponentMap.EMPTY, DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return Component.empty();
        }

        List<Component> lines = lore.lines();
        MutableComponent description = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                description.append("\n");
            }
            description.append(lines.get(index).copy());
        }
        return description;
    }
}
