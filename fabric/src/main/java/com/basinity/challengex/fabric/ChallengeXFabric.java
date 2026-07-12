package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.fabric.command.ChallengeCommand;
import com.basinity.challengex.fabric.command.PresetStore;
import com.basinity.challengex.fabric.lifecycle.RunController;
import com.basinity.challengex.fabric.lifecycle.RunPersistenceBridge;
import com.basinity.challengex.fabric.lifecycle.RunStore;
import com.basinity.challengex.fabric.lifecycle.TimerConfig;
import com.basinity.challengex.fabric.modifier.FabricModifierContext;
import com.basinity.challengex.fabric.modifier.ModifierBridge;
import com.basinity.challengex.fabric.modifier.ModifierContext;
import com.basinity.challengex.fabric.modifier.ModifierEnforcementTickSource;
import com.basinity.challengex.fabric.modifier.ModifierSource;
import com.basinity.challengex.fabric.modifier.ModifierSources;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import com.basinity.challengex.fabric.trigger.TriggerContext;
import com.basinity.challengex.fabric.trigger.TriggerSource;
import com.basinity.challengex.fabric.trigger.TriggerSources;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fabric adapter's entrypoint. It owns the active run, registers every
 * trigger source against it, and executes the effects the engine fires back.
 *
 * <p>The active challenge starts empty; a preset imported through the
 * {@code /challenge} command tree swaps it in without a restart. Until the
 * run-lifecycle phase adds explicit start/reset, a loaded challenge simply
 * counts as active.
 */
public class ChallengeXFabric implements ModInitializer {

    public static final String MOD_ID = "challengex";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ChallengeXFabric instance;

    private ChallengeRun activeRun;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        instance = this;
        registerTriggerSources();
        registerModifierEnforcement();
        TimerConfig timerConfig = new TimerConfig(LOGGER);
        timerConfig.load();
        RunStore runStore = new RunStore(LOGGER);
        RunController runController = new RunController(() -> activeRun, timerConfig, runStore);
        runController.register();
        // Autosave and shutdown write the run through the same routine the
        // lifecycle transitions use, so run.json stays current between them.
        RunPersistenceBridge.arm(runController::save);
        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            // Resume a saved run, else start empty. A restored paused run is
            // re-frozen and a finished one is not re-announced (onRestored).
            runStore.load(startedServer).ifPresentOrElse(snapshot -> {
                activeRun = ChallengeRun.restore(snapshot, CoreCatalog.createRegistries(),
                        new FabricEffectExecutor(startedServer, LOGGER));
                runController.onRestored(startedServer, snapshot.state());
                LOGGER.info("Restored {} run at {} ticks.", snapshot.state(), snapshot.elapsedTicks());
            }, () -> loadChallenge(Challenge.empty()));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(stoppedServer -> {
            activeRun = null;
            server = null;
        });
        PresetStore presetStore = new PresetStore(LOGGER);
        presetStore.ensureDir();
        new ChallengeCommand(presetStore, runController, timerConfig).register();
        LOGGER.info("ChallengeX initialized.");
    }

    /** Swaps the active run to a fresh run of the given challenge. */
    public void loadChallenge(Challenge challenge) {
        activeRun = new ChallengeRun(challenge, CoreCatalog.createRegistries(),
                new FabricEffectExecutor(server, LOGGER));
    }

    public static ChallengeXFabric instance() {
        return instance;
    }

    /** The active run, or null before the server has started. */
    public ChallengeRun activeRun() {
        return activeRun;
    }

    private void registerTriggerSources() {
        TriggerContext context = new FabricTriggerContext(() -> activeRun);
        // Mixins reach the run through the static bridge; event and poll sources
        // take the same context directly.
        MixinTriggerBridge.arm(context);
        for (TriggerSource source : TriggerSources.all()) {
            source.register(context);
        }
    }

    private void registerModifierEnforcement() {
        ModifierContext context = new FabricModifierContext(() -> activeRun);
        // Mixins reach the active modifiers through the static bridge; the tick
        // enforcer and event-cancel sources take the same context directly.
        ModifierBridge.arm(context);
        new ModifierEnforcementTickSource().register(context);
        for (ModifierSource source : ModifierSources.all()) {
            source.register(context);
        }
    }
}
