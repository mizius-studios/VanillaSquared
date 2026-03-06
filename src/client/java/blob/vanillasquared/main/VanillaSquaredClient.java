package blob.vanillasquared.main;

import net.fabricmc.api.ClientModInitializer;
import blob.vanillasquared.main.gui.settings.controls.VSQControls;
import blob.vanillasquared.main.network.handlers.KeybindInputHandler;

public class VanillaSquaredClient implements ClientModInitializer {
    public static final String MOD_ID = "vsq";

    @Override
    public void onInitializeClient() {
        VSQControls.initialize();
        KeybindInputHandler.register();
    }
}
