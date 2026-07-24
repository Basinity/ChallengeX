package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.Completion;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;

/**
 * Puts a player into spectator the moment they win or lose, and tells them why.
 * Going to spectator is also what takes them out of the roster, since the roster
 * is a live read of game mode. A run that ends as a loss with people still
 * playing sweeps up the rest, so a time limit running out still empties the
 * world the way it always did.
 *
 * <p>The move is made at once, on the tick the outcome lands, so a run whose
 * last player has just died ends immediately rather than waiting on them to
 * click respawn. It is then retried until the player is actually seen outside
 * survival, rather than assumed to have moved: they may be offline, and a
 * respawn rebuilds the player, so a single attempt could quietly fail and
 * strand them in the run. A player whose loss the engine clears drops out of
 * the retry entirely, so a deliberate return to survival is never fought.
 *
 * <p>Each player is moved once per outcome. That is what lets somebody who was
 * eliminated switch back to survival and carry on: the tick loop will not keep
 * shoving them back, because a player whose loss the engine has since cleared
 * drops out of this memory entirely and is only acted on if they are eliminated
 * again. A win is permanent, so a winner is never re-sent and may rejoin freely.
 *
 * <p>Each player's mode at the moment they left is remembered so reset or import
 * can put them back, and a player already outside survival is left alone both
 * ways. The memory is in-process only, as it was before per-player outcomes:
 * after a server restart the restore is a no-op and the host sets modes by hand.
 */
public final class OutcomeSpectator {

    private final Map<UUID, GameType> previousModes = new HashMap<>();
    private final Set<String> moved = new HashSet<>();
    private final Set<String> waiting = new LinkedHashSet<>();
    private final Set<String> told = new HashSet<>();

    /** The game modes a player can be in and still count as playing. */
    static boolean isPlaying(ServerPlayer player) {
        return player.gameMode() == GameType.SURVIVAL || player.gameMode() == GameType.ADVENTURE;
    }

    /**
     * Moves everybody the run has just decided an outcome for into spectator.
     * Called each tick of a running run, so a race empties one player at a time
     * as they finish rather than all at once at the end.
     */
    public void syncOutcomes(MinecraftServer server, ChallengeRun run) {
        Set<String> finished = new LinkedHashSet<>();
        for (Completion completion : run.completions()) {
            finished.add(completion.playerId());
        }
        Set<String> out = new LinkedHashSet<>(finished);
        out.addAll(run.eliminated());

        for (String name : out) {
            if (!moved.contains(name)) {
                waiting.add(name);
            }
        }
        // Anyone the engine no longer counts as out is back in play, so forget
        // them: a later outcome has to be able to move them afresh.
        waiting.retainAll(out);
        moved.retainAll(out);
        told.retainAll(out);

        waiting.removeIf(name -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(name);
            if (player == null) {
                // Offline: nothing to move, and nothing to retry against until
                // they come back. They are outside the roster meanwhile anyway.
                return false;
            }
            if (!isPlaying(player)) {
                moved.add(name);
                told.remove(name);
                return true;
            }
            if (told.add(name)) {
                announceTo(player, finished.contains(name));
            }
            sendOut(player);
            return false;
        });
    }

    /**
     * Sweeps every remaining player into spectator when the run itself is lost.
     * A run-level loss (a time limit expiring) leaves people still playing, and
     * they have lost too. No per-player message here: the run announcement has
     * already told everyone.
     */
    public void applyRunLoss(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendOut(player);
        }
    }

    /** Returns every remembered player still online to the mode they had. */
    public void restore(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GameType mode = previousModes.remove(player.getUUID());
            if (mode != null) {
                player.setGameMode(mode);
            }
        }
        previousModes.clear();
        moved.clear();
        waiting.clear();
        told.clear();
    }

    private void sendOut(ServerPlayer player) {
        if (player == null || !isPlaying(player)) {
            return;
        }
        previousModes.putIfAbsent(player.getUUID(), player.gameMode());
        player.setGameMode(GameType.SPECTATOR);
    }

    /**
     * Tells the player what just happened to them. Without it, finishing or
     * being knocked out reads as the game silently taking control away.
     */
    private void announceTo(ServerPlayer player, boolean won) {
        String title = won ? "You Finished" : "You Are Out";
        String line = won
                ? "You finished the challenge. The run continues for everyone still playing."
                : "You are out of the run. The run continues for everyone still playing.";
        ChatFormatting color = won ? ChatFormatting.GREEN : ChatFormatting.RED;

        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(title).withStyle(color)));
        player.sendSystemMessage(Component.literal(line).withStyle(color));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                won ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ANVIL_LAND,
                SoundSource.MASTER, 1.0f, won ? 1.0f : 0.8f);
    }
}
