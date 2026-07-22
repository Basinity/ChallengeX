package com.basinity.challengex.core.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.GoalCompletion;
import com.basinity.challengex.core.model.GoalMode;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.registry.CoreCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The seam between the mod and the web builder, tested from the mod's side.
 *
 * <p>The site and the mod are separately built artifacts in different
 * languages sharing one JSON contract, with no compiler in between to catch a
 * mismatch. These fixtures are written by the site's own export path
 * ({@code node web/test/run.js}) and committed, so this test parses real site
 * output with the real codec rather than a hand-typed imitation of it. A change
 * to the site that breaks the contract shows up here as a failing build, and a
 * change to the catalog that the site has not caught up with shows up as a
 * fixture diff.
 */
class PresetContractTest {

    private static final Path FIXTURES = Path.of("..", "web", "test", "out");

    private final PresetCodec codec = new PresetCodec(CoreCatalog.createRegistries());

    @Test
    void aChallengeComposedOnTheSiteImportsCleanly() {
        Preset preset = parse("site-export.json");

        assertEquals("Blood Sugar Rush", preset.name());
        Challenge challenge = preset.challenge();
        assertEquals(3, challenge.rules().size(), "rules");
        assertEquals(3, challenge.modifiers().size(), "modifiers");
        assertTrue(challenge.goal().isPresent(), "goal");
        assertEquals("goal.beat_game", challenge.goal().get().goalId());

        Rule first = challenge.rules().get(0);
        assertEquals("trigger.damage_taken", first.trigger().id());
        assertEquals("effect.random_effect", first.effect().id());
        assertEquals(Scope.EVERY_PLAYER, first.trigger().scope().orElseThrow());
        assertEquals(Scope.PER_PLAYER, first.effect().scope().orElseThrow());
    }

    @Test
    void theSiteWritesNumbersAsNumbersAndBooleansAsBooleans() {
        Challenge challenge = parse("site-export.json").challenge();
        Rule spawn = challenge.rules().get(1);

        assertEquals(2L, longParam(spawn, "count"), "an INT parameter arrives as a whole number");
        assertTrue(boolParam(spawn, "baby"), "a BOOL parameter arrives as a boolean");
    }

    @Test
    void aPlayerlessEntryCarriesNoScopeAndAScopedOneCarriesItsOwn() {
        Challenge challenge = parse("site-export.json").challenge();

        Rule interval = challenge.rules().get(2);
        assertTrue(interval.trigger().scope().isEmpty(),
                "trigger.fixed_interval has no player dimension, so the site must omit its scope");

        Modifier timeLimit = challenge.modifiers().get(2);
        assertEquals("modifier.time_limit", timeLimit.modifierId());
        assertTrue(timeLimit.scope().isEmpty(), "modifier.time_limit is run-level");

        Modifier keepInventory = challenge.modifiers().get(1);
        assertEquals(new Scope.SpecificPlayers(Set.of("Basinity")),
                keepInventory.scope().orElseThrow(),
                "a specific-player scope round-trips as the names it named");
    }

    @Test
    void aModifierOnlyChallengeIsAcceptedWithNoRulesAndNoGoal() {
        Challenge challenge = parse("site-export-modifier-only.json").challenge();

        assertTrue(challenge.rules().isEmpty(), "rules");
        assertTrue(challenge.goal().isEmpty(), "goal");
        assertEquals(2, challenge.modifiers().size(), "modifiers");
    }

    /**
     * The broad one: the site emitting every id it knows, so a catalog entry
     * the site can produce but the mod would reject cannot slip through.
     */
    @Test
    void everyEntryTheSiteCanEmitIsAcceptedByTheCodec() {
        Challenge challenge = parse("site-export-every-entry.json").challenge();

        Set<String> emitted = new HashSet<>();
        for (Rule rule : challenge.rules()) {
            emitted.add(rule.trigger().id());
            emitted.add(rule.effect().id());
        }
        for (Modifier modifier : challenge.modifiers()) {
            emitted.add(modifier.modifierId());
        }
        challenge.goal().ifPresent(goal -> emitted.add(goal.goalId()));

        var registries = CoreCatalog.createRegistries();
        Set<String> expected = new HashSet<>();
        expected.addAll(registries.triggers().ids());
        expected.addAll(registries.effects().ids());
        expected.addAll(registries.modifiers().ids());

        assertTrue(emitted.containsAll(expected),
                "the site did not emit every trigger, effect and modifier: missing "
                        + minus(expected, emitted));
    }

    @Test
    void theSitesGoalModesArriveAsTheModelsModes() {
        Challenge versus = parse("site-export-versus-goal.json").challenge();
        assertEquals(GoalMode.VERSUS, versus.goal().orElseThrow().mode());

        Challenge everyone = parse("site-export-everyone-goal.json").challenge();
        assertEquals(GoalMode.TOGETHER, everyone.goal().orElseThrow().mode());
        assertEquals(GoalCompletion.EVERYONE, everyone.goal().orElseThrow().completion());

        Challenge plain = parse("site-export.json").challenge();
        assertEquals(GoalMode.TOGETHER, plain.goal().orElseThrow().mode());
        assertEquals(GoalCompletion.ANYONE, plain.goal().orElseThrow().completion());
    }

    @Test
    void theRoundTripThroughTheCodecIsStable() {
        Preset preset = parse("site-export.json");
        String rewritten = codec.toJson(preset);

        try {
            Preset again = codec.fromJson(rewritten);
            assertEquals(preset.name(), again.name());
            assertEquals(preset.challenge(), again.challenge(),
                    "reading the mod's own re-serialization must give back the same challenge");
        } catch (PresetFormatException e) {
            fail("the codec rejected its own output: " + e.problems());
        }
    }

    @Test
    void aPresetFromANewerSchemaIsStillRefused() {
        String future = """
                { "schemaVersion": 99, "name": "From The Future" }
                """;
        PresetFormatException thrown = null;
        try {
            codec.fromJson(future);
        } catch (PresetFormatException e) {
            thrown = e;
        }
        assertFalse(thrown == null, "a newer schema version must be refused");
        assertTrue(thrown.problems().get(0).contains("update ChallengeX"), thrown.problems().toString());
    }

    // ---- helpers ----

    private Preset parse(String fixture) {
        Path file = FIXTURES.resolve(fixture);
        if (!Files.isRegularFile(file)) {
            fail("missing fixture " + file.toAbsolutePath()
                    + " — regenerate the site's export with: node web/test/run.js");
        }
        try {
            return codec.fromJson(Files.readString(file));
        } catch (PresetFormatException e) {
            return fail("the mod rejected a preset the site produced (" + fixture + "): "
                    + String.join("; ", e.problems()));
        } catch (IOException e) {
            return fail("could not read " + file + ": " + e.getMessage());
        }
    }

    private static long longParam(Rule rule, String name) {
        return switch (rule.effect().params().get(name)) {
            case com.basinity.challengex.core.model.ParamValue.OfInt value -> value.value();
            case null -> fail("no parameter '" + name + "'");
            default -> fail("parameter '" + name + "' is not an int");
        };
    }

    private static boolean boolParam(Rule rule, String name) {
        return switch (rule.effect().params().get(name)) {
            case com.basinity.challengex.core.model.ParamValue.OfBool value -> value.value();
            case null -> fail("no parameter '" + name + "'");
            default -> fail("parameter '" + name + "' is not a boolean");
        };
    }

    private static Set<String> minus(Set<String> all, Set<String> present) {
        Set<String> missing = new HashSet<>(all);
        missing.removeAll(present);
        return missing;
    }
}
