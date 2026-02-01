package blob.vanillasquared;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaSquared implements ModInitializer {
	public static final String MOD_ID = "vanilla-squared";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/*
	private int healPlayer(ServerPlayer player, CommandSourceStack context) {
		if (player != null) {
			player.setHealth(player.getMaxHealth()); // Sets health to max
			player.getFoodData().eat(20, 1.0f); // Optional: Feeds the player
			context.sendSuccess(() -> Component.literal("Healed"), false);
			return 1;
		}
		return 0;
	}
	*/

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		TestItem.initialize();

		LOGGER.info("Loaded 1 Blob Mods");

		/*
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("heal").executes(context -> healPlayer(context.getSource().getPlayer(), context.getSource())));
		});
		*/
	}
}