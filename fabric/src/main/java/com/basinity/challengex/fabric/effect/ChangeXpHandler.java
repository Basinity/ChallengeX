package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.change_xp}: adjusts experience. {@code set} switches between
 * adding/subtracting (default, may be negative) and clearing all experience
 * then setting it to {@code amount} (clamped to zero); {@code levels} switches
 * between experience points (default) and whole levels.
 */
public final class ChangeXpHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int amount = EffectParams.integer(command, "amount", 0);
        boolean set = EffectParams.bool(command, "set", false);
        boolean levels = EffectParams.bool(command, "levels", false);
        for (ServerPlayer target : targets) {
            if (set) {
                setExperience(target, Math.max(0, amount), levels);
            } else if (levels) {
                target.giveExperienceLevels(amount);
            } else {
                target.giveExperiencePoints(amount);
            }
        }
    }

    /** Wipes the player's experience, then grants exactly {@code amount} of it. */
    private static void setExperience(ServerPlayer target, int amount, boolean levels) {
        target.totalExperience = 0;
        target.experienceProgress = 0.0f;
        target.setExperienceLevels(0);
        if (levels) {
            target.setExperienceLevels(amount);
        } else {
            target.giveExperiencePoints(amount);
        }
    }
}
