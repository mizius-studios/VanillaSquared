package blob.vanillasquared.main.gui.settings.controls;

import blob.vanillasquared.main.VanillaSquaredClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class vsqControls {
    KeyMapping.Category vsqControlsCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "vsq_controls"));
    KeyMapping specialEffectKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("vsq.gui.controls.vsq.keybinds.special_effect", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, vsqControlsCategory));
    public static void initialize() {
    }
}