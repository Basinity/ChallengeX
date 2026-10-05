package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >=26.3
import net.minecraft.util.Prediction;

/** {@code effect.drop_held_item}: throws the selected slot's whole stack out of the hand. */
public final class DropHeldItemHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            //? if >=26.3 {
            target.drop(target.getInventory().removeFromSelected(true), false, Prediction.SERVER_ONLY);
            //?} else
            //target.drop(target.getInventory().removeFromSelected(true), false);
        }
    }
}
