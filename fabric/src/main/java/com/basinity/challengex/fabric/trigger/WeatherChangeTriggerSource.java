package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code trigger.weather_changed}: the overworld's weather turned over, between
 * clear, raining, and thundering. The {@code weather} parameter matches the new
 * weather ({@code clear} / {@code rain} / {@code thunder}); omitting it fires on
 * any change. Playerless: the weather belongs to the world, so no player set it off.
 */
public final class WeatherChangeTriggerSource implements TriggerSource {

    private String lastSeen;

    @Override
    public void register(TriggerContext context) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            String current = describe(server.overworld());
            // The first tick only records a baseline; a fresh world starts over.
            if (lastSeen != null && !lastSeen.equals(current)) {
                context.emit(GameEvent.playerless("trigger.weather_changed",
                        Map.of("weather", ParamValue.of(current))));
            }
            lastSeen = current;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> lastSeen = null);
    }

    private static String describe(ServerLevel level) {
        if (level.isThundering()) {
            return "thunder";
        }
        return level.isRaining() ? "rain" : "clear";
    }
}
