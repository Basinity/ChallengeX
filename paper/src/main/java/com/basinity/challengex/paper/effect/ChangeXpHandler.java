package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code effect.change_xp}: adjusts experience. {@code set} switches between
 * adding/subtracting (default, may be negative) and clearing all experience
 * then setting it to {@code amount} (clamped to zero); {@code levels} switches
 * between experience points (default) and whole levels.
 */
public final class ChangeXpHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int amount = EffectParams.integer(command, "amount", 0);
        boolean set = EffectParams.bool(command, "set", false);
        boolean levels = EffectParams.bool(command, "levels", false);
        for (Player target : targets) {
            if (set) {
                setExperience(target, Math.max(0, amount), levels);
            } else if (levels) {
                target.giveExpLevels(amount);
            } else {
                target.giveExp(amount);
            }
        }
    }

    /** Wipes the player's experience, then grants exactly {@code amount} of it. */
    private static void setExperience(Player target, int amount, boolean levels) {
        target.setTotalExperience(0);
        target.setExp(0.0f);
        target.setLevel(0);
        if (levels) {
            target.setLevel(amount);
        } else {
            target.giveExp(amount);
        }
    }
}
