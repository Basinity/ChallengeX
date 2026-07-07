package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import com.basinity.challengex.fabric.trigger.TriggerContext;
import com.basinity.challengex.fabric.trigger.TriggerSource;
import com.basinity.challengex.fabric.trigger.TriggerSources;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fabric adapter's entrypoint. It owns the active run, registers every
 * trigger source against it, and executes the effects the engine fires back.
 *
 * <p>Until the run-lifecycle phase adds explicit start/reset, a loaded challenge
 * simply counts as active, and the challenge loaded is the hardcoded slice: any
 * mob killed poisons its killer.
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
        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            loadChallenge(verticalSliceChallenge());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(stoppedServer -> {
            activeRun = null;
            server = null;
        });
        registerTriggerSources();
        ChallengeXDevCommand.register();
        LOGGER.info("ChallengeX initialized.");
    }

    /** Swaps the active run to a fresh run of the given challenge. */
    void loadChallenge(Challenge challenge) {
        activeRun = new ChallengeRun(challenge, CoreCatalog.createRegistries(),
                new FabricEffectExecutor(server, LOGGER));
    }

    static ChallengeXFabric instance() {
        return instance;
    }

    private void registerTriggerSources() {
        // The dev watch wraps the real context to echo fired triggers; it taps
        // emit and leaves production dispatch untouched.
        TriggerContext context = new DevTriggerWatch(new FabricTriggerContext(() -> activeRun), () -> server,
                () -> activeRun);
        // Mixins reach the run through the static bridge; event and poll sources
        // take the same context directly.
        MixinTriggerBridge.arm(context);
        for (TriggerSource source : TriggerSources.all()) {
            source.register(context);
        }
    }

    static Challenge verticalSliceChallenge() {
        Rule mobKillPoisonsKiller = new Rule(
                TriggerSpec.of("trigger.mob_killed"),
                new EffectSpec("effect.apply_status_effect",
                        Map.of("effect", ParamValue.of("minecraft:poison"),
                                "duration", ParamValue.of(10L),
                                "amplifier", ParamValue.of(1L)),
                        Optional.of(Scope.PER_PLAYER)));
        return new Challenge(List.of(mobKillPoisonsKiller), Optional.empty(), List.of());
    }
}
