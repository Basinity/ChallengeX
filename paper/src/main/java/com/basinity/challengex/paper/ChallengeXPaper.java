package com.basinity.challengex.paper;

import com.basinity.challengex.common.lifecycle.RunStore;
import com.basinity.challengex.common.lifecycle.TimerPreferences;
import com.basinity.challengex.common.modifier.ModifierContext;
import com.basinity.challengex.common.preset.PresetStore;
import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.paper.command.ChallengeCommand;
import com.basinity.challengex.paper.lifecycle.RunController;
import com.basinity.challengex.paper.modifier.ModifierEnforcementTickSource;
import com.basinity.challengex.paper.modifier.ModifierSource;
import com.basinity.challengex.paper.modifier.ModifierSources;
import com.basinity.challengex.paper.trigger.TriggerSource;
import com.basinity.challengex.paper.trigger.TriggerSources;
import java.nio.file.Path;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Paper adapter's entrypoint, the Bukkit-side counterpart of
 * {@code ChallengeXFabric}. It owns the active run, registers every trigger
 * source and modifier enforcer against it, executes the effects the engine
 * fires back, and owns the command tree.
 *
 * <p>The active challenge starts empty; a preset imported through the
 * {@code /challengex} command tree swaps it in without a restart.
 */
public class ChallengeXPaper extends JavaPlugin implements Listener {

    private static ChallengeXPaper instance;

    private final ModifierEnforcementTickSource tickSource = new ModifierEnforcementTickSource();
    private ChallengeRun activeRun;
    private RunController runController;

    @Override
    public void onEnable() {
        instance = this;

        Path configDir = getDataFolder().toPath();
        TimerPreferences preferences = new TimerPreferences(configDir.resolve("config.json"), getSLF4JLogger());
        preferences.load();
        PresetStore presetStore = new PresetStore(configDir.resolve("presets"), getSLF4JLogger());
        presetStore.ensureDir();

        RunStore runStore = new RunStore(getSLF4JLogger());
        Path worldRoot = RunController.worldRootOf(getServer());
        runController = new RunController(() -> activeRun, preferences, runStore, worldRoot);

        // Resume a saved run, else start empty. A finished one is not
        // re-announced (onRestored).
        runStore.load(worldRoot).ifPresentOrElse(snapshot -> {
            activeRun = ChallengeRun.restore(snapshot, CoreCatalog.createRegistries(),
                    new PaperEffectExecutor(getServer(), getSLF4JLogger()));
            runController.onRestored(getServer(), snapshot.state());
            getSLF4JLogger().info("Restored {} run at {} ticks.", snapshot.state(), snapshot.elapsedTicks());
        }, () -> loadChallenge(Challenge.empty()));

        registerTriggerSources();
        registerModifierEnforcement();
        runController.register(this);
        // The world's own save writes the run too, so run.json stays current
        // between the lifecycle transitions that also write it. Where Fabric
        // needs a Mixin for this, Paper has a real event.
        getServer().getPluginManager().registerEvents(this, this);
        new ChallengeCommand(presetStore, runController, preferences).register(this);

        getSLF4JLogger().info("ChallengeX initialized.");
    }

    @Override
    public void onDisable() {
        if (runController != null) {
            runController.save();
        }
        // Enforcers hold cross-player state (pooled values, who holds which
        // locked item); a reload has to start from a clean slate rather than
        // inheriting the last one's.
        tickSource.disabled();
        activeRun = null;
        runController = null;
        instance = null;
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if (runController != null) {
            runController.save();
        }
    }

    /** Swaps the active run to a fresh run of the given challenge. */
    public void loadChallenge(Challenge challenge) {
        activeRun = new ChallengeRun(challenge, CoreCatalog.createRegistries(),
                new PaperEffectExecutor(getServer(), getSLF4JLogger()));
    }

    public static ChallengeXPaper instance() {
        return instance;
    }

    /** The active run, or null before the plugin has enabled. */
    public ChallengeRun activeRun() {
        return activeRun;
    }

    private void registerTriggerSources() {
        TriggerContext context = new PaperTriggerContext(() -> activeRun);
        for (TriggerSource source : TriggerSources.byId().values()) {
            source.register(context, this);
        }
    }

    private void registerModifierEnforcement() {
        ModifierContext context = new PaperModifierContext(() -> activeRun);
        tickSource.register(context, this);
        for (ModifierSource source : ModifierSources.byId().values()) {
            source.register(context, this);
        }
    }
}
