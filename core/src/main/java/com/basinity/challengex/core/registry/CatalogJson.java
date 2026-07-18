package com.basinity.challengex.core.registry;

import com.basinity.challengex.core.preset.PresetCodec;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * Serializes the registries into the machine-readable catalog the web builder
 * renders its forms from. The mod's code stays the single source of truth for
 * what exists and what it takes; the site never hardcodes an entry.
 *
 * <p>Structure only. Display names and blurbs are copy and live on the web
 * side, keyed by id.
 */
public final class CatalogJson {

    /** Bumped when the shape of this document changes, not when entries are added. */
    public static final int CATALOG_VERSION = 1;

    private CatalogJson() {
    }

    public static String of(Registries registries) {
        Objects.requireNonNull(registries, "registries");
        JsonObject root = new JsonObject();
        root.addProperty("catalogVersion", CATALOG_VERSION);
        root.addProperty("schemaVersion", PresetCodec.SCHEMA_VERSION);
        root.add("scopes", scopes());
        root.add("triggers", entries(registries.triggers()));
        root.add("effects", entries(registries.effects()));
        root.add("goals", entries(registries.goals()));
        root.add("modifiers", entries(registries.modifiers()));
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }

    /**
     * Which scope values each side accepts, mirroring what {@code Scope} rules
     * out in the type system: only an effect knows a triggering player, so only
     * an effect accepts {@code per_player}, and a goal carries no scope at all.
     */
    private static JsonObject scopes() {
        JsonObject scopes = new JsonObject();
        scopes.add("trigger", strings("every_player", "specific_players"));
        scopes.add("effect", strings("per_player", "every_player", "specific_players"));
        scopes.add("goal", strings());
        scopes.add("modifier", strings("every_player", "specific_players"));
        return scopes;
    }

    private static JsonArray entries(Registry<? extends Definition> registry) {
        JsonArray array = new JsonArray();
        for (Definition definition : registry.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", definition.id());
            entry.addProperty("scoped", definition.scoped());
            JsonArray params = new JsonArray();
            for (ParamSpec spec : definition.params()) {
                JsonObject param = new JsonObject();
                param.addProperty("name", spec.name());
                param.addProperty("type", spec.type().name());
                param.addProperty("required", spec.required());
                if (spec.min() != null) {
                    param.addProperty("min", spec.min());
                }
                if (spec.max() != null) {
                    param.addProperty("max", spec.max());
                }
                params.add(param);
            }
            entry.add("params", params);
            array.add(entry);
        }
        return array;
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
