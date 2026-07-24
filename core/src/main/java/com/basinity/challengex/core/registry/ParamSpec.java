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
 *
 * <p>{@code suggests} names the suggestion source the web builder offers for a
 * STRING parameter whose values are game ids or fixed keywords ("item",
 * "mob", "weather", ...), or is null for free text. It is builder metadata
 * only: suggestions never restrict what a preset may carry, so a modded or
 * unknown id stays as legal as it always was, and the preset codec ignores
 * the field entirely.
 *
 * <p>{@code allowed} is the opposite: the closed set of values a STRING
 * parameter may carry, or null when anything goes. It exists for parameters
 * whose value changes what the run does rather than which game object it
 * names, where falling back to a default on a typo would silently change the
 * outcome instead of reporting a problem. Unlike {@code suggests}, validation
 * enforces it and a preset carrying anything else is rejected.
 */
public record ParamSpec(String name, ParamType type, boolean required, Integer min, Integer max,
        String suggests, Set<String> allowed) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    public ParamSpec {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid parameter name '" + name + "': expected lower_snake_case");
        }
        Objects.requireNonNull(type, "type");
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("Parameter '" + name + "' has min " + min + " above max " + max);
        }
        if (suggests != null && !NAME.matcher(suggests).matches()) {
            throw new IllegalArgumentException("Invalid suggestion source '" + suggests + "': expected lower_snake_case");
        }
        if (allowed != null) {
            if (type != ParamType.STRING) {
                throw new IllegalArgumentException("Parameter '" + name + "' restricts values but is not a STRING");
            }
            allowed = Set.copyOf(allowed);
            if (allowed.isEmpty()) {
                throw new IllegalArgumentException("Parameter '" + name + "' allows no values at all");
            }
        }
    }

    public static ParamSpec required(String name, ParamType type) {
        return new ParamSpec(name, type, true, null, null, null, null);
    }

    public static ParamSpec optional(String name, ParamType type) {
        return new ParamSpec(name, type, false, null, null, null, null);
    }

    /** This parameter with both bounds set, as {@code clamp(value, min, max)} does in the code. */
    public ParamSpec bounded(int min, int max) {
        return new ParamSpec(name, type, required, min, max, suggests, allowed);
    }

    /** This parameter with a lower bound only, as {@code Math.max(min, value)} does in the code. */
    public ParamSpec atLeast(int min) {
        return new ParamSpec(name, type, required, min, null, suggests, allowed);
    }

    /** This parameter with a suggestion source the web builder offers values from. */
    public ParamSpec suggesting(String source) {
        return new ParamSpec(name, type, required, min, max, source, allowed);
    }

    /** This parameter restricted to a closed set of values, enforced by validation. */
    public ParamSpec oneOf(String... values) {
        return new ParamSpec(name, type, required, min, max, suggests, Set.of(values));
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
