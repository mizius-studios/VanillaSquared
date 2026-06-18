package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.util.api.references.RegistryReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public record EnchantingBlockRequirement(
        Identifier blockId,
        Identifier tagId,
        LevelBasedValue count
) {
    public static final Codec<EnchantingBlockRequirement> CODEC = ExtraCodecs.JSON.flatXmap(
            EnchantingBlockRequirement::vsq$decode,
            EnchantingBlockRequirement::vsq$encode
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingBlockRequirement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, requirement -> requirement.tagId() != null,
            Identifier.STREAM_CODEC, requirement -> requirement.tagId() != null ? requirement.tagId() : requirement.blockId(),
            EnchantingRecipeValue.STREAM_CODEC, EnchantingBlockRequirement::count,
            EnchantingBlockRequirement::vsq$decodeStream
    );

    public EnchantingBlockRequirement {
        if ((blockId == null) == (tagId == null)) {
            throw new IllegalArgumentException("Enchanting block requirements must define exactly one block reference");
        }
        if (count == null) {
            throw new IllegalArgumentException("Enchanting block requirement count must be defined");
        }
    }

    public static EnchantingBlockRequirement forBlock(Identifier blockId, int count) {
        return forBlock(blockId, EnchantingRecipeValue.constant(count));
    }

    public static EnchantingBlockRequirement forBlock(Identifier blockId, LevelBasedValue count) {
        return new EnchantingBlockRequirement(blockId, null, count);
    }

    public static EnchantingBlockRequirement forTag(Identifier tagId, int count) {
        return forTag(tagId, EnchantingRecipeValue.constant(count));
    }

    public static EnchantingBlockRequirement forTag(Identifier tagId, LevelBasedValue count) {
        return new EnchantingBlockRequirement(null, tagId, count);
    }

    public int placedCount(Map<Identifier, Integer> countedBlocks) {
        if (this.blockId != null) {
            return countedBlocks.getOrDefault(this.blockId, 0);
        }

        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, this.tagId);
        int total = 0;
        for (Map.Entry<Identifier, Integer> entry : countedBlocks.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.getValue(entry.getKey());
            if (block.defaultBlockState().is(tagKey)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    public boolean matches(Map<Identifier, Integer> countedBlocks, int level) {
        return this.placedCount(countedBlocks) >= this.count(level);
    }

    public int count(int level) {
        return EnchantingRecipeValue.blockCount(this.count, level);
    }

    public Identifier displayBlockId() {
        if (this.blockId != null) {
            return this.blockId;
        }

        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, this.tagId);
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey)) {
            return BuiltInRegistries.BLOCK.getKey(holder.value());
        }
        return BuiltInRegistries.BLOCK.getKey(Blocks.BARRIER);
    }

    private static EnchantingBlockRequirement vsq$decodeStream(boolean isTag, Identifier id, LevelBasedValue count) {
        return isTag ? forTag(id, count) : forBlock(id, count);
    }

    private static DataResult<EnchantingBlockRequirement> vsq$decode(JsonElement json) {
        if (!json.isJsonObject()) {
            return DataResult.error(() -> "Enchanting block requirements must be JSON objects");
        }

        JsonObject object = json.getAsJsonObject();
        boolean hasBlock = object.has("block");
        if (!hasBlock) {
            return DataResult.error(() -> "Enchanting block requirements must define a block reference");
        }

        LevelBasedValue count = EnchantingRecipeValue.constant(1);
        if (object.has("count")) {
            var countResult = EnchantingRecipeValue.CODEC.parse(JsonOps.INSTANCE, object.get("count")).result();
            if (countResult.isEmpty()) {
                return DataResult.error(() -> "Invalid enchanting block requirement count");
            }
            count = countResult.get();
        }

        try {
            var blockReferenceResult = RegistryReference.CODEC.parse(JsonOps.INSTANCE, object.get("block")).result();
            if (blockReferenceResult.isEmpty()) {
                return DataResult.error(() -> "Invalid block reference in Enchanting block requirement");
            }

            RegistryReference blockReference = blockReferenceResult.get();
            if (blockReference.tag()) {
                return DataResult.success(forTag(blockReference.id(), count));
            }
            return DataResult.success(forBlock(blockReference.id(), count));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DataResult<JsonElement> vsq$encode(EnchantingBlockRequirement requirement) {
        JsonObject object = new JsonObject();
        if (requirement.blockId() != null) {
            object.addProperty("block", requirement.blockId().toString());
        } else {
            object.addProperty("block", RegistryReference.tag(requirement.tagId()).asString());
        }
        object.add("count", EnchantingRecipeValue.CODEC.encodeStart(JsonOps.INSTANCE, requirement.count()).getOrThrow());
        return DataResult.success(object);
    }
}
