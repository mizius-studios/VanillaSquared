package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.util.api.references.RegistryReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record EnchantingIngredient(Ingredient ingredient, LevelBasedValue count, Identifier tagId) {
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
                LevelBasedValue count = EnchantingRecipeValue.constant(1);
                if (countValue != null) {
                    var parsedCount = EnchantingRecipeValue.CODEC.parse(ops, countValue).result();
                    if (parsedCount.isEmpty()) {
                        return DataResult.error(() -> "Invalid ingredient count");
                    }
                    count = parsedCount.get();
                }
                final LevelBasedValue finalCount = count;

                T itemValue = mapLike.get("item");
                if (itemValue != null) {
                    var itemReferenceResult = RegistryReference.CODEC.parse(ops, itemValue).result();
                    if (itemReferenceResult.isEmpty()) {
                        return DataResult.error(() -> "Invalid item identifier");
                    }

                    RegistryReference itemReference = itemReferenceResult.get();
                    if (itemReference.tag()) {
                        return DataResult.success(Pair.of(new EnchantingIngredient(null, finalCount, itemReference.id()), input));
                    }

                    Item item = BuiltInRegistries.ITEM.getValue(itemReference.id());
                    if (item == null) {
                        return DataResult.error(() -> "Unknown item: " + itemReference.id());
                    }
                    return DataResult.success(Pair.of(new EnchantingIngredient(Ingredient.of(item), finalCount, null), input));
                }

                if (mapLike.get("fabric:type") != null) {
                    T ingredientInput = vsq$removeCount(ops, input);
                    return Ingredient.CODEC.parse(ops, ingredientInput)
                            .map(parsedIngredient -> Pair.of(new EnchantingIngredient(parsedIngredient, finalCount, null), input));
                }

                return DataResult.error(() -> "Ingredient object must contain 'item' or 'fabric:type'");
            });
        }

        @Override
        public <T> DataResult<T> encode(EnchantingIngredient input, DynamicOps<T> ops, T prefix) {
            if (input.tagId != null) {
                var builder = ops.mapBuilder();
                builder.add("item", RegistryReference.CODEC.encodeStart(ops, RegistryReference.tag(input.tagId)).getOrThrow());
                if (!input.hasDefaultCount()) {
                    builder.add("count", EnchantingRecipeValue.CODEC.encodeStart(ops, input.count).getOrThrow());
                }
                return builder.build(prefix);
            }

            return Ingredient.CODEC.encodeStart(ops, input.ingredient).flatMap(encoded -> {
                var builder = ops.mapBuilder();
                var encodedMap = ops.getMap(encoded).result();
                if (encodedMap.isPresent()) {
                    encodedMap.get().entries().forEach(entry -> builder.add(entry.getFirst(), entry.getSecond()));
                } else {
                    builder.add("item", encoded);
                }
                if (!input.hasDefaultCount()) {
                    builder.add(ops.createString("count"), EnchantingRecipeValue.CODEC.encodeStart(ops, input.count).getOrThrow());
                }
                return builder.build(prefix);
            });
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingIngredient> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnchantingIngredient decode(RegistryFriendlyByteBuf buf) {
            boolean hasTag = buf.readBoolean();
            LevelBasedValue count = EnchantingRecipeValue.STREAM_CODEC.decode(buf);
            if (hasTag) {
                return new EnchantingIngredient(null, count, Identifier.STREAM_CODEC.decode(buf));
            }
            return new EnchantingIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), count, null);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantingIngredient value) {
            buf.writeBoolean(value.tagId != null);
            EnchantingRecipeValue.STREAM_CODEC.encode(buf, value.count);
            if (value.tagId != null) {
                Identifier.STREAM_CODEC.encode(buf, value.tagId);
                return;
            }
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.ingredient);
        }
    };

    public EnchantingIngredient {
        if (count == null) {
            throw new IllegalArgumentException("Enchanting ingredient count must be defined");
        }
        if ((ingredient == null) == (tagId == null)) {
            throw new IllegalArgumentException("Enchanting ingredient must define exactly one of ingredient or tagId");
        }
    }

    public boolean test(ItemStack stack) {
        return this.test(stack, 1);
    }

    public boolean test(ItemStack stack, int level) {
        if (stack.getCount() < this.count(level)) {
            return false;
        }
        return this.matchesIgnoringCount(stack);
    }

    public int count(int level) {
        return EnchantingRecipeValue.itemCount(this.count, level);
    }

    public boolean hasDefaultCount() {
        return this.count instanceof LevelBasedValue.Constant constant && constant.value() == 1.0F;
    }

    public boolean matchesIgnoringCount(ItemStack stack) {
        if (this.tagId != null) {
            return stack.is(TagKey.create(Registries.ITEM, this.tagId));
        }
        return this.ingredient.test(stack);
    }

    public Ingredient ingredient() {
        if (this.ingredient != null) {
            return this.ingredient;
        }
        return this.safeIngredient().orElseGet(() -> Ingredient.of(Stream.empty()));
    }

    public Optional<Ingredient> safeIngredient() {
        if (this.ingredient != null) {
            return Optional.of(this.ingredient);
        }

        return TAG_INGREDIENT_CACHE.computeIfAbsent(this.tagId, EnchantingIngredient::resolveTagIngredient);
    }

    private static Optional<Ingredient> resolveTagIngredient(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        List<Holder<Item>> holders = StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).spliterator(), false)
                .toList();
        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Ingredient.of(HolderSet.direct(holders)));
    }

    public SlotDisplay display() {
        return this.display(this.count(1));
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
            return new SlotDisplay.TagSlotDisplay(TagKey.create(Registries.ITEM, this.tagId));
        }
        return Empty.INSTANCE;
    }

    public ItemStack previewStack() {
        return this.previewStacks().stream().findFirst().orElse(ItemStack.EMPTY);
    }

    private List<ItemStack> previewStacks() {
        if (this.ingredient != null) {
            return this.ingredient.display()
                    .resolveForStacks(new ContextMap.Builder().create(SlotDisplayContext.CONTEXT));
        }

        return List.copyOf(TAG_PREVIEW_CACHE.computeIfAbsent(this.tagId, EnchantingIngredient::resolveTagPreview));
    }

    private static List<ItemStack> resolveTagPreview(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).spliterator(), false)
                .map(holder -> new ItemStack(holder.value()))
                .toList();
    }

    private static <T> T vsq$removeCount(DynamicOps<T> ops, T input) {
        JsonElement element = ops.convertTo(JsonOps.INSTANCE, input);
        if (!(element instanceof JsonObject json)) {
            return input;
        }
        JsonObject copy = json.deepCopy();
        copy.remove("count");
        return JsonOps.INSTANCE.convertTo(ops, copy);
    }
}
