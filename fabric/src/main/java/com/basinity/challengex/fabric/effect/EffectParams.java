package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.model.ParamValue;

/**
 * Reads typed parameters out of an {@link EffectCommand}, falling back when a
 * parameter is absent. Shared by the effect handlers so each reads its own
 * parameters the same way.
 */
public final class EffectParams {

    private EffectParams() {
    }

    public static String string(EffectCommand command, String name) {
        return command.params().get(name) instanceof ParamValue.OfString value ? value.value() : null;
    }

    public static int integer(EffectCommand command, String name, int fallback) {
        return command.params().get(name) instanceof ParamValue.OfInt value ? (int) value.value() : fallback;
    }

    public static double decimal(EffectCommand command, String name, double fallback) {
        ParamValue value = command.params().get(name);
        if (value instanceof ParamValue.OfDecimal decimal) {
            return decimal.value();
        }
        if (value instanceof ParamValue.OfInt integer) {
            return integer.value();
        }
        return fallback;
    }

    public static boolean bool(EffectCommand command, String name, boolean fallback) {
        return command.params().get(name) instanceof ParamValue.OfBool value ? value.value() : fallback;
    }

    /** Whether the command carries a value for the named parameter at all. */
    public static boolean has(EffectCommand command, String name) {
        return command.params().get(name) != null;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
