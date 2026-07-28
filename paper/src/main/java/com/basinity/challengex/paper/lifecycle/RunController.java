package com.basinity.challengex.paper.lifecycle;

import com.basinity.challengex.common.lifecycle.RunClock;
import com.basinity.challengex.common.lifecycle.RunStore;
import com.basinity.challengex.common.lifecycle.TimerColors;
import com.basinity.challengex.common.lifecycle.TimerPreferences;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.engine.RunState;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Drives a run's lifecycle once a tick and carries out the world-facing side of
 * the {@code /challengex} lifecycle commands. Each tick it advances the clock
 * while running (which can end the run on a time limit), refreshes the roster
 * from who is still in survival or adventure, sends each player who wins or
 * loses into spectator as it happens, announces a finished run once with its
 * results, and refreshes the action-bar clock every player sees.
 *
 * <p>It holds a supplier rather than the run itself because it registers once at
 * plugin enable while runs are swapped on import.
 *
 * <p>Pausing is not here: the freeze is its own research spike on this platform
 * and has not landed yet, so the run state exists but nothing holds the world
 * still for it.
 */
public final class RunController {

    /** Bounds the gradient's scroll counter; a multiple of the ramp period, so its wrap is seamless. */
    private static final int ANIMATION_PERIOD_TICKS = 100_000;

    private final Supplier<ChallengeRun> activeRun;
    private final TimerPreferences preferences;
    private final RunStore runStore;
    private final Path worldRoot;
    private final OutcomeSpectator outcomeSpectator = new OutcomeSpectator();
    private RunState previous = RunState.NOT_STARTED;
    private int animTick;

    public RunController(Supplier<ChallengeRun> activeRun, TimerPreferences preferences,
            RunStore runStore, Path worldRoot) {
        this.activeRun = activeRun;
        this.preferences = preferences;
        this.runStore = runStore;
        this.worldRoot = worldRoot;
    }

    public void register(Plugin plugin) {
        Server server = plugin.getServer();
        server.getScheduler().runTaskTimer(plugin, () -> tick(server), 1L, 1L);
    }

    private void tick(Server server) {
        ChallengeRun run = activeRun.get();
        if (run == null) {
            return;
        }
        if (run.state() == RunState.RUNNING) {
            run.updateRoster(roster(server));
            run.tick(1);
            // Finishing or being knocked out puts a player into spectator,
            // which is what takes them out of the roster.
            outcomeSpectator.syncOutcomes(server, run);
            // That just changed who is playing, so read it again rather than
            // carrying a stale roster into the next tick: the run has to be
            // able to end on the tick its last player goes out, not a tick
            // later while they are still staring at the death screen.
            run.updateRoster(roster(server));
        }
        RunState state = run.state();
        if (previous != RunState.FINISHED && state == RunState.FINISHED) {
            RunAnnouncer.announce(server, run);
            if (run.outcome() == RunOutcome.LOSS) {
                outcomeSpectator.applyRunLoss(server);
            }
            save();
        }
        animTick = (animTick + 1) % ANIMATION_PERIOD_TICKS;
        renderActionBar(server, run, state);
        previous = state;
    }

    /**
     * Who is still playing, read off game mode: survival and adventure are in
     * the run, creative and spectator are outside it. A player who won, lost, or
     * simply switched themselves out leaves the roster, and one who switches
     * back rejoins it.
     */
    private static List<String> roster(Server server) {
        return server.getOnlinePlayers().stream()
                .filter(OutcomeSpectator::isPlaying)
                .map(Player::getName)
                .toList();
    }

    /** Begins a not-started run. The caller has already checked it is startable. */
    public void start() {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.start();
            save();
        }
    }

    /** Pauses a running run. The world itself is not frozen on this platform yet. */
    public void pause() {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.pause();
            save();
        }
    }

    /** Resumes a paused run. */
    public void resume() {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.resume();
            save();
        }
    }

    /** Rebuilds the run fresh. */
    public void reset(Server server) {
        outcomeSpectator.restore(server);
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.reset();
        }
        previous = RunState.NOT_STARTED;
        save();
    }

    /** A freshly imported challenge starts not-started. */
    public void onChallengeReplaced(Server server) {
        outcomeSpectator.restore(server);
        previous = RunState.NOT_STARTED;
        save();
    }

    /**
     * Syncs the controller to a run restored from disk on startup: marking the
     * previous state as the restored one keeps a finished run from re-announcing.
     */
    public void onRestored(RunState state) {
        previous = state;
    }

    /**
     * Persists the current run so {@code run.json} mirrors it. An empty,
     * never-imported challenge writes nothing: there is no run to resume until a
     * preset is imported.
     */
    public void save() {
        ChallengeRun run = activeRun.get();
        if (run == null || run.challenge().isEmpty()) {
            return;
        }
        runStore.save(worldRoot, run.snapshot());
    }

    private void renderActionBar(Server server, ChallengeRun run, RunState state) {
        if (state != RunState.RUNNING && state != RunState.PAUSED) {
            return;
        }
        String time = RunClock.format(run.displayTicks());
        // The bar is per-player, since color and visibility are per-player
        // preferences, but most players share a color: build one component per
        // distinct color this tick rather than one per player.
        Map<String, Component> byColor = new HashMap<>();
        for (Player player : server.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (preferences.hideTimer(id)) {
                continue;
            }
            player.sendActionBar(byColor.computeIfAbsent(preferences.timerColor(id),
                    color -> bar(TimerColors.ramp(color), time, state)));
        }
    }

    private Component bar(int[] ramp, String time, RunState state) {
        Component bar = gradient(ramp, time);
        if (state == RunState.PAUSED) {
            bar = bar.append(Component.text("  (paused)", NamedTextColor.GRAY));
        }
        return bar;
    }

    /**
     * Renders text in bold with the ramp scrolled by the current animation tick:
     * one component per character, each colored by its own sample of the ramp.
     * The ramp and the sampling are shared; building the components is not.
     */
    private Component gradient(int[] ramp, String text) {
        Component line = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            TextColor color = TextColor.color(TimerColors.colorAt(ramp, i, animTick));
            line = line.append(Component.text(String.valueOf(text.charAt(i)), color)
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        }
        return line;
    }

    /**
     * The save root the run snapshot lives under, resolved once at enable, so
     * the run travels with a copied world.
     *
     * <p>Built from the world container and the world's name rather than from
     * {@code World#getWorldFolder()}, which points at the dimension folder
     * ({@code world/dimensions/minecraft/overworld}) and would bury the
     * snapshot a level deeper than the Fabric adapter puts it. Both write
     * {@code <world>/data/challengex/run.json}, so a world carried between the
     * two finds its run either way.
     */
    public static Path worldRootOf(Server server) {
        World overworld = server.getWorlds().getFirst();
        return server.getWorldContainer().toPath().resolve(overworld.getName());
    }
}
