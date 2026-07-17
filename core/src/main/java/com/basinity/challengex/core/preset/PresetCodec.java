package com.basinity.challengex.core.preset;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Goal;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.ChallengeValidation;
import com.basinity.challengex.core.registry.Registries;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads and writes the preset JSON, the contract between the mod and the web
 * builder. Reading is strict: a schema version newer than this build supports
 * is rejected outright with a pointer at a mod update, and any other problem
 * (unknown ids, undeclared or mistyped parameters, malformed structure) rejects
 * the whole preset with every problem named at once. A preset is never
 * partially imported.
 *
 * <p>Scopes are written as {@code "per_player"}, {@code "every_player"}, or an
 * array of player names. There are no defaults: wherever the catalog entry
 * supports a scope the field is required, and a playerless entry (a weather
 * trigger, a world-level effect, a run-level modifier) must not carry one;
 * validation rejects either violation. Integral JSON numbers always parse as
 * ints, which decimal-typed parameters accept.
 */
public final class PresetCodec {

    public static final int SCHEMA_VERSION = 1;

    private final Registries registries;

    public PresetCodec(Registries registries) {
        this.registries = Objects.requireNonNull(registries, "registries");
    }

    public String toJson(Preset preset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("name", preset.name());
        writeChallenge(root, preset.challenge());
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    /**
     * Writes a challenge's rules, goal, and modifiers onto the given object. The
     * run-snapshot codec reuses this so a persisted run's composition is written
     * exactly as a preset's is.
     */
    void writeChallenge(JsonObject target, Challenge challenge) {
        if (!challenge.rules().isEmpty()) {
            JsonArray rules = new JsonArray();
            for (Rule rule : challenge.rules()) {
                JsonObject ruleJson = new JsonObject();
                ruleJson.add("trigger", blockJson(rule.trigger().id(), rule.trigger().params(),
                        rule.trigger().scope()));
                ruleJson.add("effect", blockJson(rule.effect().id(), rule.effect().params(),
                        rule.effect().scope()));
                rules.add(ruleJson);
            }
            target.add("rules", rules);
        }
        challenge.goal().ifPresent(goal ->
                target.add("goal", blockJson(goal.goalId(), goal.params(), Optional.empty())));
        if (!challenge.modifiers().isEmpty()) {
            JsonArray modifiers = new JsonArray();
            for (Modifier modifier : challenge.modifiers()) {
                JsonObject modifierJson = blockJson(modifier.modifierId(), modifier.params(),
                        modifier.scope());
                modifiers.add(modifierJson);
            }
            target.add("modifiers", modifiers);
        }
    }

    public Preset fromJson(String json) throws PresetFormatException {
        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new PresetFormatException(List.of("not valid JSON: " + e.getMessage()));
        }
        if (!rootElement.isJsonObject()) {
            throw new PresetFormatException(List.of("the preset must be a JSON object"));
        }
        JsonObject root = rootElement.getAsJsonObject();

        long version = requireSchemaVersion(root);
        if (version > SCHEMA_VERSION) {
            throw new PresetFormatException(List.of("written for schema version " + version
                    + ", this build supports up to " + SCHEMA_VERSION + " — update ChallengeX"));
        }

        List<String> problems = new ArrayList<>();
        String name = readName(root, problems);
        Challenge challenge = readChallenge(root, problems);

        if (problems.isEmpty()) {
            return new Preset(name, challenge);
        }
        throw new PresetFormatException(problems);
    }

    /**
     * Reads a challenge's rules, goal, and modifiers from the given object and
     * validates the whole against the registries. Every problem found is added
     * to the list; the return is non-null only when the object read cleanly, so
     * a caller trusts it exactly when it added no problems. The run-snapshot
     * codec reuses this so a persisted run's composition faces the same strict
     * validation a preset's does.
     */
    Challenge readChallenge(JsonObject source, List<String> problems) {
        int before = problems.size();
        List<Rule> rules = readRules(source, problems);
        Optional<Goal> goal = readGoal(source, problems);
        List<Modifier> modifiers = readModifiers(source, problems);
        if (problems.size() > before) {
            return null;
        }
        Challenge challenge = new Challenge(rules, goal, modifiers);
        problems.addAll(ChallengeValidation.problemsOf(challenge, registries));
        return problems.size() > before ? null : challenge;
    }

