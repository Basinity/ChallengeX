package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code effect.give_item}: gives each target one of the named item. */
public final class GiveItemHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveItemHandler.class);

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String itemId = EffectParams.string(command, "item");
        if (itemId == null) {
            LOGGER.warn("give_item is missing its item id; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(itemId);
        Holder<Item> item = id == null ? null : BuiltInRegistries.ITEM.get(id).orElse(null);
        if (item == null) {
            LOGGER.warn("Unknown item {}; skipping.", itemId);
            return;
        }
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 1));
        for (ServerPlayer target : targets) {
            target.addItem(new ItemStack(item, amount));
        }
    }
}
