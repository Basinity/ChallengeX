package com.basinity.challengex.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Definition;
import com.basinity.challengex.core.registry.Registries;
import com.basinity.challengex.core.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlatformSupportTest {

    private static Set<String> catalog() {
        Registries registries = CoreCatalog.createRegistries();
        Set<String> ids = new TreeSet<>();
        for (Registry<? extends Definition> registry :
                java.util.List.of(registries.triggers(), registries.effects(), registries.modifiers())) {
            registry.all().forEach(definition -> ids.add(definition.id()));
        }
        return ids;
    }

    @Test
    @DisplayName("every id named as unsupported is a real catalog id")
    void namedIdsAreReal() {
        Set<String> unknown = new TreeSet<>(SupportJson.allNamedIds());
        unknown.removeAll(catalog());

        assertTrue(unknown.isEmpty(), "not in the catalog: " + unknown);
    }

    @Test
    @DisplayName("both platforms are offered, and a platform running everything says so with an empty set")
    void bothPlatformsAreDeclared() {
        assertEquals(Set.of(PlatformSupport.FABRIC, PlatformSupport.PAPER),
                new TreeSet<>(PlatformSupport.platforms()));
        assertTrue(PlatformSupport.unsupportedOn(PlatformSupport.FABRIC).isEmpty());
    }

    @Test
    @DisplayName("the platform order is fixed, so the generated file does not churn between builds")
    void platformOrderIsStable() {
        // It was read off an immutable map's keys, whose iteration order is
        // unspecified and salted per JVM: harmless with two platforms and a
        // rewrite of the committed file on every build with a third.
        assertEquals(java.util.List.of(PlatformSupport.FABRIC, PlatformSupport.PAPER),
                PlatformSupport.platforms());
        assertEquals(SupportJson.write(), SupportJson.write());
    }

    @Test
    @DisplayName("every declared platform has a set, and every set has a declared platform")
    void platformsAndSetsAgree() {
        PlatformSupport.platforms().forEach(name ->
                assertNotNull(PlatformSupport.unsupportedOn(name), name + " has no set"));
        assertTrue(SupportJson.write().contains("\"fabric\""), "fabric is exported");
        assertTrue(SupportJson.write().contains("\"paper\""), "paper is exported");
    }

    @Test
    @DisplayName("an unknown platform is treated as running everything rather than nothing")
    void unknownPlatformSupportsEverything() {
        // The site could ask about a platform this build predates; refusing
        // every entry would read as a broken challenge rather than an unknown.
        assertTrue(PlatformSupport.unsupportedOn("nonesuch").isEmpty());
        assertTrue(PlatformSupport.supports("nonesuch", "modifier.share_inventory"));
    }

    @Test
    @DisplayName("supports() answers per platform")
    void supportsIsPerPlatform() {
        assertFalse(PlatformSupport.supports(PlatformSupport.PAPER, "modifier.share_inventory"));
        assertTrue(PlatformSupport.supports(PlatformSupport.FABRIC, "modifier.share_inventory"));
        assertTrue(PlatformSupport.supports(PlatformSupport.PAPER, "modifier.share_health"));
    }

    @Test
    @DisplayName("a challenge reports the pieces its platform cannot run, once each")
    void unsupportedUsedByNamesEachPieceOnce() {
        Challenge challenge = new Challenge(List.of(),
                List.of(shareInventory(), shareInventory(), shareHealth()));

        assertEquals(Set.of("modifier.share_inventory"),
                PlatformSupport.unsupportedUsedBy(challenge, PlatformSupport.PAPER));
    }

    @Test
    @DisplayName("the same challenge reports nothing on a platform that runs all of it")
    void unsupportedUsedBySaysNothingWhenEverythingRuns() {
        Challenge challenge = new Challenge(List.of(),
                List.of(shareInventory(), shareHealth()));

        assertEquals(Set.of(),
                PlatformSupport.unsupportedUsedBy(challenge, PlatformSupport.FABRIC));
    }

    @Test
    @DisplayName("a challenge using nothing missing reports nothing, whatever the platform")
    void aPortableChallengeReportsNothing() {
        Challenge challenge = new Challenge(List.of(
                new Rule(TriggerSpec.of("trigger.mob_killed"),
                        new EffectSpec("effect.lightning", Map.of(), Optional.of(Scope.EVERY_PLAYER)))),
                List.of(shareHealth()));

        assertEquals(Set.of(),
                PlatformSupport.unsupportedUsedBy(challenge, PlatformSupport.PAPER));
    }

    private static Modifier shareInventory() {
        return new Modifier("modifier.share_inventory", Map.of(), Optional.of(Scope.EVERY_PLAYER));
    }

    private static Modifier shareHealth() {
        return new Modifier("modifier.share_health", Map.of(), Optional.of(Scope.EVERY_PLAYER));
    }

    @Test
    @DisplayName("the exported file is a script assigning a global, so it opens off disk")
    void exportIsAScript() {
        String written = SupportJson.write();

        assertTrue(written.contains("window.CX_SUPPORT ="), written);
        assertTrue(written.contains("\"supportVersion\": 1"), written);
        assertTrue(written.contains("modifier.share_inventory"), written);
    }
}
