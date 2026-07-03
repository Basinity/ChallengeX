package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.change_time}: sets the overworld time. Accepts the keywords
 * day, noon, night, and midnight (moved to the matching clock marker) or a tick
 * number set as the clock's absolute total. Playerless: it acts on the world.
 *
 * <p>26.2 replaced the old day-time setter with a {@link ServerClockManager}
 * driving named {@link WorldClock}s, so this drives the overworld clock through
 * that manager rather than mutating a level field.
 */
public final class ChangeTimeHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTimeHandler.class);

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String value = EffectParams.string(command, "value");
        if (value == null) {
            return;
        }
        Holder<WorldClock> clock = server.registryAccess()
                .lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);
        ServerClockManager clockManager = server.clockManager();
        ResourceKey<ClockTimeMarker> marker = markerFor(value);
        if (marker != null) {
            clockManager.moveToTimeMarker(clock, marker);
            return;
        }
        try {
            long ticks = Long.parseLong(value.trim());
            clockManager.setTotalTicks(clock, Math.max(0, ticks));
        } catch (NumberFormatException notANumber) {
            LOGGER.warn("Unknown time value {}; expected day, noon, night, midnight, or a tick number.", value);
        }
    }

    private static ResourceKey<ClockTimeMarker> markerFor(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "day" -> ClockTimeMarkers.DAY;
            case "noon" -> ClockTimeMarkers.NOON;
            case "night" -> ClockTimeMarkers.NIGHT;
            case "midnight" -> ClockTimeMarkers.MIDNIGHT;
            default -> null;
        };
    }
}
