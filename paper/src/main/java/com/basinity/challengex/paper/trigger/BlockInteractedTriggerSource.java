package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * {@code trigger.block_interacted}: a player right-clicked a block, a lever,
 * button, door, or any other interactable. The {@code block} parameter matches
 * the block interacted with; omitting it fires on any block.
 */
public final class BlockInteractedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockInteracted(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        context().emit(GameEvent.of("trigger.block_interacted", event.getPlayer().getName(),
                Map.of("block", ParamValue.of(block.getType().getKey().toString()))));
    }
}
