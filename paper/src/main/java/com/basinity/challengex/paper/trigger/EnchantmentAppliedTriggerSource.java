package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;

/**
 * {@code trigger.enchantment_applied}: a player enchanted an item at a table.
 * The {@code enchantment} and {@code level} parameters match what was applied.
 * An enchant that grants several enchantments at once fires once per
 * enchantment, so a rule watching for one of them still sees it.
 */
public final class EnchantmentAppliedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantmentApplied(EnchantItemEvent event) {
        String name = event.getEnchanter().getName();
        for (Map.Entry<Enchantment, Integer> applied : event.getEnchantsToAdd().entrySet()) {
            context().emit(GameEvent.of("trigger.enchantment_applied", name,
                    Map.of("enchantment", ParamValue.of(applied.getKey().getKey().toString()),
                            "level", ParamValue.of(applied.getValue().longValue()))));
        }
    }
}
