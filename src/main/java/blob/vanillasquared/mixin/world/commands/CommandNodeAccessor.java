package blob.vanillasquared.mixin.world.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandNode.class)
public interface CommandNodeAccessor<S> {
    @Accessor("command")
    void vsq$setCommand(Command<S> command);
}
