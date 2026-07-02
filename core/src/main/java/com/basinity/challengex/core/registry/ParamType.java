package com.basinity.challengex.core.registry;

import com.basinity.challengex.core.model.ParamValue;

/**
 * The declared type of a parameter. {@code DECIMAL} accepts an integer value
 * too, so a preset writing {@code 3} where {@code 3.0} is expected is not
 * rejected over a formality.
 */
public enum ParamType {
    STRING,
    INT,
    DECIMAL,
    BOOL;

    public boolean matches(ParamValue value) {
        return switch (this) {
            case STRING -> value instanceof ParamValue.OfString;
            case INT -> value instanceof ParamValue.OfInt;
            case DECIMAL -> value instanceof ParamValue.OfDecimal || value instanceof ParamValue.OfInt;
            case BOOL -> value instanceof ParamValue.OfBool;
        };
    }
}
