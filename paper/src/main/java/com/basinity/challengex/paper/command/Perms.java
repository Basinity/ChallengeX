package com.basinity.challengex.paper.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.function.Predicate;

/**
 * The single point every admin command routes its permission check through.
 *
 * <p>Fabric gates on a vanilla op level, since it has no permission system to
 * speak to. Bukkit servers do, and their admins expect to grant through it, so
 * the gate here is a named node declared in {@code paper-plugin.yml} with a
 * default of op: the same host gets in either way, and a server running a
 * permissions plugin can hand the node out without giving away op.
 */
final class Perms {

    static final String ADMIN = "challengex.admin";

    private Perms() {
    }

    static Predicate<CommandSourceStack> requireAdmin() {
        return source -> source.getSender().hasPermission(ADMIN);
    }
}
