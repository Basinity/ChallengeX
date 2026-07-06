package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.GameEvent;
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
 */
final class DevTriggerWatch implements TriggerContext {

    private static volatile boolean enabled;

    private final TriggerContext delegate;
    private final Supplier<MinecraftServer> server;

    DevTriggerWatch(TriggerContext delegate, Supplier<MinecraftServer> server) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.server = Objects.requireNonNull(server, "server");
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
        delegate.emit(event);
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
