package com.basinity.challengex.common.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Reads typed parameters out of a {@link Modifier}, falling back when a
 * parameter is absent. The modifier-side mirror of {@code EffectParams}.
 */
public final class ModifierParams {

    /** Seeds rolled for modifiers that were given none, held for the life of the activation. */
    private static final Map<Modifier, Integer> rolledSeeds = new IdentityHashMap<>();

    private ModifierParams() {
    }

    /**
     * The {@code seed} parameter every randomizer shares. An unset seed rolls a
     * fresh random one the first time this particular modifier activation asks
     * for it, then keeps it for the rest of that activation (identified by the
     * {@link Modifier} instance itself, which a preset import or dev-command
     * reload creates fresh each time), so everything randomized in one run stays
     * consistent with itself but a later activation with no seed still given
     * gets its own new mapping.
     */
    public static int seed(Modifier modifier) {
        if (has(modifier, "seed")) {
            return integer(modifier, "seed", 0);
        }
        return rolledSeeds.computeIfAbsent(modifier, ignored -> new Random().nextInt());
    }

    public static String string(Modifier modifier, String name) {
        return modifier.params().get(name) instanceof ParamValue.OfString value ? value.value() : null;
    }

    public static int integer(Modifier modifier, String name, int fallback) {
        return modifier.params().get(name) instanceof ParamValue.OfInt value ? (int) value.value() : fallback;
    }

    /** A decimal parameter, accepting a whole number written where one was expected. */
    public static double decimal(Modifier modifier, String name, double fallback) {
        ParamValue value = modifier.params().get(name);
        if (value instanceof ParamValue.OfDecimal decimal) {
            return decimal.value();
        }
        if (value instanceof ParamValue.OfInt integer) {
            return integer.value();
        }
        return fallback;
    }

    public static boolean bool(Modifier modifier, String name, boolean fallback) {
        return modifier.params().get(name) instanceof ParamValue.OfBool value ? value.value() : fallback;
    }

    /** Whether the modifier carries a value for the named parameter at all. */
    public static boolean has(Modifier modifier, String name) {
        return modifier.params().get(name) != null;
    }
}
