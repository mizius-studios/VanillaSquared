package blob.vanillasquared.main;

import blob.vanillasquared.main.network.VSQNetworking;
import blob.vanillasquared.main.world.item.Items.TestItem;
import net.fabricmc.api.ModInitializer;
import blob.vanillasquared.main.world.item.components.dualwield.DualWieldEvents;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectEvents;
import blob.vanillasquared.util.api.modules.attributes.RegisterAttributes;
import blob.vanillasquared.util.api.modules.components.RegisterComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaSquared implements ModInitializer {
    public static final String MOD_ID = "vsq";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {
        TestItem.initialize();
        RegisterComponents.initialize();
        DualWieldEvents.initialize();
        SpecialEffectEvents.initialize();
        VSQNetworking.initialize();

        LOGGER.info("Blob");

        RegisterAttributes.initialize();
    }
}
