package blob.vanillasquared.main.world.item.enchantment;

public interface VSQEnchantmentAccess {
    VSQEnchantmentSlotType vsq$getEnchantmentSlotType();

    void vsq$setEnchantmentSlotType(VSQEnchantmentSlotType slotType);

    java.util.List<VSQEnchantmentProfile> vsq$getProfiles();

    void vsq$setProfiles(java.util.List<VSQEnchantmentProfile> profiles);
}
