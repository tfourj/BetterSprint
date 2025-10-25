package me.tfourj.bettersprint;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bettersprint implements ModInitializer {
    public static final String MOD_ID = "bettersprint";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.debug("BetterSprint base initialization complete");
    }
}
