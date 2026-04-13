package blob.vanillasquared.main.world.recipe.enchanting;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

public record EnchantingIngredient(Ingredient ingredient, int count, Identifier tagId) {
    private static final Codec<Integer> COUNT_CODEC = Codec.intRange(1, Item.ABSOLUTE_MAX_STACK_SIZE);
    private static final Map<Identifier, Optional<Ingredient>> TAG_INGREDIENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, List<ItemStack>> TAG_PREVIEW_CACHE = new ConcurrentHashMap<>();

    public static void clearTagCache() {
        TAG_INGREDIENT_CACHE.clear();
        TAG_PREVIEW_CACHE.clear();
    }

    public static final Codec<EnchantingIngredient> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<EnchantingIngredient, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(mapLike -> {
                T countValue = mapLike.get("count");
                int count = 1;
                if (countValue != null) {
                    var parsedCount = COUNT_CODEC.parse(ops, countValue).result();
                    if (parsedCount.isEmpty()) {
                        return DataResult.error(() -> "Ingredient count must be between 1 and " + Item.ABSOLUTE_MAX_STACK_SIZE);
                    }
                    count = parsedCount.get();
                }
                final int finalCount = count;

                T tagValue = mapLike.get("tag");
                if (tagValue != null) {
                    var tagIdResult = Identifier.CODEC.parse(ops, tagValue).result();
                    if (tagIdResult.isEmpty()) {
                        return DataResult.error(() -> "Invalid item tag identifier");
                    }

                    Identifier parsedTagId = tagIdResult.get();
                    return DataResult.success(Pair.of(new EnchantingIngredient(null, finalCount, parsedTagId), input));
                }

                if (mapLike.get("fabric:type") != null) {
                    T ingredientInput = vsq$removeCount(ops, input);
                    return Ingredient.CODEC.parse(ops, ingredientInput)
                            .map(parsedIngredient -> Pair.of(new EnchantingIngredient(parsedIngredient, finalCount, null), input));
                }

                T itemValue = mapLike.get("item");
                if (itemValue != null) {
                    var itemIdResult = Identifier.CODEC.parse(ops, itemValue).result();
                    if (itemIdResult.isEmpty()) {
                        return DataResult.error(() -> "Invalid item identifier");
                    }

                    Identifier itemId = itemIdResult.get();
                    Item item = BuiltInRegistries.ITEM.getValue(itemId);
                    if (item == null) {
                        return DataResult.error(() -> "Unknown item: " + itemId);
                    }
                    return DataResult.success(Pair.of(new EnchantingIngredient(Ingredient.of(item), finalCount, null), input));
                }

                return DataResult.error(() -> "Ingredient object must contain 'item', 'tag', or 'fabric:type'");
            });
        }

        @Override
        public <T> DataResult<T> encode(EnchantingIngredient input, DynamicOps<T> ops, T prefix) {
            if (input.tagId != null) {
                var builder = ops.mapBuilder();
                builder.add("tag", Identifier.CODEC.encodeStart(ops, input.tagId).getOrThrow());
                if (input.count != 1) {
                    builder.add("count", ops.createInt(input.count));
                }
                return builder.build(prefix);
            }

            return Ingredient.CODEC.encodeStart(ops, input.ingredient).flatMap(encoded -> {
                return ops.getMap(encoded).flatMap(map -> {
                    var builder = ops.mapBuilder();
                    map.entries().forEach(entry -> builder.add(entry.getFirst(), entry.getSecond()));
                    if (input.count != 1) {
                        builder.add(ops.createString("count"), ops.createInt(input.count));
                    }
                    return builder.build(prefix);
                });
            });
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingIngredient> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnchantingIngredient decode(RegistryFriendlyByteBuf buf) {
            boolean hasTag = buf.readBoolean();
            int count = ByteBufCodecs.VAR_INT.decode(buf);
            if (hasTag) {
                return new EnchantingIngredient(null, count, Identifier.STREAM_CODEC.decode(buf));
            }
            return new EnchantingIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), count, null);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantingIngredient value) {
            buf.writeBoolean(value.tagId != null);
            ByteBufCodecs.VAR_INT.encode(buf, value.count);
            if (value.tagId != null) {
                Identifier.STREAM_CODEC.encode(buf, value.tagId);
                return;
            }
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.ingredient);
        }
    };

    public EnchantingIngredient {
        if (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE) {
            throw new IllegalArgumentException("Enchanting ingredient count must be between 1 and " + Item.ABSOLUTE_MAX_STACK_SIZE);
        }
        if ((ingredient == null) == (tagId == null)) {
            throw new IllegalArgumentException("Enchanting ingredient must define exactly one of ingredient or tagId");
        }
    }

    public boolean test(ItemStack stack) {
        if (stack.getCount() < this.count) {
            return false;
        }
        return this.matchesIgnoringCount(stack);
    }

    public boolean matchesIgnoringCount(ItemStack stack) {
        if (this.tagId != null) {
            return stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, this.tagId));
        }
        return this.ingredient.test(stack);
    }

    public Ingredient ingredient() {
        if (this.ingredient != null) {
            return this.ingredient;
        }
        return this.safeIngredient().orElse(Ingredient.EMPTY);
    }

    public Optional<Ingredient> safeIngredient() {
        if (this.ingredient != null) {
            return Optional.of(this.ingredient);
        }

        return TAG_INGREDIENT_CACHE.computeIfAbsent(this.tagId, EnchantingIngredient::resolveTagIngredient);
    }

    private static Optional<Ingredient> resolveTagIngredient(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
        List<Holder<Item>> holders = StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).spliterator(), false)
                .toList();
        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Ingredient.of(HolderSet.direct(holders)));
    }

    public SlotDisplay display() {
        return this.display(this.count);
    }

    public SlotDisplay display(int displayCount) {
        List<SlotDisplay> displays = this.previewStacks().stream()
                .map(stack -> {
                    ItemStack preview = stack.copy();
                    preview.setCount(displayCount);
                    return (SlotDisplay) new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(preview));
                })
                .toList();
        if (displays.size() == 1) {
            return displays.getFirst();
        }
        if (!displays.isEmpty()) {
            return new SlotDisplay.Composite(displays);
        }
        if (this.tagId != null) {
            return new SlotDisplay.TagSlotDisplay(TagKey.create(net.minecraft.core.registries.Registries.ITEM, this.tagId));
        }
        return Empty.INSTANCE;
    }

    public ItemStack previewStack() {
        return this.previewStacks().stream().findFirst().orElse(ItemStack.EMPTY);
    }

    private List<ItemStack> previewStacks() {
        if (this.ingredient != null) {
            return this.ingredient.items()
                    .map(holder -> new ItemStack(holder.value()))
                    .toList();
        }

        return List.copyOf(TAG_PREVIEW_CACHE.computeIfAbsent(this.tagId, EnchantingIngredient::resolveTagPreview));
    }

    private static List<ItemStack> resolveTagPreview(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).spliterator(), false)
                .map(holder -> new ItemStack(holder.value()))
                .toList();
    }

    private static <T> T vsq$removeCount(DynamicOps<T> ops, T input) {
        JsonObject json = (JsonObject) ops.convertTo(JsonOps.INSTANCE, input);
        JsonObject copy = json.deepCopy();
        copy.remove("count");
        return JsonOps.INSTANCE.convertTo(ops, copy);
    }
}
