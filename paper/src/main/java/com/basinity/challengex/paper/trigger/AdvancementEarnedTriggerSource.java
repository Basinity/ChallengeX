package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * {@code trigger.advancement_earned}: a player completed a proper advancement.
 * Only the five real advancement trees count ({@code story}, {@code nether},
 * {@code end}, {@code adventure}, {@code husbandry}); recipe unlocks and other
 * hidden advancements are skipped. The {@code advancement} parameter matches the
 * completed advancement's id.
 *
 * <p>Where Fabric had to ride the criterion award and check the advancement was
 * now done, Bukkit fires only on completion, so the once-only behaviour comes
 * free and only the tree filter is left.
 */
public final class AdvancementEarnedTriggerSource extends EventTriggerSource {

    private static final Set<String> CLASSICAL_TREES =
            Set.of("story", "nether", "end", "adventure", "husbandry");

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementEarned(PlayerAdvancementDoneEvent event) {
        NamespacedKey id = event.getAdvancement().getKey();
        if (!isClassical(id)) {
            return;
        }
        context().emit(GameEvent.of("trigger.advancement_earned", event.getPlayer().getName(),
                Map.of("advancement", ParamValue.of(id.toString()))));
    }

    /** True for advancements in one of the five real trees, excluding recipes. */
    private static boolean isClassical(NamespacedKey id) {
        String path = id.getKey();
        int slash = path.indexOf('/');
        return slash > 0 && CLASSICAL_TREES.contains(path.substring(0, slash));
    }
}
