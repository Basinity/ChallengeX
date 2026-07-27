package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * {@code trigger.projectile_shot}: a player fired a projectile, an arrow, snowball,
 * trident, or anything else thrown. The {@code projectile} parameter matches what
 * was fired.
 */
public final class ProjectileShotTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileShot(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        context().emit(GameEvent.of("trigger.projectile_shot", shooter.getName(),
                Map.of("projectile", ParamValue.of(GameIds.of(event.getEntity())))));
    }
}
