package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.log.WarnOnce;
import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.common.trigger.TriggerParams;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code trigger.time_of_day}: the overworld clock reached a time of day. The
 * {@code time} parameter names the moment watched for, taking the same day,
 * noon, night, and midnight keywords the change-time effect accepts. Playerless:
 * the clock belongs to the world.
 *
 * <p>Only the times some rule configures are polled, and each fires once on
 * arrival rather than for every tick spent at the marker. Bukkit has no
 * named-marker lookup, so the markers are the vanilla tick values the
 * {@code /time set} keywords map to, the same ones the change-time effect uses.
 */
public final class TimeOfDayTriggerSource implements TriggerSource {

    private static final String TRIGGER_ID = "trigger.time_of_day";
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeOfDayTriggerSource.class);

    private static final Map<String, Long> MARKERS = Map.of(
            "day", 1000L,
            "noon", 6000L,
            "night", 13000L,
            "midnight", 18000L);

    private final Set<String> atMarker = new HashSet<>();
    // The poll runs every tick, so an unknown value would otherwise be
    // reported twenty times a second for as long as the challenge is loaded.
    private final WarnOnce warned = new WarnOnce();

    @Override
    public void register(TriggerContext context, Plugin plugin) {
        World overworld = plugin.getServer().getWorlds().getFirst();
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> poll(overworld, context), 1L, 1L);
    }

    private void poll(World overworld, TriggerContext context) {
        long time = overworld.getTime();
        for (ParamValue configured : context.configured(TRIGGER_ID, "time")) {
            String name = TriggerParams.string(configured);
            if (name == null) {
                continue;
            }
            Long marker = MARKERS.get(name.toLowerCase(Locale.ROOT));
            if (marker == null) {
                warned.warn(LOGGER, name,
                        "Unknown time value {}; expected day, noon, night, or midnight.", name);
                continue;
            }
            boolean now = time == marker;
            // Fire on arrival only, so a clock stopped on the marker by
            // /time set or a gamerule does not fire every tick it sits there.
            if (now && atMarker.add(name)) {
                context.emit(GameEvent.playerless(TRIGGER_ID, Map.of("time", configured)));
            } else if (!now) {
                atMarker.remove(name);
            }
        }
    }
}
