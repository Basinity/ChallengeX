package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * {@code trigger.fixed_interval}: fires every N seconds, over and over. The
 * {@code seconds} parameter is the period watched rather than a fact about an
 * event, so the source reads the configured periods and schedules them itself.
 * Playerless: a clock ticking over is nobody's doing.
 *
 * <p>It counts real server ticks, not the world clock, so it keeps its pace
 * through a slept night or a changed time. The count runs from server start,
 * the same stand-in the loaded-challenge-is-active rule uses until the
 * run-lifecycle phase lands a real run clock to measure from.
 */
public final class FixedIntervalTriggerSource implements TriggerSource {

    private static final String TRIGGER_ID = "trigger.fixed_interval";
    private static final int TICKS_PER_SECOND = 20;
    private static final long MIN_SECONDS = 1;
    private static final long MAX_SECONDS = 24 * 60 * 60;

    private long ticks;

    @Override
    public void register(TriggerContext context) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;
            for (ParamValue configured : context.configured(TRIGGER_ID, "seconds")) {
                long period = clamp(TriggerParams.integer(configured));
                if (ticks % (period * TICKS_PER_SECOND) == 0) {
                    context.emit(GameEvent.playerless(TRIGGER_ID, Map.of("seconds", configured)));
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ticks = 0);
    }

    private static long clamp(long seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }
}
