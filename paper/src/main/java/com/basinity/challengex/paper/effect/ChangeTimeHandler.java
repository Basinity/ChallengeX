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
 * {@code effect.change_time}: sets the overworld time. Accepts the keywords
 * day, noon, night, and midnight (moved to the matching point in the day) or a
 * tick number set as the clock's absolute total. Playerless: it acts on the
 * world.
 *
 * <p>Where the Fabric adapter drives 26.2's clock manager by named time marker,
 * Bukkit still exposes the day as a tick offset, so the markers are the vanilla
 * tick values the {@code /time set} keywords map to.
 */
public final class ChangeTimeHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTimeHandler.class);

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String value = EffectParams.string(command, "value");
        if (value == null) {
            return;
        }
        World world = Worlds.overworld(server);
        Long marker = markerFor(value);
        if (marker != null) {
            world.setTime(marker);
            return;
        }
        try {
            long ticks = Long.parseLong(value.trim());
            world.setFullTime(Math.max(0L, ticks));
        } catch (NumberFormatException notANumber) {
            LOGGER.warn("Unknown time value {}; expected day, noon, night, midnight, or a tick number.", value);
        }
    }

    private static Long markerFor(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> null;
        };
    }
}
