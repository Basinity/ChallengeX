package com.basinity.challengex.core.model;

/**
 * A typed parameter value carried by triggers, effects, goals, and modifiers.
 * The four shapes mirror the JSON primitives a preset can hold; the registry
 * definitions declare which type each named parameter expects.
 */
public sealed interface ParamValue {

    record OfString(String value) implements ParamValue {
        public OfString {
            if (value == null) {
                throw new IllegalArgumentException("A string parameter value requires a string");
            }
        }
    }

    record OfInt(long value) implements ParamValue {
    }

    record OfDecimal(double value) implements ParamValue {
    }

    record OfBool(boolean value) implements ParamValue {
    }

    static ParamValue of(String value) {
        return new OfString(value);
    }

    static ParamValue of(long value) {
        return new OfInt(value);
    }

    static ParamValue of(double value) {
        return new OfDecimal(value);
    }

    static ParamValue of(boolean value) {
        return new OfBool(value);
    }
}
