package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.change_xp}: adjusts experience. {@code set} switches between
 * adding/subtracting (default, may be negative) and setting an absolute value
 * (clamped to zero); {@code levels} switches between experience points (default)
 * and whole levels.
 */
public final class ChangeXpHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int amount = EffectParams.integer(command, "amount", 0);
        boolean set = EffectParams.bool(command, "set", false);
        boolean levels = EffectParams.bool(command, "levels", false);
        for (ServerPlayer target : targets) {
            if (set) {
                int value = Math.max(0, amount);
                if (levels) {
                    target.setExperienceLevels(value);
                } else {
                    target.setExperiencePoints(value);
                }
            } else if (levels) {
                target.giveExperienceLevels(amount);
            } else {
                target.giveExperiencePoints(amount);
            }
        }
    }
}
