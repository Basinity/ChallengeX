package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Locale;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.change_weather}: sets the weather to clear, rain, or thunder.
 * Playerless: it acts on the world's weather for everyone.
 */
public final class ChangeWeatherHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeWeatherHandler.class);
    private static final int DURATION_TICKS = 6000;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String value = EffectParams.string(command, "value");
        if (value == null) {
            return;
        }
        World world = Worlds.overworld(server);
        // The storm and thunder flags go first: setStorm resets the clear-weather
        // duration, so setting durations before it would be undone.
        switch (value.toLowerCase(Locale.ROOT)) {
            case "clear", "sun" -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(DURATION_TICKS);
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(DURATION_TICKS);
            }
            case "thunder", "storm" -> {
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(DURATION_TICKS);
                world.setThunderDuration(DURATION_TICKS);
            }
            default -> LOGGER.warn("Unknown weather value {}; expected clear, rain, or thunder.", value);
        }
    }
}
