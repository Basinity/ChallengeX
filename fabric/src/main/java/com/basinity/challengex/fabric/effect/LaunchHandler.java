package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** {@code effect.launch}: flings each target upward with the given strength. */
public final class LaunchHandler implements EffectHandler {

    private static final double DEFAULT_STRENGTH = 1.5;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        double strength = CatalogBounds.clampDouble(command.effectId(), "strength",
                EffectParams.decimal(command, "strength", DEFAULT_STRENGTH));
        for (ServerPlayer target : targets) {
            target.setDeltaMovement(0.0, strength, 0.0);
            // Forces a velocity packet so the client actually moves.
            target.hurtMarked = true;
        }
    }
}
