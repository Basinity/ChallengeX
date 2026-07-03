package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * {@code effect.swap_inventory}: swaps each target's inventory with a random
 * other online player, from any dimension.
 */
public final class SwapInventoryHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            ServerPlayer other = Players.randomOther(target, server);
            if (other != null) {
                swap(target.getInventory(), other.getInventory());
            }
        }
    }

    private static void swap(Inventory first, Inventory second) {
        for (int slot = 0; slot < first.getContainerSize(); slot++) {
            ItemStack held = first.getItem(slot);
            first.setItem(slot, second.getItem(slot));
            second.setItem(slot, held);
        }
    }
}
