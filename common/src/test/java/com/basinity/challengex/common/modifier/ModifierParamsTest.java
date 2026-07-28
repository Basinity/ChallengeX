package com.basinity.challengex.common.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Scope;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModifierParamsTest {

    private static Modifier randomizer(Map<String, ParamValue> params) {
        return new Modifier("modifier.randomize_block_drops", params, Optional.of(Scope.EVERY_PLAYER));
    }

    @Test
    @DisplayName("a configured seed is used as given")
    void configuredSeedWins() {
        assertEquals(42, ModifierParams.seed(randomizer(Map.of("seed", ParamValue.of(42L)))));
    }

    @Test
    @DisplayName("an unset seed holds still for the life of one activation")
    void rolledSeedIsStable() {
        Modifier modifier = randomizer(Map.of());

        int first = ModifierParams.seed(modifier);
        for (int again = 0; again < 20; again++) {
            // A randomizer that re-rolled per lookup would remap every drop and
            // stop being learnable, which is the whole point of seeding it.
            assertEquals(first, ModifierParams.seed(modifier));
        }
    }

    @Test
    @DisplayName("forgetting the rolled seeds leaves a configured one alone")
    void forgettingDoesNotDisturbConfiguredSeeds() {
        Modifier configured = randomizer(Map.of("seed", ParamValue.of(7L)));

        ModifierParams.seed(randomizer(Map.of()));
        ModifierParams.forgetRolledSeeds();

        assertEquals(7, ModifierParams.seed(configured));
    }
}
