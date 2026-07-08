package com.basinity.challengex.fabric.modifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The modifier id to enforcer map {@code ModifierEnforcementTickSource}
 * dispatches through. Modifiers not yet wired are simply absent and stay
 * inert; enforcers land here as the building-block-library phase implements
 * them, the modifier-side mirror of {@code EffectHandlers}.
 */
public final class ModifierEnforcers {

    private ModifierEnforcers() {
    }

    public static Map<String, ModifierEnforcer> byId() {
        Map<String, ModifierEnforcer> enforcers = new HashMap<>();
        enforcers.put("modifier.disable_jump", new DisableJumpEnforcer());
        enforcers.put("modifier.status_effect", new StatusEffectEnforcer());
        enforcers.put("modifier.no_hunger_drain", new NoHungerDrainEnforcer());
        return Map.copyOf(enforcers);
    }
}
