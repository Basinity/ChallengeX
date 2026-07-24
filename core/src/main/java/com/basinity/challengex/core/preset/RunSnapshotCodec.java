package com.basinity.challengex.core.preset;

import com.basinity.challengex.core.engine.Completion;
import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.engine.RunSnapshot;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.registry.Registries;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Reads and writes the per-world run snapshot JSON. It reuses {@link
 * PresetCodec}'s challenge read/write so a persisted run's composition faces the
 * same strict validation a preset's does, and applies that same strictness to
 * its own snapshot version: a snapshot written for a newer version is rejected
 * outright rather than misread, and any other problem rejects the whole snapshot
 * with every problem named at once.
 */
public final class RunSnapshotCodec {

    private final PresetCodec presetCodec;

    public RunSnapshotCodec(Registries registries) {
        this.presetCodec = new PresetCodec(Objects.requireNonNull(registries, "registries"));
    }

    public String toJson(RunSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshotVersion", RunSnapshot.SNAPSHOT_VERSION);
        root.addProperty("state", snapshot.state().name());
        root.addProperty("elapsedTicks", snapshot.elapsedTicks());
        root.addProperty("outcome", snapshot.outcome().name());
        if (!snapshot.completions().isEmpty()) {
            // Written in order: the order is the placing, not an accident of
            // iteration, so it is part of the format rather than sorted away.
            JsonArray completions = new JsonArray();
            for (Completion completion : snapshot.completions()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("player", completion.playerId());
                entry.addProperty("atTick", completion.atTick());
                completions.add(entry);
            }
            root.add("completions", completions);
        }
        if (!snapshot.eliminated().isEmpty()) {
            JsonArray eliminated = new JsonArray();
            snapshot.eliminated().stream().sorted().forEach(eliminated::add);
            root.add("eliminated", eliminated);
        }
        JsonObject challenge = new JsonObject();
        presetCodec.writeChallenge(challenge, snapshot.challenge());
        root.add("challenge", challenge);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    public RunSnapshot fromJson(String json) throws PresetFormatException {
        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new PresetFormatException(List.of("not valid JSON: " + e.getMessage()));
        }
        if (!rootElement.isJsonObject()) {
            throw new PresetFormatException(List.of("the run snapshot must be a JSON object"));
        }
        JsonObject root = rootElement.getAsJsonObject();

        long version = requireSnapshotVersion(root);
        if (version > RunSnapshot.SNAPSHOT_VERSION) {
            throw new PresetFormatException(List.of("written for snapshot version " + version
                    + ", this build supports up to " + RunSnapshot.SNAPSHOT_VERSION + " — update ChallengeX"));
        }
        // An older snapshot tracked goal progress, which no longer decides
        // anything. Resuming from it would restore a run whose finishing order
        // and casualties had silently vanished, so the run is dropped and the
        // world starts a fresh one instead.
        if (version < RunSnapshot.SNAPSHOT_VERSION) {
            throw new PresetFormatException(List.of("written for snapshot version " + version
                    + ", which predates per-player outcomes — this run cannot be resumed"));
        }

        List<String> problems = new ArrayList<>();
        RunState state = readEnum(root, "state", RunState.class, problems);
        RunOutcome outcome = readEnum(root, "outcome", RunOutcome.class, problems);
        long elapsedTicks = readElapsedTicks(root, problems);
        List<Completion> completions = readCompletions(root, problems);
        Set<String> eliminated = readEliminated(root, problems);
        Challenge challenge = readChallengeField(root, problems);

        if (problems.isEmpty()) {
            return new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, challenge, state,
                    elapsedTicks, outcome, completions, eliminated);
        }
        throw new PresetFormatException(problems);
    }

    private List<Completion> readCompletions(JsonObject root, List<String> problems) {
        JsonElement element = root.get("completions");
        if (element == null) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            problems.add("'completions' must be an array");
            return List.of();
        }
        List<Completion> completions = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                problems.add("each completion must be an object");
                continue;
            }
            JsonObject completion = entry.getAsJsonObject();
            String player = readPlayer(completion, problems);
            long atTick = readAtTick(completion, problems);
            if (player == null || atTick < 0) {
                continue;
            }
            if (!seen.add(player)) {
                problems.add("'" + player + "' finishes twice");
                continue;
            }
            completions.add(new Completion(player, atTick));
        }
        return completions;
    }

    private String readPlayer(JsonObject completion, List<String> problems) {
        JsonElement element = completion.get("player");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
            problems.add("a completion needs a non-blank 'player'");
            return null;
        }
        return element.getAsString();
    }

    /** @return the tick, or -1 when a problem was recorded */
    private long readAtTick(JsonObject completion, List<String> problems) {
        JsonElement element = completion.get("atTick");
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            BigDecimal number = element.getAsBigDecimal();
            if (number.stripTrailingZeros().scale() <= 0 && number.signum() >= 0) {
                try {
                    return number.longValueExact();
                } catch (ArithmeticException e) {
                    problems.add("a completion's 'atTick' is out of range");
                    return -1L;
                }
            }
        }
        problems.add("a completion's 'atTick' must be a non-negative whole number");
        return -1L;
    }

    private Set<String> readEliminated(JsonObject root, List<String> problems) {
        JsonElement element = root.get("eliminated");
        if (element == null) {
            return Set.of();
        }
        if (!element.isJsonArray()) {
            problems.add("'eliminated' must be an array");
            return Set.of();
        }
        Set<String> eliminated = new LinkedHashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()
                    || entry.getAsString().isBlank()) {
                problems.add("every eliminated entry must be a non-blank player name");
                continue;
            }
            eliminated.add(entry.getAsString());
        }
        return eliminated;
    }

    private long requireSnapshotVersion(JsonObject root) throws PresetFormatException {
        JsonElement element = root.get("snapshotVersion");
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new PresetFormatException(List.of("missing or non-numeric snapshotVersion"));
        }
        BigDecimal number = element.getAsBigDecimal();
        if (number.stripTrailingZeros().scale() > 0) {
            throw new PresetFormatException(List.of("snapshotVersion must be a whole number"));
        }
        return number.longValue();
    }

    private <E extends Enum<E>> E readEnum(JsonObject root, String key, Class<E> type,
            List<String> problems) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            problems.add("missing or non-string '" + key + "'");
            return null;
        }
        try {
            return Enum.valueOf(type, element.getAsString());
        } catch (IllegalArgumentException e) {
            problems.add("unknown " + key + " '" + element.getAsString() + "'");
            return null;
        }
    }

    private long readElapsedTicks(JsonObject root, List<String> problems) {
        JsonElement element = root.get("elapsedTicks");
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            BigDecimal number = element.getAsBigDecimal();
            if (number.stripTrailingZeros().scale() <= 0 && number.signum() >= 0) {
                try {
                    return number.longValueExact();
                } catch (ArithmeticException e) {
                    problems.add("elapsedTicks out of range");
                    return 0L;
                }
            }
        }
        problems.add("elapsedTicks must be a non-negative whole number");
        return 0L;
    }

    private Challenge readChallengeField(JsonObject root, List<String> problems) {
        JsonElement element = root.get("challenge");
        if (element == null || !element.isJsonObject()) {
            problems.add("missing or non-object 'challenge'");
            return null;
        }
        return presetCodec.readChallenge(element.getAsJsonObject(), problems);
    }
}
