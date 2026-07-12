package com.basinity.challengex.fabric.lifecycle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Each player's own run-clock settings, held in {@code config/challengex/config.json}
 * alongside the presets folder and keyed by player UUID: the color the clock
 * draws in, and whether they see it at all. These are personal display
 * preferences rather than host settings, so every player sets their own through
 * {@code /challenge config} with no permission gate, and one player hiding the
 * clock changes nothing for anyone else.
 *
 * <p>The file is read once at startup and rewritten whenever a player changes
 * something, so the running clock picks the change up on its next frame. A
 * player who has never changed anything is simply absent from the file and gets
 * the defaults; an entry that falls back to the defaults is dropped again on
 * write, so the file only ever holds real choices.
 */
public final class TimerPreferences {

    private static final String PLAYERS_KEY = "players";
    private static final String COLOR_KEY = "timerColor";
    private static final String HIDE_KEY = "hideTimer";

    /** What a player sees before they have changed anything. */
    private static final Prefs DEFAULTS = new Prefs(TimerColors.DEFAULT, false);

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Prefs> byPlayer = new HashMap<>();

    public TimerPreferences(Logger logger) {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("challengex").resolve("config.json");
        this.logger = logger;
    }

    /** Reads the config file. A missing or malformed file leaves every player on the defaults. */
    public void load() {
        byPlayer.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (!root.has(PLAYERS_KEY) || !root.get(PLAYERS_KEY).isJsonObject()) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(PLAYERS_KEY).entrySet()) {
                read(entry.getKey(), entry.getValue());
            }
        } catch (IOException | RuntimeException malformed) {
            logger.warn("Could not read {}; every player is on the defaults: {}", file, malformed.getMessage());
        }
    }

    /** The color this player's clock draws in. */
    public String timerColor(UUID player) {
        return prefs(player).color();
    }

    /** The color ramp this player's clock should draw in right now. */
    public int[] timerRamp(UUID player) {
        return TimerColors.ramp(timerColor(player));
    }

    /** Whether this player has hidden their clock. */
    public boolean hideTimer(UUID player) {
        return prefs(player).hidden();
    }

    /** Sets and persists this player's clock color; false when the name is not a known color. */
    public boolean setTimerColor(UUID player, String name) {
        if (!TimerColors.has(name)) {
            return false;
        }
        put(player, new Prefs(name, hideTimer(player)));
        return true;
    }

    /** Sets and persists whether this player sees their clock. */
    public void setHideTimer(UUID player, boolean hidden) {
        put(player, new Prefs(timerColor(player), hidden));
    }

    private Prefs prefs(UUID player) {
        return byPlayer.getOrDefault(player, DEFAULTS);
    }

    /** Stores a player's choice, forgetting them again once it is back to the defaults. */
    private void put(UUID player, Prefs prefs) {
        if (DEFAULTS.equals(prefs)) {
            byPlayer.remove(player);
        } else {
            byPlayer.put(player, prefs);
        }
        save();
    }

    private void read(String id, JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject entry = element.getAsJsonObject();
        UUID player;
        try {
            player = UUID.fromString(id);
        } catch (IllegalArgumentException notAUuid) {
            logger.warn("Skipping config entry '{}': not a player UUID.", id);
            return;
        }
        String color = DEFAULTS.color();
        if (entry.has(COLOR_KEY) && entry.get(COLOR_KEY).getAsJsonPrimitive().isString()) {
            String stored = entry.get(COLOR_KEY).getAsString();
            color = TimerColors.has(stored) ? stored : DEFAULTS.color();
        }
        boolean hidden = DEFAULTS.hidden();
        if (entry.has(HIDE_KEY) && entry.get(HIDE_KEY).getAsJsonPrimitive().isBoolean()) {
            hidden = entry.get(HIDE_KEY).getAsBoolean();
        }
        Prefs prefs = new Prefs(color, hidden);
        if (!DEFAULTS.equals(prefs)) {
            byPlayer.put(player, prefs);
        }
    }

    private void save() {
        JsonObject players = new JsonObject();
        byPlayer.forEach((player, prefs) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty(COLOR_KEY, prefs.color());
            entry.addProperty(HIDE_KEY, prefs.hidden());
            players.add(player.toString(), entry);
        });
        JsonObject root = new JsonObject();
        root.add(PLAYERS_KEY, players);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(root));
        } catch (IOException e) {
            logger.warn("Could not write {}: {}", file, e.getMessage());
        }
    }

    private record Prefs(String color, boolean hidden) {
    }
}
