package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * {@code trigger.player_died}: a player died. The {@code source} parameter
 * matches the damage type that killed them ({@code minecraft:fall}); omitting it
 * fires on any death. Dying carries no built-in run meaning; a death-ends-the-run
 * challenge pairs this trigger with the lose-challenge effect like any other rule.
 */
public final class PlayerDeathTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDied(PlayerDeathEvent event) {
        DamageSource source = event.getDamageSource();
        context().emit(GameEvent.of("trigger.player_died", event.getEntity().getName(),
                Map.of("source", ParamValue.of(GameIds.of(source)))));
    }
}
