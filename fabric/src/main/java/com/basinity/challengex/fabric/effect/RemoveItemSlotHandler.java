package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** {@code effect.remove_item_slot}: deletes the item in each target's selected hotbar slot. */
public final class RemoveItemSlotHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            target.getInventory().setSelectedItem(ItemStack.EMPTY);
        }
    }
}
