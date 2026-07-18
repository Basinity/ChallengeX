package com.basinity.challengex.core.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One declared parameter of a catalog entry. Names are part of the frozen
 * preset vocabulary, so they follow the same lower_snake_case format as ids.
 *
 * <p>{@code min} and {@code max} are the value's inclusive bounds, or null for
 * an open end. They are the single source of the clamps the runtime applies and
 * the web builder enforces on its inputs, so the two can never disagree. They
 * are whole numbers even for a {@code DECIMAL} parameter, since every bound the
 * catalog needs is integer-valued.
 */
public record ParamSpec(String name, ParamType type, boolean required, Integer min, Integer max) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    public ParamSpec {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid parameter name '" + name + "': expected lower_snake_case");
        }
        Objects.requireNonNull(type, "type");
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("Parameter '" + name + "' has min " + min + " above max " + max);
        }
    }

    public static ParamSpec required(String name, ParamType type) {
        return new ParamSpec(name, type, true, null, null);
    }

    public static ParamSpec optional(String name, ParamType type) {
        return new ParamSpec(name, type, false, null, null);
    }

    /** This parameter with both bounds set, as {@code clamp(value, min, max)} does in the code. */
    public ParamSpec bounded(int min, int max) {
        return new ParamSpec(name, type, required, min, max);
    }

    /** This parameter with a lower bound only, as {@code Math.max(min, value)} does in the code. */
    public ParamSpec atLeast(int min) {
        return new ParamSpec(name, type, required, min, null);
    }

    /** Copies the list, rejecting duplicate parameter names. */
    static List<ParamSpec> uniqueNamed(List<ParamSpec> specs) {
        List<ParamSpec> copy = List.copyOf(specs);
        Set<String> seen = new HashSet<>();
        for (ParamSpec spec : copy) {
            if (!seen.add(spec.name())) {
                throw new IllegalArgumentException("Duplicate parameter name: " + spec.name());
            }
        }
        return copy;
    }
}
