package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.loot.LootContextBridge;
import blob.vanillasquared.main.world.loot.LootTableIdResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@Mixin(LootContext.class)
public abstract class LootContextMixin implements LootContextBridge {
    @Unique
    private final Deque<LootTable> vsq$lootTableStack = new ArrayDeque<>();

    @Inject(method = "pushVisitedElement", at = @At("RETURN"))
    private void vsq$trackLootTablePush(LootContext.VisitedEntry<?> element, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && element.type() == LootDataType.TABLE && element.value() instanceof LootTable table) {
            this.vsq$lootTableStack.addLast(table);
        }
    }

    @Inject(method = "popVisitedElement", at = @At("HEAD"))
    private void vsq$trackLootTablePop(LootContext.VisitedEntry<?> element, CallbackInfo ci) {
        if (element.type() == LootDataType.TABLE
                && element.value() instanceof LootTable table
                && !this.vsq$lootTableStack.isEmpty()
                && this.vsq$lootTableStack.peekLast() == table) {
            this.vsq$lootTableStack.removeLast();
        }
    }

    @Override
    public Optional<Identifier> vsq$currentLootTableId() {
        LootTable table = this.vsq$lootTableStack.peekLast();
        if (table == null) {
            return Optional.empty();
        }
        return LootTableIdResolver.lookup((LootContext) (Object) this, table);
    }
}
