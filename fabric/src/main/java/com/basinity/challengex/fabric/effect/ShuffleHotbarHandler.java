package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** {@code effect.shuffle_hotbar}: randomly reorders each target's nine hotbar slots. */
public final class ShuffleHotbarHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int hotbarSize = Inventory.getSelectionSize();
        for (ServerPlayer target : targets) {
            Inventory inventory = target.getInventory();
            List<ItemStack> slots = new ArrayList<>(hotbarSize);
            for (int slot = 0; slot < hotbarSize; slot++) {
                slots.add(inventory.getItem(slot));
            }
            Collections.shuffle(slots);
            for (int slot = 0; slot < hotbarSize; slot++) {
                inventory.setItem(slot, slots.get(slot));
            }
        }
    }
}