    // ---- writing helpers ----

    private JsonObject blockJson(String id, Map<String, ParamValue> params,
            Optional<? extends Scope> scope) {
        JsonObject block = new JsonObject();
        block.addProperty("id", id);
        if (!params.isEmpty()) {
            JsonObject paramsJson = new JsonObject();
            params.forEach((name, value) -> {
                switch (value) {
                    case ParamValue.OfString s -> paramsJson.addProperty(name, s.value());
                    case ParamValue.OfInt i -> paramsJson.addProperty(name, i.value());
                    case ParamValue.OfDecimal d -> paramsJson.addProperty(name, d.value());
                    case ParamValue.OfBool b -> paramsJson.addProperty(name, b.value());
                }
            });
            block.add("params", paramsJson);
        }
        scope.ifPresent(value -> block.add("scope", scopeJson(value)));
        return block;
    }

    private JsonElement scopeJson(Scope scope) {
        return switch (scope) {
            case Scope.PerPlayer ignored -> new JsonPrimitive("per_player");
            case Scope.EveryPlayer ignored -> new JsonPrimitive("every_player");
            case Scope.SpecificPlayers specific -> {
                JsonArray players = new JsonArray();
                specific.playerIds().stream().sorted().forEach(players::add);
                yield players;
            }
        };
    }

    // ---- reading helpers ----

