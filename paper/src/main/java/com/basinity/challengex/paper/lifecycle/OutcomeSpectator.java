package com.basinity.challengex.paper.lifecycle;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.Completion;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Puts a player into spectator the moment they win or lose, and tells them why.
 * Going to spectator is also what takes them out of the roster, since the roster
 * is a live read of game mode. A run that ends as a loss with people still
 * playing sweeps up the rest, so a time limit running out still empties the
 * world the way it always did.
 *
 * <p>The move is retried until the player is actually seen outside survival,
 * rather than assumed to have moved: they may be offline, and a respawn rebuilds
 * the player, so a single attempt could quietly fail and strand them in the run.
 * A player whose loss the engine clears drops out of the retry entirely, so a
 * deliberate return to survival is never fought.
 *
 * <p>Each player's mode at the moment they left is remembered so reset or import
 * can put them back, and a player already outside survival is left alone both
 * ways. The memory is in-process only: after a restart the restore is a no-op
 * and the host sets modes by hand.
 */
public final class OutcomeSpectator {

    private static final Title.Times TIMES =
            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500));

    private final Map<UUID, GameMode> previousModes = new HashMap<>();
    private final Set<String> moved = new HashSet<>();
    private final Set<String> waiting = new LinkedHashSet<>();
    private final Set<String> told = new HashSet<>();

    /** The game modes a player can be in and still count as playing. */
    public static boolean isPlaying(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    /**
     * Moves everybody the run has just decided an outcome for into spectator.
     * Called each tick of a running run, so a race empties one player at a time
     * as they finish rather than all at once at the end.
     */
    public void syncOutcomes(Server server, ChallengeRun run) {
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
            Player player = server.getPlayerExact(name);
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
    public void applyRunLoss(Server server) {
        for (Player player : server.getOnlinePlayers()) {
            sendOut(player);
        }
    }

    /** Returns every remembered player still online to the mode they had. */
    public void restore(Server server) {
        for (Player player : server.getOnlinePlayers()) {
            GameMode mode = previousModes.remove(player.getUniqueId());
            if (mode != null) {
                player.setGameMode(mode);
            }
        }
        previousModes.clear();
        moved.clear();
        waiting.clear();
        told.clear();
    }

    private void sendOut(Player player) {
        if (player == null || !isPlaying(player)) {
            return;
        }
        previousModes.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * Tells the player what just happened to them. Without it, finishing or
     * being knocked out reads as the game silently taking control away.
     */
    private static void announceTo(Player player, boolean won) {
        String title = won ? "You Finished" : "You Are Out";
        String line = won
                ? "You finished the challenge. The run continues for everyone still playing."
                : "You are out of the run. The run continues for everyone still playing.";
        NamedTextColor color = won ? NamedTextColor.GREEN : NamedTextColor.RED;

        player.showTitle(Title.title(Component.text(title, color), Component.empty(), TIMES));
        player.sendMessage(Component.text(line, color));
        player.playSound(player.getLocation(),
                won ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_ANVIL_LAND,
                1.0f, won ? 1.0f : 0.8f);
    }
}
