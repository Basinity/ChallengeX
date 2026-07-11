package com.basinity.challengex.fabric.lifecycle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * The mod's persistent settings, held in {@code config/challengex/config.json}
 * alongside the presets folder. For now it holds one thing: the run-clock
 * color. It is read once at startup and rewritten whenever a setting changes,
 * so an edit through {@code /challenge config} lands in the file and the running
 * gradient picks it up on its next frame.
 */
public final class TimerConfig {

    private static final String COLOR_KEY = "timerColor";

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String timerColor = TimerColors.DEFAULT;

    public TimerConfig(Logger logger) {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("challengex").resolve("config.json");
        this.logger = logger;
    }

    /** Reads the config file, falling back to defaults and writing a fresh file when absent or invalid. */
    public void load() {
        if (!Files.isRegularFile(file)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.has(COLOR_KEY) && root.get(COLOR_KEY).getAsJsonPrimitive().isString()) {
                String stored = root.get(COLOR_KEY).getAsString();
                timerColor = TimerColors.has(stored) ? stored : TimerColors.DEFAULT;
            }
        } catch (IOException | RuntimeException malformed) {
            logger.warn("Could not read {}; using defaults: {}", file, malformed.getMessage());
        }
    }

    public String timerColor() {
        return timerColor;
    }

    /** The color ramp the action-bar clock should draw in right now. */
    public int[] timerRamp() {
        return TimerColors.ramp(timerColor);
    }

    /** Sets and persists the run-clock color; false when the name is not a known color. */
    public boolean setTimerColor(String name) {
        if (!TimerColors.has(name)) {
            return false;
        }
        timerColor = name;
        save();
        return true;
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty(COLOR_KEY, timerColor);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(root));
        } catch (IOException e) {
            logger.warn("Could not write {}: {}", file, e.getMessage());
        }
    }
}
