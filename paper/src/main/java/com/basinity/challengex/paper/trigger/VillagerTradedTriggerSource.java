package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * {@code trigger.villager_traded}: a player completed a trade with a villager or
 * wandering trader.
 */
public final class VillagerTradedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerTraded(PlayerTradeEvent event) {
        context().emit(GameEvent.of("trigger.villager_traded", event.getPlayer().getName()));
    }
}
