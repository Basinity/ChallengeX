package com.basinity.challengex.core.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One declared parameter of a catalog entry. Names are part of the frozen
 * preset vocabulary, so they follow the same lower_snake_case format as ids.
 */
public record ParamSpec(String name, ParamType type, boolean required) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    public ParamSpec {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid parameter name '" + name + "': expected lower_snake_case");
        }
        Objects.requireNonNull(type, "type");
    }

    public static ParamSpec required(String name, ParamType type) {
        return new ParamSpec(name, type, true);
    }

    public static ParamSpec optional(String name, ParamType type) {
        return new ParamSpec(name, type, false);
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
