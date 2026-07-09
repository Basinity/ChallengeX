package com.basinity.challengex.fabric.command;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The single point every admin command routes its permission check through.
 * Gating is a vanilla op-level-2 check ({@code LEVEL_GAMEMASTERS}), which needs
 * no external permission library: a challenge host is the server operator, so
 * op level is the natural gate. Keeping every command's check here means
 * swapping to named permission nodes later (should the mod ever want per-node
 * grants) is a one-file change.
 */
final class Perms {

    private Perms() {
    }

    static Predicate<CommandSourceStack> requireAdmin() {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }
}
