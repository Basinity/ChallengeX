package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;

/**
 * Reads typed parameters out of a {@link Modifier}, falling back when a
 * parameter is absent. The modifier-side mirror of {@code EffectParams}.
 */
public final class ModifierParams {

    private ModifierParams() {
    }

    public static String string(Modifier modifier, String name) {
        return modifier.params().get(name) instanceof ParamValue.OfString value ? value.value() : null;
    }

    public static int integer(Modifier modifier, String name, int fallback) {
        return modifier.params().get(name) instanceof ParamValue.OfInt value ? (int) value.value() : fallback;
    }

    public static boolean bool(Modifier modifier, String name, boolean fallback) {
        return modifier.params().get(name) instanceof ParamValue.OfBool value ? value.value() : fallback;
    }

    /** Whether the modifier carries a value for the named parameter at all. */
    public static boolean has(Modifier modifier, String name) {
        return modifier.params().get(name) != null;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
