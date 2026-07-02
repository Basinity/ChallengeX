package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.Engine;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.registry.CoreCatalog;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChallengeXFabric implements ModInitializer {

    public static final String MOD_ID = "challengex";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Prove the core engine is on the classpath and constructible.
        // Event wiring follows in the vertical-slice phase.
        new Engine(Challenge.empty(), CoreCatalog.createRegistries());
        LOGGER.info("ChallengeX initialized.");
    }
}
