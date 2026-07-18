package com.basinity.challengex.core.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Clamps a parameter value to the bounds its {@link ParamSpec} declares. The
 * bounds live in {@link CoreCatalog} alone, so the runtime and the web builder
 * enforce one set of numbers that cannot drift apart: a handler passes the raw
 * value through here instead of repeating a min and max of its own.
 *
 * <p>An unknown entry, an unknown parameter, or an open bound leaves the value
 * untouched, so passing a parameter the catalog does not bound is a no-op.
 */
public final class CatalogBounds {

    private static final Map<String, Definition> BY_ID = index();

    private CatalogBounds() {
    }

    public static int clampInt(String entryId, String param, int value) {
        return (int) clampDouble(entryId, param, value);
    }

    public static long clampLong(String entryId, String param, long value) {
        return (long) clampDouble(entryId, param, value);
    }

    public static double clampDouble(String entryId, String param, double value) {
        ParamSpec spec = spec(entryId, param);
        if (spec == null) {
            return value;
        }
        if (spec.min() != null && value < spec.min()) {
            value = spec.min();
        }
        if (spec.max() != null && value > spec.max()) {
            value = spec.max();
        }
        return value;
    }

    private static ParamSpec spec(String entryId, String param) {
        Definition definition = BY_ID.get(entryId);
        return definition == null ? null : definition.param(param).orElse(null);
    }

    private static Map<String, Definition> index() {
        Registries registries = CoreCatalog.createRegistries();
        Map<String, Definition> byId = new HashMap<>();
        addAll(byId, registries.triggers());
        addAll(byId, registries.effects());
        addAll(byId, registries.goals());
        addAll(byId, registries.modifiers());
        return Map.copyOf(byId);
    }

    private static void addAll(Map<String, Definition> byId, Registry<? extends Definition> registry) {
        for (Definition definition : registry.all()) {
            byId.put(definition.id(), definition);
        }
    }
}
