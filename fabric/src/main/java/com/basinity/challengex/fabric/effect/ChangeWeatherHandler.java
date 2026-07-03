package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.change_weather}: sets the weather to clear, rain, or thunder.
 * Playerless: it acts on the server's weather for everyone.
 */
public final class ChangeWeatherHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeWeatherHandler.class);
    private static final int DURATION_TICKS = 6000;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String value = EffectParams.string(command, "value");
        if (value == null) {
            return;
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "clear", "sun" -> server.setWeatherParameters(DURATION_TICKS, 0, false, false);
            case "rain" -> server.setWeatherParameters(0, DURATION_TICKS, true, false);
            case "thunder", "storm" -> server.setWeatherParameters(0, DURATION_TICKS, true, true);
            default -> LOGGER.warn("Unknown weather value {}; expected clear, rain, or thunder.", value);
        }
    }
}
