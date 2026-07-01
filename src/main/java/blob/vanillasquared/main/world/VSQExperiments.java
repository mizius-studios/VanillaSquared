package blob.vanillasquared.main.world;

import blob.vanillasquared.main.VanillaSquared;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VSQExperiments {
    public static final Identifier PREVIEW_FEATURE_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "preview");
    public static final Identifier PREVIEW_BUILTIN_PACK_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "vsq_preview");

    private static final List<BuiltinExperiment> BUILTIN_EXPERIMENTS = List.of(
            new BuiltinExperiment(
                    PREVIEW_FEATURE_ID,
                    PREVIEW_BUILTIN_PACK_ID,
                    "vsq.gui.experiments.vsq",
                    "vsq.gui.experiments.vsq.description"
            )
    );

    private static final Map<Identifier, FeatureFlag> FEATURE_FLAGS = new ConcurrentHashMap<>();

    private VSQExperiments() {
    }

    public static void initialize() {
        ModContainer modContainer = FabricLoader.getInstance()
                .getModContainer("vanilla-squared")
                .orElseThrow(() -> new IllegalStateException("Missing mod container for vanilla-squared"));

        for (BuiltinExperiment experiment : BUILTIN_EXPERIMENTS) {
            if (!ResourceLoader.registerBuiltinPack(
                    experiment.packId(),
                    modContainer,
                    Component.translatable(experiment.titleKey()),
                    PackActivationType.NORMAL
            )) {
                VanillaSquared.LOGGER.warn("Failed to register VSQ builtin datapack {}", experiment.packId());
            }
        }

        ServerPlayerEvents.JOIN.register(VSQExperiments::grantPreviewRecipes);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> grantPreviewRecipes(newPlayer));
    }

    public static boolean isPreviewEnabled(ServerLevel level) {
        return isEnabled(level, PREVIEW_FEATURE_ID);
    }

    public static boolean isEnabled(ServerLevel level, Identifier featureId) {
        FeatureFlag featureFlag = FEATURE_FLAGS.get(featureId);
        return featureFlag != null && level.enabledFeatures().contains(featureFlag);
    }

    public static void grantPreviewRecipes(ServerPlayer player) {
        if (!isPreviewEnabled(player.level())) {
            return;
        }
    }

    public static List<Identifier> builtinFeatureIds() {
        return BUILTIN_EXPERIMENTS.stream()
                .map(BuiltinExperiment::featureId)
                .toList();
    }

    public static Set<String> builtinPackIds() {
        return BUILTIN_EXPERIMENTS.stream()
                .map(BuiltinExperiment::packId)
                .map(Identifier::toString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static boolean isBuiltinPackId(String packId) {
        return builtinPackIds().contains(packId);
    }

    public static void vsq$setFeatureFlag(Identifier featureId, FeatureFlag flag) {
        FEATURE_FLAGS.put(featureId, flag);
    }

    private record BuiltinExperiment(Identifier featureId, Identifier packId, String titleKey, String descriptionKey) {
    }
}
