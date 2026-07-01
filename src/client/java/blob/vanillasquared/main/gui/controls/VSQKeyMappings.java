package blob.vanillasquared.main.gui.controls;

import blob.vanillasquared.main.network.payload.SpecialEnchantmentHotkeyPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class VSQKeyMappings {
    private static final KeyMapping ENCHANTMENT_HOTKEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.vsq.enchantment_hotkey",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            KeyMapping.Category.MISC
    ));

    private VSQKeyMappings() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ENCHANTMENT_HOTKEY.consumeClick()) {
                if (client.player != null && client.getConnection() != null) {
                    ClientPlayNetworking.send(SpecialEnchantmentHotkeyPayload.INSTANCE);
                }
            }
        });
    }
}
