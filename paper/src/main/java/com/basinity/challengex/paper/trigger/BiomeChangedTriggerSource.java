package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * {@code trigger.biome_changed}: a player walked into a different biome. The
 * {@code biome} parameter matches the biome arrived in. Biomes interleave along
 * their borders, so walking a border fires repeatedly, once per crossing.
 */
public final class BiomeChangedTriggerSource extends PlayerPollTriggerSource<String> {

    @Override
    protected String read(Player player) {
        Location at = player.getLocation();
        return player.getWorld().getBiome(at).getKey().toString();
    }

    @Override
    protected void onChange(Player player, String previous, String current, TriggerContext context) {
        context.emit(GameEvent.of("trigger.biome_changed", player.getName(),
                Map.of("biome", ParamValue.of(current))));
    }
}
