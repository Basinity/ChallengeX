package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.apply_status_effect}: applies a status effect to each target.
 * {@code duration} is in seconds; omitting it, or a value of zero or less, makes
 * the effect infinite. {@code amplifier} is player-facing (1 = level I).
 */
public final class ApplyStatusEffectHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyStatusEffectHandler.class);
    private static final int TICKS_PER_SECOND = 20;
    // Players write the potency they see (1 = level I), which is one above the
    // zero-based amplifier the game uses, so the input is shifted down by one.
    private static final int DEFAULT_AMPLIFIER = 1;
    private static final int MIN_AMPLIFIER = 1;
    private static final int MAX_AMPLIFIER = 256;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String effectId = EffectParams.string(command, "effect");
        if (effectId == null) {
            LOGGER.warn("apply_status_effect is missing its effect id; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(effectId);
        Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (effect == null) {
            LOGGER.warn("Unknown status effect {}; skipping.", effectId);
            return;
        }
        int seconds = EffectParams.integer(command, "duration", 0);
        int duration = seconds <= 0 ? MobEffectInstance.INFINITE_DURATION : seconds * TICKS_PER_SECOND;
        int amplifier = EffectParams.clamp(EffectParams.integer(command, "amplifier", DEFAULT_AMPLIFIER),
                MIN_AMPLIFIER, MAX_AMPLIFIER);
        for (ServerPlayer target : targets) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier - 1));
        }
    }
}
