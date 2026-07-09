package com.basinity.challengex.fabric.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Locates and reads preset JSON files from the mod's own config folder, the
 * single import source. The mod never fetches a preset over the network; a
 * preset arrives here by the host dropping the file the website exported into
 * {@code config/challengex/presets}. Reading only lists and loads {@code .json}
 * files by bare name and refuses any name that reaches outside the folder.
 */
public final class PresetStore {

    private static final String EXTENSION = ".json";

    private final Path dir;
    private final Logger logger;

    public PresetStore(Logger logger) {
        this.dir = FabricLoader.getInstance().getConfigDir().resolve("challengex").resolve("presets");
        this.logger = logger;
    }

    /** Creates the presets folder if it is missing, so the host has somewhere to drop files. */
    public void ensureDir() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.warn("Could not create presets folder {}: {}", dir, e.getMessage());
        }
    }

    /** The bare names (no {@code .json}) of the preset files present, sorted. */
    public List<String> listPresetNames() {
        ensureDir();
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            logger.warn("Could not list presets in {}: {}", dir, e.getMessage());
            return List.of();
        }
    }

    /**
     * Reads the raw JSON of the named preset, or empty when no such file exists
     * or the name is unsafe. A trailing {@code .json} on the name is tolerated.
     */
    public Optional<String> read(String rawName) {
        String name = rawName.endsWith(EXTENSION)
                ? rawName.substring(0, rawName.length() - EXTENSION.length())
                : rawName;
        if (!isSafeName(name)) {
            return Optional.empty();
        }
        Path file = dir.resolve(name + EXTENSION);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            logger.warn("Could not read preset {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    /** The absolute folder path, for showing the host where presets live. */
    public String displayPath() {
        return dir.toAbsolutePath().toString();
    }

    private static boolean isSafeName(String name) {
        return !name.isBlank()
                && name.indexOf('/') < 0
                && name.indexOf('\\') < 0
                && !name.contains("..");
    }
}
