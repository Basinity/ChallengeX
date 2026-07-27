package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.entity.Player;

/**
 * {@code trigger.sneaked}: a player started sneaking. It fires on the way into a
 * sneak, not once per tick spent sneaking, so holding the key is one event.
 */
public final class SneakTriggerSource extends PlayerPollTriggerSource<Boolean> {

    @Override
    protected Boolean read(Player player) {
        return player.isSneaking();
    }

    @Override
    protected void onChange(Player player, Boolean previous, Boolean current, TriggerContext context) {
        if (current) {
            context.emit(GameEvent.of("trigger.sneaked", player.getName()));
        }
    }
}
