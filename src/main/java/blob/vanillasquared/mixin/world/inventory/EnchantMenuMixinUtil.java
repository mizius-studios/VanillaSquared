package blob.vanillasquared.mixin.world.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Unique;

public class EnchantMenuMixinUtil {
    @Unique
    public static final int VSQ$DUMMYLEVELREQUIREMENT = 69;
    @Unique
    public static final int VSQ$DUMMYBLOCKREQUIREMENT = 4;
    @Unique
    public static final TagKey<Block> VSQ$DUMMYBLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("vsq", "dummy"));
    @Unique
    public static int VSQ$PlAYERLEVEL = 0;
    @Unique
    public static int VSQ$BLOCKAMOUNT = 0;
    @Unique
    public static int getBlockRequirement() {
        return VSQ$DUMMYBLOCKREQUIREMENT;
    }
    @Unique
    public static int getLevelRequirement() {
        return VSQ$DUMMYLEVELREQUIREMENT;
    }
    @Unique
    private static TagKey<Block> getDummyBlocks() {
        return VSQ$DUMMYBLOCKS;
    }
    @Unique
    public static int getPlayerLevel() {
        return VSQ$PlAYERLEVEL;
    }
    @Unique
    public static void setPlayerLevel(int level) {
        VSQ$BLOCKAMOUNT = level;
    }
    @Unique
    public static int getBlockAmount() {
        return VSQ$BLOCKAMOUNT;
    }
    @Unique
    public static void setBlockAmount(int amount) {
        VSQ$BLOCKAMOUNT = amount;
    }
}
