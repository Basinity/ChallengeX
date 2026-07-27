package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * {@code trigger.weather_changed}: the overworld's weather turned over, between
 * clear, raining, and thundering. The {@code weather} parameter matches the new
 * weather ({@code clear} / {@code rain} / {@code thunder}); omitting it fires on
 * any change. Playerless: the weather belongs to the world, so no player set it off.
 */
public final class WeatherChangeTriggerSource implements TriggerSource {

    private String lastSeen;

    @Override
    public void register(TriggerContext context, Plugin plugin) {
        World overworld = plugin.getServer().getWorlds().getFirst();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            String current = describe(overworld);
            // The first tick only records a baseline; a fresh world starts over.
            if (lastSeen != null && !lastSeen.equals(current)) {
                context.emit(GameEvent.playerless("trigger.weather_changed",
                        Map.of("weather", ParamValue.of(current))));
            }
            lastSeen = current;
        }, 1L, 1L);
    }

    private static String describe(World world) {
        if (world.isThundering()) {
            return "thunder";
        }
        return world.hasStorm() ? "rain" : "clear";
    }
}
