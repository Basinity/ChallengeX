package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.CatalogBounds;
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
 * {@code modifier.status_effect}: gives an infinite-duration status effect once
 * on activation, then only re-gives it on a tick where the player is found to
 * be missing it, whatever caused that (milk, death, anything else that can
 * strip an effect). This is a cheap presence check every tick rather than an
 * unconditional reapply, since {@code amplifier} is player-facing (1 = level
 * I), matching {@code effect.apply_status_effect}.
 */
public final class StatusEffectEnforcer implements ModifierEnforcer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusEffectEnforcer.class);
    private static final int DEFAULT_AMPLIFIER = 1;

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        give(player, modifier);
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        Holder<MobEffect> effect = resolve(modifier);
        if (effect != null && !player.hasEffect(effect)) {
            give(player, modifier);
        }
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        Holder<MobEffect> effect = resolve(modifier);
        if (effect != null) {
            player.removeEffect(effect);
        }
    }

    private void give(ServerPlayer player, Modifier modifier) {
        Holder<MobEffect> effect = resolve(modifier);
        if (effect == null) {
            return;
        }
        int amplifier = CatalogBounds.clampInt(modifier.modifierId(), "amplifier",
                ModifierParams.integer(modifier, "amplifier", DEFAULT_AMPLIFIER));
        player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier - 1));
    }

    private Holder<MobEffect> resolve(Modifier modifier) {
        String effectId = ModifierParams.string(modifier, "effect");
        if (effectId == null) {
            LOGGER.warn("status_effect is missing its effect id; skipping.");
            return null;
        }
        Identifier id = Identifier.tryParse(effectId);
        Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (effect == null) {
            LOGGER.warn("Unknown status effect {}; skipping.", effectId);
        }
        return effect;
    }
}
