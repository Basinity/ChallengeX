package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** {@code effect.replace_held_random}: swaps each target's held item for a random one. */
public final class ReplaceHeldRandomHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            BuiltInRegistries.ITEM.getRandom(target.getRandom())
                    .ifPresent(item -> target.getInventory().setSelectedItem(new ItemStack(item)));
        }
    }
}
