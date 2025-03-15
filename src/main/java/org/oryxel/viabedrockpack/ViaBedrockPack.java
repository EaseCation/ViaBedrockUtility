package org.oryxel.viabedrockpack;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViaBedrockPack implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("viabedrockpack");

	@Override
	public void onInitialize() {
		LOGGER.info("Hello ViaBedrockPack!");
	}
}