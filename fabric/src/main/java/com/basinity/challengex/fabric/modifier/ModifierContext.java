package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import java.util.Optional;

/**
 * Fabric's read side of the modifier contract: what's currently in force for a
 * player. Unlike triggers and effects, nothing fires a modifier; a source or
 * enforcer just asks whether one is active right now.
 */
public interface ModifierContext {

    List<Modifier> activeModifiersFor(String playerId);

    default Optional<Modifier> find(String playerId, String modifierId) {
        return activeModifiersFor(playerId).stream()
                .filter(modifier -> modifier.modifierId().equals(modifierId))
                .findFirst();
    }

    /**
     * Whether a playerless modifier (one with no scope at all, in force for the
     * whole run regardless of who's asked about) is active. The engine ignores
     * the player id entirely for a playerless modifier, so any id, including
     * this empty one, resolves it correctly.
     */
    default boolean isGloballyActive(String modifierId) {
        return find("", modifierId).isPresent();
    }
}
