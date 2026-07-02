package com.basinity.challengex.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Holds the catalog entries of one category. Ids are stable, namespaced
 * identifiers ({@code trigger.damage_taken} style), fixed once registered and
 * never renamed afterwards: they are the durable preset vocabulary, and a
 * rename would break every shared preset with no migration path. The registry
 * enforces the format and uniqueness at registration so a malformed or
 * colliding id can never enter the catalog.
 */
public final class Registry<D extends Definition> {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    private final String prefix;
    private final Map<String, D> entries = new LinkedHashMap<>();

    /** @param category the id prefix without the dot, e.g. {@code "trigger"} */
    public Registry(String category) {
        this.prefix = category + ".";
    }

    public void register(D definition) {
        String id = definition.id();
        if (!id.startsWith(prefix) || !NAME.matcher(id.substring(prefix.length())).matches()) {
            throw new IllegalArgumentException(
                    "Invalid id '" + id + "': expected '" + prefix + "' followed by lower_snake_case");
        }
        if (entries.putIfAbsent(id, definition) != null) {
            throw new IllegalArgumentException("Duplicate id: " + id);
        }
    }

    public Optional<D> find(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public D require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown id: " + id));
    }

    public boolean contains(String id) {
        return entries.containsKey(id);
    }

    /** All registered ids, in registration order. */
    public Set<String> ids() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public Collection<D> all() {
        return Collections.unmodifiableCollection(entries.values());
    }
}
