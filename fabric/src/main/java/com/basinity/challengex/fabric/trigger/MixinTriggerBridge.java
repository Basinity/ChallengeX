package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;

/**
 * The static seam Mixins emit through. Mixins are woven into vanilla classes and
 * cannot hold a reference to the adapter, so they reach the active run through
 * this holder, which the adapter arms once at mod init with the same context the
 * event-based sources use.
 *
 * <p>Emitting before the bridge is armed, or with no run active, is a no-op, so
 * a Mixin firing early in startup idles rather than throwing into vanilla code.
 */
public final class MixinTriggerBridge {

    private static volatile TriggerContext context;

    private MixinTriggerBridge() {
    }

    public static void arm(TriggerContext triggerContext) {
        context = triggerContext;
    }

    public static void emit(GameEvent event) {
        TriggerContext current = context;
        if (current != null) {
            current.emit(event);
        }
    }
}
