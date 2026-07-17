package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModifierEvaluationTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private Engine engineFor(Modifier... modifiers) {
        return new Engine(new Challenge(List.of(), Optional.empty(), List.of(modifiers)), registries);
    }

    @Test
    void everyPlayerModifierIsInForceForAnyone() {
        Engine engine = engineFor(Modifier.of("modifier.keep_inventory"));

        assertEquals(1, engine.activeModifiersFor("alice").size());
        assertEquals(1, engine.activeModifiersFor("bob").size());
    }

    @Test
    void targetedModifierIsInForceOnlyForItsTargets() {
        Engine engine = engineFor(new Modifier("modifier.disable_jump",
                Map.of(), Optional.of(Scope.players("alice"))));

        assertEquals(1, engine.activeModifiersFor("alice").size());
        assertEquals(List.of(), engine.activeModifiersFor("bob"));
    }

    @Test
    void playerlessModifierIsInForceRegardlessOfPlayer() {
        Engine engine = engineFor(new Modifier("modifier.time_limit",
                Map.of("minutes", ParamValue.of(30)), Optional.empty()));

        assertEquals(1, engine.activeModifiersFor("alice").size());
        assertEquals(1, engine.activeModifiersFor("bob").size());
    }

}
