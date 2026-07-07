package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.TriggerContext;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * A throwaway diagnostic that echoes every fired trigger to chat, the trigger
 * side of the dev harness the effect {@code fire} command is the other half of.
 * It wraps the real {@link TriggerContext} so the sources stay unaware of it and
 * production emit is untouched. Toggled by {@code /challengex-dev watch on|off}
 * and removed with the rest of the dev harness once phase 5 lands the real
 * command tree.
 *
 * <p>Without it, most triggers have nothing visible to test against until preset
 * import exists, since the only loaded rule reacts to a single trigger.
 *
 * <p>It also always announces a run reaching {@link RunOutcome#WIN} or {@link
 * RunOutcome#LOSS}, regardless of the watch toggle: nothing in the adapter reads
 * {@link ChallengeRun#outcome()} otherwise, so a goal completing (or a
 * lose-challenge effect firing) is invisible in-game until phase 6 builds the
 * real run-lifecycle handling.
 */
final class DevTriggerWatch implements TriggerContext {

    private static volatile boolean enabled;

    private final TriggerContext delegate;
    private final Supplier<MinecraftServer> server;
    private final Supplier<ChallengeRun> activeRun;

    DevTriggerWatch(TriggerContext delegate, Supplier<MinecraftServer> server, Supplier<ChallengeRun> activeRun) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.server = Objects.requireNonNull(server, "server");
        this.activeRun = Objects.requireNonNull(activeRun, "activeRun");
    }

    static void setEnabled(boolean on) {
        enabled = on;
    }

    static boolean isEnabled() {
        return enabled;
    }

    @Override
    public void emit(GameEvent event) {
        if (enabled) {
            announce(event);
        }
        RunOutcome before = outcomeNow();
        delegate.emit(event);
        RunOutcome after = outcomeNow();
        if (before == RunOutcome.ONGOING && after != RunOutcome.ONGOING) {
            announceOutcome(after);
        }
    }

    private RunOutcome outcomeNow() {
        ChallengeRun run = activeRun.get();
        return run == null ? RunOutcome.ONGOING : run.outcome();
    }

    private void announceOutcome(RunOutcome outcome) {
        MinecraftServer current = server.get();
        if (current == null) {
            return;
        }
        current.getPlayerList().broadcastSystemMessage(
                Component.literal("[outcome] " + outcome), false);
    }

    @Override
    public List<ParamValue> configured(String triggerId, String paramName) {
        return delegate.configured(triggerId, paramName);
    }

    private void announce(GameEvent event) {
        MinecraftServer current = server.get();
        if (current == null) {
            return;
        }
        String who = event.playerId().orElse("(world)");
        String context = event.context().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + describe(entry.getValue()))
                .collect(Collectors.joining(", "));
        String suffix = context.isEmpty() ? "" : " {" + context + "}";
        current.getPlayerList().broadcastSystemMessage(
                Component.literal("[trigger] " + event.triggerId() + " <- " + who + suffix), false);
    }

    private static String describe(ParamValue value) {
        return switch (value) {
            case ParamValue.OfString string -> string.value();
            case ParamValue.OfInt integer -> Long.toString(integer.value());
            case ParamValue.OfDecimal decimal -> Double.toString(decimal.value());
            case ParamValue.OfBool bool -> Boolean.toString(bool.value());
        };
    }
}
