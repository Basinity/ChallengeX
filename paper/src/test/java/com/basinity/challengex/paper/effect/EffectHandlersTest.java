package com.basinity.challengex.paper.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.EffectDefinition;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the adapter against the failure this map makes easy: an effect that
 * exists in the catalog, imports without complaint, and then quietly does
 * nothing because nobody wired a handler for it.
 */
class EffectHandlersTest {

    /** Settled by the engine itself and never dispatched to an adapter. */
    private static final Set<String> ENGINE_HANDLED = Set.of(
            CoreCatalog.EFFECT_WIN_CHALLENGE, CoreCatalog.EFFECT_LOSE_CHALLENGE);

    private static Set<String> dispatchedCatalogEffects() {
        Set<String> ids = new TreeSet<>();
        for (EffectDefinition definition : CoreCatalog.createRegistries().effects().all()) {
            if (!ENGINE_HANDLED.contains(definition.id())) {
                ids.add(definition.id());
            }
        }
        return ids;
    }

    @Test
    @DisplayName("every dispatched catalog effect has a handler, and none is invented")
    void handlersCoverTheCatalog() {
        Set<String> wired = new TreeSet<>(EffectHandlers.byId().keySet());

        assertEquals(dispatchedCatalogEffects(), wired);
    }

    @Test
    @DisplayName("the run-control effects stay out, since the engine settles them")
    void runControlEffectsAreNotWired() {
        Set<String> wired = EffectHandlers.byId().keySet();

        for (String id : ENGINE_HANDLED) {
            assertTrue(!wired.contains(id), id + " should not be dispatched to an adapter");
        }
    }
}
