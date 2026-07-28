package com.basinity.challengex.paper.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.common.support.PlatformSupport;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.ModifierDefinition;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Holds the two modifier tables to the catalog between them, the way the
 * trigger and effect tests do for their sides. A modifier nobody enforces is
 * inert and says nothing about it in game, so the gap has to be visible here.
 */
class ModifierCoverageTest {

    /** Read by the engine itself and never enforced per player by an adapter. */
    private static final Set<String> ENGINE_HANDLED = Set.of(CoreCatalog.MODIFIER_TIME_LIMIT);

    /**
     * Deliberately not supported on Paper. Read from the shared declaration the
     * website and the in-game report both use, rather than restated here, so
     * this test is what proves that declaration matches the adapter.
     */
    private static final Set<String> CUT = PlatformSupport.unsupportedOn(PlatformSupport.PAPER);

    private static Set<String> catalogModifiers() {
        Set<String> ids = new TreeSet<>();
        for (ModifierDefinition definition : CoreCatalog.createRegistries().modifiers().all()) {
            ids.add(definition.id());
        }
        return ids;
    }

    private static Set<String> enforced() {
        Set<String> ids = new TreeSet<>(ModifierEnforcers.byId().keySet());
        ids.addAll(ModifierSources.byId().keySet());
        return ids;
    }

    @Test
    @DisplayName("every modifier is enforced, engine-handled, or recorded as cut")
    void everyModifierIsAccountedFor() {
        Set<String> accountedFor = new TreeSet<>(enforced());
        accountedFor.addAll(ENGINE_HANDLED);
        accountedFor.addAll(CUT);

        assertEquals(catalogModifiers(), accountedFor);
    }

    @Test
    @DisplayName("nothing is wired for a modifier the catalog does not have")
    void nothingIsInvented() {
        Set<String> unknown = new TreeSet<>(enforced());
        unknown.removeAll(catalogModifiers());

        assertTrue(unknown.isEmpty(), "not in the catalog: " + unknown);
    }

    @Test
    @DisplayName("a cut modifier is not also wired, and neither is the engine's own")
    void cutAndEngineHandledStayUnwired() {
        Set<String> contradictory = new TreeSet<>(enforced());
        contradictory.retainAll(union(CUT, ENGINE_HANDLED));

        assertTrue(contradictory.isEmpty(), "wired but recorded as unsupported: " + contradictory);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> all = new TreeSet<>(first);
        all.addAll(second);
        return all;
    }
}
