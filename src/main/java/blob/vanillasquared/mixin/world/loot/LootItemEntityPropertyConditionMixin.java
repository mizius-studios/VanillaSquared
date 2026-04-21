package blob.vanillasquared.mixin.world.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(LootItemEntityPropertyCondition.class)
public abstract class LootItemEntityPropertyConditionMixin {
    @Shadow
    @Final
    @Mutable
    public static MapCodec<LootItemEntityPropertyCondition> MAP_CODEC;

    @Shadow
    public abstract Optional<EntityPredicate> predicate();

    @Shadow
    public abstract LootContext.EntityTarget entityTarget();

    @Unique
    private static final ThreadLocal<Boolean> VSQ_DECODE_ALL_ENTITIES = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Boolean> VSQ_ENCODE_ALL_ENTITIES = ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean vsq$allEntities;

    @Unique
    private static final Codec<LootContext.EntityTarget> VSQ_ENTITY_TARGET_CODEC = Codec.STRING.flatXmap(
            name -> {
                if ("entities".equals(name)) {
                    VSQ_DECODE_ALL_ENTITIES.set(true);
                    return DataResult.success(LootContext.EntityTarget.THIS);
                }

                LootContext.EntityTarget target = LootContext.EntityTarget.CODEC.byName(name);
                VSQ_DECODE_ALL_ENTITIES.set(false);
                return target == null ? DataResult.error(() -> "Invalid entity target " + name) : DataResult.success(target);
            },
            target -> {
                if (Boolean.TRUE.equals(VSQ_ENCODE_ALL_ENTITIES.get())) {
                    VSQ_ENCODE_ALL_ENTITIES.set(false);
                    return DataResult.success("entities");
                }

                return DataResult.success(target.getSerializedName());
            }
    );

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vsq$rememberAllEntitiesFlag(Optional<EntityPredicate> predicate, LootContext.EntityTarget entityTarget, CallbackInfo ci) {
        this.vsq$allEntities = Boolean.TRUE.equals(VSQ_DECODE_ALL_ENTITIES.get());
        VSQ_DECODE_ALL_ENTITIES.remove();
    }

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void vsq$testAllEntities(LootContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!this.vsq$allEntities) {
            return;
        }

        Optional<EntityPredicate> predicate = this.predicate();
        if (predicate.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }

        var level = context.getLevel();
        var origin = context.getOptionalParameter(LootContextParams.ORIGIN);
        boolean matched = false;
        for (var entity : level.getAllEntities()) {
            var resolvedOrigin = origin != null ? origin : entity.position();
            if (predicate.get().matches(level, resolvedOrigin, entity)) {
                matched = true;
                break;
            }
        }

        cir.setReturnValue(matched);
    }

    @Inject(method = "getReferencedContextParams", at = @At("HEAD"), cancellable = true)
    private void vsq$allEntitiesReferenceContext(CallbackInfoReturnable<Set<ContextKey<?>>> cir) {
        if (!this.vsq$allEntities) {
            return;
        }

        cir.setReturnValue(Set.of(LootContextParams.ORIGIN));
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$extendCodec(CallbackInfo ci) {
        MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(LootItemEntityPropertyCondition::predicate),
                VSQ_ENTITY_TARGET_CODEC.fieldOf("entity").forGetter(LootItemEntityPropertyConditionMixin::vsq$entityTargetForCodec)
        ).apply(instance, LootItemEntityPropertyCondition::new));
    }

    @Unique
    private static LootContext.EntityTarget vsq$entityTargetForCodec(LootItemEntityPropertyCondition condition) {
        LootItemEntityPropertyConditionMixin self = (LootItemEntityPropertyConditionMixin) (Object) condition;
        VSQ_ENCODE_ALL_ENTITIES.set(self.vsq$allEntities);
        return condition.entityTarget();
    }
}
