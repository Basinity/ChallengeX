package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code effect.give_item}: gives each target one of the named item. */
public final class GiveItemHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveItemHandler.class);

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String itemId = EffectParams.string(command, "item");
        if (itemId == null) {
            LOGGER.warn("give_item is missing its item id; skipping.");
            return;
        }
        Material item = GameIds.item(itemId);
        if (item == null) {
            LOGGER.warn("Unknown item {}; skipping.", itemId);
            return;
        }
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 1));
        for (Player target : targets) {
            target.getInventory().addItem(new ItemStack(item, amount));
        }
    }
}
