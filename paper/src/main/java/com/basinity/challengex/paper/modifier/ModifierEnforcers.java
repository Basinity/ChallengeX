package com.basinity.challengex.paper.modifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The modifier id to enforcer map {@link ModifierEnforcementTickSource}
 * dispatches through, for the modifiers enforced by continuous per-tick state.
 * The ones enforced by cancelling an action are in {@link ModifierSources}
 * instead, and between the two every modifier this adapter supports is listed.
 */
public final class ModifierEnforcers {

    private ModifierEnforcers() {
    }

    public static Map<String, ModifierEnforcer> byId() {
        Map<String, ModifierEnforcer> enforcers = new LinkedHashMap<>();
        enforcers.put("modifier.status_effect", new StatusEffectEnforcer());
        enforcers.put("modifier.no_hunger_drain", new NoHungerDrainEnforcer());
        enforcers.put("modifier.item_lock", new ItemLockEnforcer());
        enforcers.put("modifier.share_health", new SharedHealthEnforcer());
        enforcers.put("modifier.share_hunger", new SharedHungerEnforcer());
        enforcers.put("modifier.share_xp", new SharedXpEnforcer());
        return Map.copyOf(enforcers);
    }
}
