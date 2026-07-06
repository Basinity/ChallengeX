package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code trigger.time_of_day}: the overworld clock reached a time of day. The
 * {@code time} parameter names the moment watched for, taking the same day,
 * noon, night, and midnight keywords the change-time effect accepts. Playerless:
 * the clock belongs to the world.
 *
 * <p>Only the times some rule configures are polled, and each fires once on
 * arrival rather than for every tick spent at the marker.
 */
public final class TimeOfDayTriggerSource implements TriggerSource {

    private static final String TRIGGER_ID = "trigger.time_of_day";
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeOfDayTriggerSource.class);

    private static final Map<String, ResourceKey<ClockTimeMarker>> MARKERS = Map.of(
            "day", ClockTimeMarkers.DAY,
            "noon", ClockTimeMarkers.NOON,
            "night", ClockTimeMarkers.NIGHT,
            "midnight", ClockTimeMarkers.MIDNIGHT);

    private final Set<String> atMarker = new HashSet<>();

    @Override
    public void register(TriggerContext context) {
        ServerTickEvents.END_SERVER_TICK.register(server -> poll(server, context));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> atMarker.clear());
    }

    private void poll(MinecraftServer server, TriggerContext context) {
        for (ParamValue configured : context.configured(TRIGGER_ID, "time")) {
            String name = TriggerParams.string(configured);
            if (name == null) {
                continue;
            }
            ResourceKey<ClockTimeMarker> marker = MARKERS.get(name.toLowerCase(Locale.ROOT));
            if (marker == null) {
                LOGGER.warn("Unknown time value {}; expected day, noon, night, or midnight.", name);
                continue;
            }
            boolean now = server.clockManager().isAtTimeMarker(overworldClock(server), marker);
            // Fire on arrival only: the marker stays true while the clock sits on it.
            if (now && atMarker.add(name)) {
                context.emit(GameEvent.playerless(TRIGGER_ID, Map.of("time", configured)));
            } else if (!now) {
                atMarker.remove(name);
            }
        }
    }

    private static Holder<WorldClock> overworldClock(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
    }
}
