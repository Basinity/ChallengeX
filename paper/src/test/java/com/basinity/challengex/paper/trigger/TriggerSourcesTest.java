package com.basinity.challengex.paper.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.TriggerDefinition;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the trigger side honest the way {@code EffectHandlersTest} does the
 * effect side: a trigger nobody wired never fires, and nothing in the game says
 * so, since a rule using it simply sits there.
 */
class TriggerSourcesTest {

    /**
     * Catalog triggers this adapter does not source, whether still undecided or
     * deliberately cut. Empty: every catalog trigger reaches the Paper adapter.
     * Listed rather than assumed away, so a future trigger that Bukkit cannot
     * express has to be recorded here instead of quietly going missing.
     */
    private static final Set<String> NOT_YET_SOURCED = Set.of();

    private static Set<String> catalogTriggers() {
        Set<String> ids = new TreeSet<>();
        for (TriggerDefinition definition : CoreCatalog.createRegistries().triggers().all()) {
            ids.add(definition.id());
        }
        return ids;
    }

    @Test
    @DisplayName("every trigger is either wired or listed as still unsourced")
    void everyTriggerIsAccountedFor() {
        Set<String> accountedFor = new TreeSet<>(TriggerSources.byId().keySet());
        accountedFor.addAll(NOT_YET_SOURCED);

        assertEquals(catalogTriggers(), accountedFor);
    }

    @Test
    @DisplayName("no source is wired for a trigger the catalog does not have")
    void noSourceIsInvented() {
        Set<String> unknown = new TreeSet<>(TriggerSources.byId().keySet());
        unknown.removeAll(catalogTriggers());

        assertTrue(unknown.isEmpty(), "not in the catalog: " + unknown);
    }

    @Test
    @DisplayName("the unsourced list does not outlive the gap it describes")
    void unsourcedListStaysAccurate() {
        Set<String> wiredAndListed = new TreeSet<>(NOT_YET_SOURCED);
        wiredAndListed.retainAll(TriggerSources.byId().keySet());

        assertTrue(wiredAndListed.isEmpty(),
                "wired but still listed as unsourced: " + wiredAndListed);
    }
}
