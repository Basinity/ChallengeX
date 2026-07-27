package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * {@code trigger.block_placed}: a player placed a block. The {@code block}
 * parameter matches the block's id.
 *
 * <p>Where Fabric needed a Mixin into the item's place call, Bukkit has an
 * event for it.
 */
public final class BlockPlacedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent event) {
        String blockId = event.getBlockPlaced().getType().getKey().toString();
        context().emit(GameEvent.of("trigger.block_placed", event.getPlayer().getName(),
                Map.of("block", ParamValue.of(blockId))));
    }
}
