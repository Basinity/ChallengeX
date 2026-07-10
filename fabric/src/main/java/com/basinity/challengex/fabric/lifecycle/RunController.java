package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.RunState;
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
 * side of the {@code /challenge} lifecycle commands. Each tick it advances the
 * clock while running (which can end the run on a time limit), announces a
 * finished run once, holds players still while paused, and refreshes the
 * action-bar clock every player sees.
 *
 * <p>It holds a supplier rather than the run itself because it registers once
 * at mod init while runs come and go with the server and are swapped on import.
 */
public final class RunController {

    /** Bounds the gradient's scroll counter; a multiple of the ramp period, so its wrap is seamless. */
    private static final int ANIMATION_PERIOD_TICKS = 100_000;

    private final Supplier<ChallengeRun> activeRun;
    private final TimerConfig timerConfig;
    private final PauseControl pause = new PauseControl();
    private RunState previous = RunState.NOT_STARTED;
    private int animTick;

    public RunController(Supplier<ChallengeRun> activeRun, TimerConfig timerConfig) {
        this.activeRun = activeRun;
        this.timerConfig = timerConfig;
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
            run.tick(1);
        }
        RunState state = run.state();
        if (previous != RunState.FINISHED && state == RunState.FINISHED) {
            RunAnnouncer.announce(server, run.outcome(), run.elapsedTicks());
        }
        if (state == RunState.PAUSED) {
            pause.holdPlayers(server);
        }
        animTick = (animTick + 1) % ANIMATION_PERIOD_TICKS;
        renderActionBar(server, run, state);
        previous = state;
    }

    /** Begins a not-started run. The caller has already checked it is startable. */
    public void start() {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.start();
        }
    }

    /** Pauses a running run and freezes the world around it. */
    public void pause(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.pause();
            pause.freeze(server);
        }
    }

    /** Resumes a paused run and unfreezes the world. */
    public void resume(MinecraftServer server) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.resume();
            pause.unfreeze(server);
        }
    }

    /** Rebuilds the run fresh and lifts any freeze it left behind. */
    public void reset(MinecraftServer server) {
        pause.unfreeze(server);
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.reset();
        }
        previous = RunState.NOT_STARTED;
    }

    /** A freshly imported challenge starts not-started; lift any freeze from the last run. */
    public void onChallengeReplaced(MinecraftServer server) {
        pause.unfreeze(server);
        previous = RunState.NOT_STARTED;
    }

    private void renderActionBar(MinecraftServer server, ChallengeRun run, RunState state) {
        if (state != RunState.RUNNING && state != RunState.PAUSED) {
            return;
        }
        String time = RunClock.format(run.displayTicks());
        MutableComponent bar = Component.empty()
                .append(TimerColors.gradient(timerConfig.timerRamp(), time, animTick));
        if (state == RunState.PAUSED) {
            bar.append(Component.literal("  (paused)").withStyle(ChatFormatting.GRAY));
        }
        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(bar);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }
}
