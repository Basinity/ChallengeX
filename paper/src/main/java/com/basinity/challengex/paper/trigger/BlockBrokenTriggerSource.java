package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * {@code trigger.block_broken}: a player finished breaking a block. The
 * {@code block} parameter matches the block's id.
 *
 * <p>Listened for at monitor priority and ignoring cancelled events, which is
 * how Bukkit expresses the Fabric side's after-break hook: by then every plugin
 * that might have stopped the break has had its say.
 */
public final class BlockBrokenTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBroken(BlockBreakEvent event) {
        String blockId = event.getBlock().getType().getKey().toString();
        context().emit(GameEvent.of("trigger.block_broken", event.getPlayer().getName(),
                Map.of("block", ParamValue.of(blockId))));
    }
}
