package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.RunSnapshot;
import com.basinity.challengex.core.preset.PresetFormatException;
import com.basinity.challengex.core.preset.RunSnapshotCodec;
import com.basinity.challengex.core.registry.CoreCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/**
 * Reads and writes the run snapshot at {@code <world>/data/challengex/run.json},
 * so a run belongs to the world it plays in and travels with a copied world
 * rather than being global to the instance like the config-folder presets.
 *
 * <p>A snapshot that cannot be read — missing, malformed, or written by a newer
 * build — yields an empty result, so the server starts with no run rather than
 * failing to boot on a bad file.
 */
public final class RunStore {

    private static final String FILE = "run.json";

    private final RunSnapshotCodec codec;
    private final Logger logger;

    public RunStore(Logger logger) {
        this.codec = new RunSnapshotCodec(CoreCatalog.createRegistries());
        this.logger = logger;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("challengex").resolve(FILE);
    }

    /** Writes the run snapshot, creating the folder if needed. */
    public void save(MinecraftServer server, RunSnapshot snapshot) {
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, codec.toJson(snapshot));
        } catch (IOException e) {
            logger.warn("Could not save run to {}: {}", path, e.getMessage());
        }
    }

    /** Reads the saved run, or empty when there is none or it cannot be read. */
    public Optional<RunSnapshot> load(MinecraftServer server) {
        Path path = file(server);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(codec.fromJson(Files.readString(path)));
        } catch (IOException e) {
            logger.warn("Could not read run from {}: {}", path, e.getMessage());
            return Optional.empty();
        } catch (PresetFormatException e) {
            logger.warn("Ignoring unreadable run snapshot at {}: {}", path,
                    String.join("; ", e.problems()));
            return Optional.empty();
        }
    }
}
