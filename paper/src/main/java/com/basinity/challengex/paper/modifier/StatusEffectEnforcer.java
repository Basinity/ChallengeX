package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.CatalogBounds;
import com.basinity.challengex.paper.effect.GameIds;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code modifier.status_effect}: gives an infinite-duration status effect once
 * on activation, then only re-gives it on a tick where the player is found to
 * be missing it, whatever caused that (milk, death, anything else that can
 * strip an effect). This is a cheap presence check every tick rather than an
 * unconditional reapply. {@code amplifier} is player-facing (1 = level I),
 * matching {@code effect.apply_status_effect}.
 */
public final class StatusEffectEnforcer implements ModifierEnforcer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusEffectEnforcer.class);
    private static final int DEFAULT_AMPLIFIER = 1;

    @Override
    public void start(Player player, Modifier modifier, Server server) {
        give(player, modifier);
    }

    @Override
    public void tick(Player player, Modifier modifier, Server server) {
        PotionEffectType effect = resolve(modifier);
        if (effect != null && !player.hasPotionEffect(effect)) {
            give(player, modifier);
        }
    }

    @Override
    public void stop(Player player, Modifier modifier, Server server) {
        PotionEffectType effect = resolve(modifier);
        if (effect != null) {
            player.removePotionEffect(effect);
        }
    }

    private void give(Player player, Modifier modifier) {
        PotionEffectType effect = resolve(modifier);
        if (effect == null) {
            return;
        }
        int amplifier = CatalogBounds.clampInt(modifier.modifierId(), "amplifier",
                ModifierParams.integer(modifier, "amplifier", DEFAULT_AMPLIFIER));
        player.addPotionEffect(new PotionEffect(effect, PotionEffect.INFINITE_DURATION, amplifier - 1));
    }

    private PotionEffectType resolve(Modifier modifier) {
        String effectId = ModifierParams.string(modifier, "effect");
        if (effectId == null) {
            LOGGER.warn("status_effect is missing its effect id; skipping.");
            return null;
        }
        PotionEffectType effect = GameIds.effect(effectId);
        if (effect == null) {
            LOGGER.warn("Unknown status effect {}; skipping.", effectId);
        }
        return effect;
    }
}
