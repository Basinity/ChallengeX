package com.basinity.challengex.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.preset.PresetCodec;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The catalog export is what the web builder renders its forms from, so the
 * test that matters is faithfulness: every entry, every parameter, and every
 * scope flag must survive the trip unchanged. Reading the export back and
 * comparing it against the registries proves that without restating the
 * catalog, which {@link CoreCatalogTest} already pins.
 */
class CatalogJsonTest {

    private final Registries registries = CoreCatalog.createRegistries();
    private final JsonObject exported = JsonParser.parseString(CatalogJson.of(registries)).getAsJsonObject();

    @Test
    void carriesBothVersionsTheSiteNeeds() {
        assertEquals(CatalogJson.CATALOG_VERSION, exported.get("catalogVersion").getAsInt());
        assertEquals(PresetCodec.SCHEMA_VERSION, exported.get("schemaVersion").getAsInt(),
                "the site writes presets at this version, so it must come from the codec");
    }

    @Test
    void everyCategoryExportsFaithfully() {
        assertCategory("triggers", registries.triggers());
        assertCategory("effects", registries.effects());
        assertCategory("goals", registries.goals());
        assertCategory("modifiers", registries.modifiers());
    }

    @Test
    void onlyEffectsOfferPerPlayerAndGoalsOfferNothing() {
        JsonObject scopes = exported.getAsJsonObject("scopes");
        assertFalse(scopeValues(scopes, "trigger").contains("per_player"),
                "no triggering player exists yet when a trigger is evaluated");
        assertTrue(scopeValues(scopes, "effect").contains("per_player"));
        assertFalse(scopeValues(scopes, "modifier").contains("per_player"));
        assertEquals(List.of(), scopeValues(scopes, "goal"), "goals are scopeless in the MVP");
    }

    private void assertCategory(String key, Registry<? extends Definition> registry) {
        JsonArray entries = exported.getAsJsonArray(key);
        assertEquals(registry.all().size(), entries.size(), key + ": entry count");
        int index = 0;
        for (Definition definition : registry.all()) {
            JsonObject entry = entries.get(index++).getAsJsonObject();
            String where = key + " entry " + definition.id();
            assertEquals(definition.id(), entry.get("id").getAsString(), where + ": id");
            assertEquals(definition.scoped(), entry.get("scoped").getAsBoolean(), where + ": scoped");

            JsonArray params = entry.getAsJsonArray("params");
            assertEquals(definition.params().size(), params.size(), where + ": parameter count");
            for (int i = 0; i < params.size(); i++) {
                ParamSpec spec = definition.params().get(i);
                JsonObject param = params.get(i).getAsJsonObject();
                assertEquals(spec.name(), param.get("name").getAsString(), where + ": parameter name");
                assertEquals(spec.type().name(), param.get("type").getAsString(), where + ": parameter type");
                assertEquals(spec.required(), param.get("required").getAsBoolean(), where + ": parameter required");
                assertEquals(spec.min(), bound(param, "min"), where + ": parameter min");
                assertEquals(spec.max(), bound(param, "max"), where + ": parameter max");
            }
        }
    }

    /** A declared bound as an Integer, or null when the export omits it (an open end). */
    private static Integer bound(JsonObject param, String key) {
        return param.has(key) ? param.get(key).getAsInt() : null;
    }

    private static List<String> scopeValues(JsonObject scopes, String side) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : scopes.getAsJsonArray(side)) {
            values.add(element.getAsString());
        }
        return values;
    }
}
