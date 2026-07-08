package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.Optional;

/**
 * The static seam Mixins query modifier state through, the modifier-side
 * mirror of {@code MixinTriggerBridge}. Mixins are woven into vanilla classes
 * and cannot hold a reference to the adapter, so they reach the active
 * challenge's modifiers through this holder, armed once at mod init with the
 * same context the tick enforcement and event-cancel sources use.
 *
 * <p>Querying before the bridge is armed, or with no run active, reports
 * nothing active, so a Mixin firing early in startup idles rather than
 * throwing into vanilla code.
 */
public final class ModifierBridge {

    private static volatile ModifierContext context;

    private ModifierBridge() {
    }

    public static void arm(ModifierContext modifierContext) {
        context = modifierContext;
    }

    public static boolean isActive(String playerId, String modifierId) {
        return find(playerId, modifierId).isPresent();
    }

    public static Optional<Modifier> find(String playerId, String modifierId) {
        ModifierContext current = context;
        return current == null ? Optional.empty() : current.find(playerId, modifierId);
    }
}
