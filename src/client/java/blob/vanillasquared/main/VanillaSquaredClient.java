package blob.vanillasquared.main;

import blob.vanillasquared.main.gui.settings.controls.vsqControls;
import net.fabricmc.api.ClientModInitializer;

public class VanillaSquaredClient implements ClientModInitializer {
    public static final String MOD_ID = "vsq";
    @Override
    public void onInitializeClient() {
        vsqControls.initialize();
    }
}
