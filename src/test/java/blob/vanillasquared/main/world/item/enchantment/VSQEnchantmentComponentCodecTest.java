package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.serialization.DataResult;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VSQEnchantmentComponentCodecTest {
    private static HolderLookup.Provider registries;
    private static RegistryOps<Tag> registryOps;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
        registryOps = createSerializationContext(registries);
    }

    @Test
    void emptySlotEntryEncodesAsExplicitCompoundMarker() {
        CompoundTag encoded = assertCompound(getOrThrow(VSQEnchantmentSlotEntry.CODEC.encodeStart(registryOps, VSQEnchantmentSlotEntry.empty())));

        assertEquals(1, encoded.size());
        assertTrue(encoded.getBoolean("empty").orElse(false));
    }

    @Test
    void filledSlotEntryRoundTripsThroughNbt() {
        VSQEnchantmentSlotEntry entry = sharpnessEntry(3);

        CompoundTag encoded = assertCompound(getOrThrow(VSQEnchantmentSlotEntry.CODEC.encodeStart(registryOps, entry)));
        VSQEnchantmentSlotEntry decoded = getOrThrow(VSQEnchantmentSlotEntry.CODEC.parse(registryOps, encoded));

        assertEquals(entry, decoded);
    }

    @Test
    void legacyEmptyRepresentationsStillDecode() {
        assertEquals(VSQEnchantmentSlotEntry.empty(), getOrThrow(VSQEnchantmentSlotEntry.CODEC.parse(registryOps, EndTag.INSTANCE)));
        assertEquals(VSQEnchantmentSlotEntry.empty(), getOrThrow(VSQEnchantmentSlotEntry.CODEC.parse(registryOps, new CompoundTag())));
        assertEquals(VSQEnchantmentSlotEntry.empty(), getOrThrow(VSQEnchantmentSlotEntry.CODEC.parse(registryOps, StringTag.valueOf("null"))));
    }

    @Test
    void componentRoundTripsMixedEntriesWithStableListShape() {
        VSQEnchantmentComponent component = new VSQEnchantmentComponent(
                Optional.of(List.of(VSQEnchantmentSlotEntry.empty(), sharpnessEntry(3))),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of(sharpnessEntry(1))),
                Optional.of(List.of(VSQEnchantmentSlotEntry.empty())),
                Optional.empty()
        );

        CompoundTag encoded = assertCompound(getOrThrow(VSQEnchantmentComponent.CODEC.codec().encodeStart(registryOps, component)));
        VSQEnchantmentComponent decoded = getOrThrow(VSQEnchantmentComponent.CODEC.codec().parse(registryOps, encoded));

        assertEquals(component, decoded);

        ListTag special = encoded.getList("special").orElseThrow();
        assertEquals(2, special.size());
        assertInstanceOf(CompoundTag.class, special.get(0));
        assertInstanceOf(CompoundTag.class, special.get(1));
        assertTrue(((CompoundTag) special.get(0)).getBoolean("empty").orElse(false));
        assertTrue(((CompoundTag) special.get(1)).contains("id"));
        assertEquals(3, ((CompoundTag) special.get(1)).getInt("level").orElseThrow());
    }

    private static VSQEnchantmentSlotEntry sharpnessEntry(int level) {
        return VSQEnchantmentSlotEntry.of(sharpness(), level);
    }

    private static CompoundTag assertCompound(Tag tag) {
        return assertInstanceOf(CompoundTag.class, tag);
    }

    private static Holder<Enchantment> sharpness() {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    }

    @SuppressWarnings("unchecked")
    private static RegistryOps<Tag> createSerializationContext(HolderLookup.Provider registries) {
        try {
            var method = registries.getClass().getMethod("createSerializationContext", com.mojang.serialization.DynamicOps.class);
            method.setAccessible(true);
            return (RegistryOps<Tag>) method.invoke(registries, NbtOps.INSTANCE);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create registry serialization context", exception);
        }
    }

    private static <T> T getOrThrow(DataResult<T> result) {
        return result.getOrThrow(message -> new AssertionError(message));
    }
}
