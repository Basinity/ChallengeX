package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.engine.RunState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Drives a run's lifecycle once a server tick and carries out the world-facing
 * side of the {@code /challengex} lifecycle commands. Each tick it advances the
 * clock while running (which can end the run on a time limit), announces a
 * finished run once (a loss also sends every player to spectator until reset
 * or import), holds players still while paused, and refreshes the action-bar
 * clock every player sees.
 *
 * <p>It holds a supplier rather than the run itself because it registers once
 * at mod init while runs come and go with the server and are swapped on import.
 */
public final class RunController {

    /** Bounds the gradient's scroll counter; a multiple of the ramp period, so its wrap is seamless. */
    private static final int ANIMATION_PERIOD_TICKS = 100_000;

    private final Supplier<ChallengeRun> activeRun;
    private final TimerPreferences preferences;
    private final RunStore runStore;
    private final PauseControl pause = new PauseControl();
    private final LossSpectator lossSpectator = new LossSpectator();
    private RunState previous = RunState.NOT_STARTED;
    private int animTick;

    public RunController(Supplier<ChallengeRun> activeRun, TimerPreferences preferences, RunStore runStore) {
        this.activeRun = activeRun;
        this.preferences = preferences;
        this.runStore = runStore;
    }

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run == null) {
            return;
        }
        if (run.state() == RunState.RUNNING) {
            // Who is in the run right now, for an everyone-completion goal;
            // a player leaving can itself complete it, so this runs each tick.
            run.updateParticipants(server.getPlayerList().getPlayers().stream()
                    .map(ServerPlayer::getScoreboardName)
                    .toList());
            run.tick(1);
        }
        RunState state = run.state();
        if (previous != RunState.FINISHED && state == RunState.FINISHED) {
            RunAnnouncer.announce(server, run.outcome(), run.elapsedTicks(), run.winner());
            if (run.outcome() == RunOutcome.LOSS) {
                lossSpectator.apply(server);
            }
            save(server);
        }
        if (state == RunState.PAUSED) {
            pause.holdPlayers(server);
        }
        animTick = (animTick + 1) % ANIMATION_PERIOD_TICKS;
        renderActionBar(server, run, state);
        previous = state;
    }

    /** Begins a not-started run. The caller has already checked it is startable. */
    public void start(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.start();
            save(server);
        }
    }

    /** Pauses a running run and freezes the world around it. */
    public void pause(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.pause();
            pause.freeze(server);
            save(server);
        }
    }

    /** Resumes a paused run and unfreezes the world. */
    public void resume(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.resume();
            pause.unfreeze(server);
            save(server);
        }
    }

    /** Rebuilds the run fresh and lifts any freeze it left behind. */
    public void reset(MinecraftServer server) {
        pause.unfreeze(server);
        lossSpectator.restore(server);
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.reset();
        }
        previous = RunState.NOT_STARTED;
        save(server);
    }

    /** A freshly imported challenge starts not-started; lift any freeze from the last run. */
    public void onChallengeReplaced(MinecraftServer server) {
        pause.unfreeze(server);
        lossSpectator.restore(server);
        previous = RunState.NOT_STARTED;
        save(server);
    }

    /**
     * Syncs the controller to a run restored from disk on server start: a paused
     * run is re-frozen (the tick-freeze itself does not persist), and marking the
     * previous state as the restored one keeps a finished run from re-announcing.
     */
    public void onRestored(MinecraftServer server, RunState state) {
        previous = state;
        if (state == RunState.PAUSED) {
            pause.freeze(server);
        }
    }

    /**
     * Persists the current run so {@code run.json} mirrors it. An empty,
     * never-imported challenge writes nothing: there is no run to resume until a
     * preset is imported.
     */
    public void save(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run == null || run.challenge().isEmpty()) {
            return;
        }
        runStore.save(server, run.snapshot());
    }

    private void renderActionBar(MinecraftServer server, ChallengeRun run, RunState state) {
        if (state != RunState.RUNNING && state != RunState.PAUSED) {
            return;
        }
        String time = RunClock.format(run.displayTicks());
        // The bar is per-player, since color and visibility are per-player
        // preferences, but most players share a color: build one packet per
        // distinct color this tick rather than one per player.
        Map<String, ClientboundSetActionBarTextPacket> byColor = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (preferences.hideTimer(id)) {
                continue;
            }
            player.connection.send(byColor.computeIfAbsent(preferences.timerColor(id),
                    color -> bar(TimerColors.ramp(color), time, state)));
        }
    }

    private ClientboundSetActionBarTextPacket bar(int[] ramp, String time, RunState state) {
        MutableComponent bar = Component.empty().append(TimerColors.gradient(ramp, time, animTick));
        if (state == RunState.PAUSED) {
            bar.append(Component.literal("  (paused)").withStyle(ChatFormatting.GRAY));
        }
        return new ClientboundSetActionBarTextPacket(bar);
    }
}
