package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MenuType;

/**
 * {@code trigger.container_opened}: a player opened a container screen, a chest,
 * furnace, anvil, or any other menu. The {@code container} parameter matches the
 * menu's id.
 *
 * <p>The id comes from {@link MenuType} rather than from Bukkit's coarser
 * {@code InventoryType} enum, because {@code MenuType} is the same vanilla menu
 * registry the Fabric adapter reads. That is what makes the ids line up:
 * a preset naming {@code minecraft:generic_9x3} matches on either platform,
 * which an enum name would not have managed.
 *
 * <p>A view with no menu type behind it, such as a player's own inventory, is
 * skipped rather than reported under an invented id.
 */
public final class ContainerOpenedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerOpened(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        MenuType menu = event.getView().getMenuType();
        if (menu == null) {
            return;
        }
        context().emit(GameEvent.of("trigger.container_opened", player.getName(),
                Map.of("container", ParamValue.of(menu.getKey().toString()))));
    }
}