    private long requireSchemaVersion(JsonObject root) throws PresetFormatException {
        JsonElement element = root.get("schemaVersion");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new PresetFormatException(List.of("missing or non-numeric schemaVersion"));
        }
        BigDecimal number = element.getAsBigDecimal();
        if (number.stripTrailingZeros().scale() > 0) {
            throw new PresetFormatException(List.of("schemaVersion must be a whole number"));
        }
        return number.longValue();
    }

    private String readName(JsonObject root, List<String> problems) {
        JsonElement element = root.get("name");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
            problems.add("missing or blank preset name");
            return "unnamed";
        }
        return element.getAsString();
    }

    private List<Rule> readRules(JsonObject root, List<String> problems) {
        List<Rule> rules = new ArrayList<>();
        JsonArray array = optionalArray(root, "rules", problems);
        if (array == null) {
            return rules;
        }
        for (int i = 0; i < array.size(); i++) {
            String where = "rule " + (i + 1);
            JsonObject ruleJson = asObject(array.get(i), where, problems);
            if (ruleJson == null) {
                continue;
            }
            JsonObject triggerJson = asObject(ruleJson.get("trigger"), where + " trigger", problems);
            JsonObject effectJson = asObject(ruleJson.get("effect"), where + " effect", problems);
            if (triggerJson == null || effectJson == null) {
                continue;
            }
            String triggerId = readId(triggerJson, where + " trigger", problems);
            String effectId = readId(effectJson, where + " effect", problems);
            Map<String, ParamValue> triggerParams = readParams(triggerJson, where + " trigger", problems);
            Map<String, ParamValue> effectParams = readParams(effectJson, where + " effect", problems);
            Optional<Scope> triggerScope = readScope(triggerJson, false, where + " trigger", problems);
            Optional<Scope> effectScope = readScope(effectJson, true, where + " effect", problems);
            if (triggerId == null || effectId == null || triggerScope == null || effectScope == null) {
                continue;
            }
            rules.add(new Rule(
                    new TriggerSpec(triggerId, triggerParams,
                            triggerScope.map(scope -> (Scope.Absolute) scope)),
                    new EffectSpec(effectId, effectParams, effectScope)));
        }
        return rules;
    }

    private Optional<Goal> readGoal(JsonObject root, List<String> problems) {
        JsonElement element = root.get("goal");
        if (element == null) {
            return Optional.empty();
        }
        JsonObject goalJson = asObject(element, "goal", problems);
        if (goalJson == null) {
            return Optional.empty();
        }
        String id = readId(goalJson, "goal", problems);
        Map<String, ParamValue> params = readParams(goalJson, "goal", problems);
        return id == null ? Optional.empty() : Optional.of(new Goal(id, params));
    }

    private List<Modifier> readModifiers(JsonObject root, List<String> problems) {
        List<Modifier> modifiers = new ArrayList<>();
        JsonArray array = optionalArray(root, "modifiers", problems);
        if (array == null) {
            return modifiers;
        }
        for (int i = 0; i < array.size(); i++) {
            String where = "modifier " + (i + 1);
            JsonObject modifierJson = asObject(array.get(i), where, problems);
            if (modifierJson == null) {
                continue;
            }
            String id = readId(modifierJson, where, problems);
            Map<String, ParamValue> params = readParams(modifierJson, where, problems);
            Optional<Scope> scope = readScope(modifierJson, false, where, problems);
            if (id == null || scope == null) {
                continue;
            }
            modifiers.add(new Modifier(id, params,
                    scope.map(value -> (Scope.Absolute) value)));
        }
        return modifiers;
    }

    private String readId(JsonObject block, String where, List<String> problems) {
        JsonElement element = block.get("id");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
            problems.add(where + ": missing id");
            return null;
        }
        return element.getAsString();
    }

    private Map<String, ParamValue> readParams(JsonObject block, String where, List<String> problems) {
        JsonElement element = block.get("params");
        if (element == null) {
            return Map.of();
        }
        if (!element.isJsonObject()) {
            problems.add(where + ": params must be an object");
            return Map.of();
        }
        Map<String, ParamValue> params = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            ParamValue value = parseParamValue(entry.getValue(),
                    where + " parameter '" + entry.getKey() + "'", problems);
            if (value != null) {
                params.put(entry.getKey(), value);
            }
        }
        return params;
    }

    private ParamValue parseParamValue(JsonElement element, String where, List<String> problems) {
        if (!element.isJsonPrimitive()) {
            problems.add(where + ": must be a string, number, or boolean");
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return ParamValue.of(primitive.getAsBoolean());
        }
        if (primitive.isString()) {
            return ParamValue.of(primitive.getAsString());
        }
        BigDecimal number = primitive.getAsBigDecimal();
        if (number.stripTrailingZeros().scale() <= 0) {
            try {
                return ParamValue.of(number.longValueExact());
            } catch (ArithmeticException e) {
                problems.add(where + ": number out of range");
                return null;
            }
        }
        return ParamValue.of(number.doubleValue());
    }

    /**
     * Reads the scope field's shape only; whether the entry requires or
     * forbids one is validation's call, made against the registry.
     *
     * @return the scope (empty when the field is absent), or null when a
     *     problem was recorded
     */
    private Optional<Scope> readScope(JsonObject block, boolean perPlayerAllowed,
            String where, List<String> problems) {
        JsonElement element = block.get("scope");
        if (element == null) {
            return Optional.empty();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            return switch (value) {
                case "per_player" -> {
                    if (!perPlayerAllowed) {
                        problems.add(where + ": scope 'per_player' is only valid on effects");
                        yield null;
                    }
                    yield Optional.of(Scope.PER_PLAYER);
                }
                case "every_player" -> Optional.of(Scope.EVERY_PLAYER);
                default -> {
                    problems.add(where + ": unknown scope '" + value + "'");
                    yield null;
                }
            };
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            Set<String> playerIds = new HashSet<>();
            boolean valid = !array.isEmpty();
            if (!valid) {
                problems.add(where + ": a specific-player scope needs at least one player");
            }
            for (JsonElement player : array) {
                if (!player.isJsonPrimitive() || !player.getAsJsonPrimitive().isString()
                        || player.getAsString().isBlank()) {
                    problems.add(where + ": player names must be non-blank strings");
                    valid = false;
                    break;
                }
                playerIds.add(player.getAsString());
            }
            return valid ? Optional.of(new Scope.SpecificPlayers(playerIds)) : null;
        }
        problems.add(where + ": scope must be \"per_player\", \"every_player\", or an array of player names");
        return null;
    }

    private JsonArray optionalArray(JsonObject root, String key, List<String> problems) {
        JsonElement element = root.get(key);
        if (element == null) {
            return null;
        }
        if (!element.isJsonArray()) {
            problems.add("'" + key + "' must be an array");
            return null;
        }
        return element.getAsJsonArray();
    }

    private JsonObject asObject(JsonElement element, String where, List<String> problems) {
        if (element == null) {
            problems.add(where + ": missing");
            return null;
        }
        if (!element.isJsonObject()) {
            problems.add(where + ": must be an object");
            return null;
        }
        return element.getAsJsonObject();
    }
}
