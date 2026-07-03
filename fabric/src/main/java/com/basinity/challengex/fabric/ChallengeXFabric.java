package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fabric adapter's entrypoint. It maps vanilla server events onto the core
 * engine's abstract triggers and executes the effects the engine fires.
 *
 * <p>For the vertical slice it loads one hardcoded challenge on server start:
 * any mob killed poisons its killer. Until the run-lifecycle phase adds explicit
 * start/reset, a loaded challenge simply counts as active.
 */
public class ChallengeXFabric implements ModInitializer {

    public static final String MOD_ID = "challengex";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ChallengeRun activeRun;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                activeRun = new ChallengeRun(verticalSliceChallenge(), CoreCatalog.createRegistries(),
                        new FabricEffectExecutor(server, LOGGER)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> activeRun = null);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onEntityDeath);
        ChallengeXDevCommand.register();
        LOGGER.info("ChallengeX initialized.");
    }

    private void onEntityDeath(LivingEntity entity, DamageSource cause) {
        // Players dying is trigger.player_death, a separate trigger; mob_killed is mobs only.
        if (activeRun == null || entity instanceof Player) {
            return;
        }
        if (!(cause.getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        activeRun.handle(GameEvent.of("trigger.mob_killed", killer.getScoreboardName(),
                Map.of("mob", ParamValue.of(mobId))));
    }

    private static Challenge verticalSliceChallenge() {
        Rule mobKillPoisonsKiller = new Rule(
                TriggerSpec.of("trigger.mob_killed"),
                new EffectSpec("effect.apply_status_effect",
                        Map.of("effect", ParamValue.of("minecraft:poison"),
                                "duration", ParamValue.of(200L),
                                "amplifier", ParamValue.of(0L)),
                        Optional.of(Scope.PER_PLAYER)));
        return new Challenge(List.of(mobKillPoisonsKiller), Optional.empty(), List.of());
    }
}
