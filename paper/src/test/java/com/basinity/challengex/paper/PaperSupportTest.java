package com.basinity.challengex.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.basinity.challengex.common.support.PlatformSupport;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Definition;
import com.basinity.challengex.core.registry.Registries;
import com.basinity.challengex.core.registry.Registry;
import com.basinity.challengex.paper.effect.EffectHandlers;
import com.basinity.challengex.paper.modifier.ModifierEnforcers;
import com.basinity.challengex.paper.modifier.ModifierSources;
import com.basinity.challengex.paper.trigger.TriggerSources;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Holds the shared support declaration to what this adapter actually wires.
 *
 * <p>This is what makes Paper's entry in {@code PlatformSupport} derived rather
 * than trusted: the website and the in-game report both read that declaration,
 * and if it ever said more or less than the registration tables do, a challenge
 * would be marked runnable when it is not, or marked broken when it is fine.
 */
class PaperSupportTest {

    /**
     * Settled by the engine itself and never dispatched to an adapter, so their
     * absence from every table says nothing about support.
     */
    private static final Set<String> ENGINE_HANDLED = Set.of(
            CoreCatalog.EFFECT_WIN_CHALLENGE,
            CoreCatalog.EFFECT_LOSE_CHALLENGE,
            CoreCatalog.MODIFIER_TIME_LIMIT);

    private static Set<String> idsOf(Registry<? extends Definition> registry) {
        Set<String> ids = new TreeSet<>();
        registry.all().forEach(definition -> ids.add(definition.id()));
        return ids;
    }

    /** Everything in the catalog, across all three kinds. */
    private static Set<String> catalog() {
        Registries registries = CoreCatalog.createRegistries();
        Set<String> ids = new TreeSet<>();
        ids.addAll(idsOf(registries.triggers()));
        ids.addAll(idsOf(registries.effects()));
        ids.addAll(idsOf(registries.modifiers()));
        return ids;
    }

    /** Everything this adapter registers a source, handler, or enforcer for. */
    private static Set<String> wired() {
        Set<String> ids = new TreeSet<>(TriggerSources.byId().keySet());
        ids.addAll(EffectHandlers.byId().keySet());
        ids.addAll(ModifierEnforcers.byId().keySet());
        ids.addAll(ModifierSources.byId().keySet());
        return ids;
    }

    @Test
    @DisplayName("the declared gap is exactly what the tables leave out")
    void declarationMatchesTheTables() {
        Set<String> missing = catalog();
        missing.removeAll(wired());
        missing.removeAll(ENGINE_HANDLED);

        assertEquals(new TreeSet<>(PlatformSupport.unsupportedOn(PlatformSupport.PAPER)), missing);
    }

    @Test
    @DisplayName("nothing is wired that the catalog does not have")
    void nothingIsInvented() {
        Set<String> unknown = wired();
        unknown.removeAll(catalog());

        assertEquals(Set.of(), unknown);
    }
}
