package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.biome_changed}: a player walked into a different biome. The
 * {@code biome} parameter matches the biome arrived in. Biomes interleave along
 * their borders, so walking a border fires repeatedly, once per crossing.
 */
public final class BiomeChangedTriggerSource extends PlayerPollTriggerSource<String> {

    private static final String UNKNOWN = "";

    @Override
    protected String read(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(UNKNOWN);
    }

    @Override
    protected void onChange(ServerPlayer player, String previous, String current, TriggerContext context) {
        if (current.equals(UNKNOWN)) {
            return;
        }
        context.emit(GameEvent.of("trigger.biome_changed", player.getScoreboardName(),
                Map.of("biome", ParamValue.of(current))));
    }
}
