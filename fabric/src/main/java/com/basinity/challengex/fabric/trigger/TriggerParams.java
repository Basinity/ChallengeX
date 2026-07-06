package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.model.ParamValue;

/**
 * Reads configured trigger parameter values, for the threshold and schedule
 * sources that watch for a value rather than report one.
 *
 * <p>These sources echo the configured {@link ParamValue} straight back as the
 * event's context rather than rebuilding one from the number they read out of
 * it. The engine matches parameters by equality, and a preset writing {@code 5}
 * where the source would emit {@code 5.0} is the same threshold but not an
 * equal value, so echoing the original is what makes the rule fire.
 */
final class TriggerParams {

    private TriggerParams() {
    }

    /** A configured number as a double, whatever JSON shape it arrived in. */
    static double decimal(ParamValue value) {
        return switch (value) {
            case ParamValue.OfDecimal decimal -> decimal.value();
            case ParamValue.OfInt integer -> integer.value();
            default -> Double.NaN;
        };
    }

    /** A configured number as a long, whatever JSON shape it arrived in. */
    static long integer(ParamValue value) {
        return switch (value) {
            case ParamValue.OfInt integer -> integer.value();
            case ParamValue.OfDecimal decimal -> (long) decimal.value();
            default -> Long.MIN_VALUE;
        };
    }

    static String string(ParamValue value) {
        return value instanceof ParamValue.OfString text ? text.value() : null;
    }
}
