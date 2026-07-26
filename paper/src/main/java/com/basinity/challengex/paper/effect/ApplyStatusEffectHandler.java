package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String effectId = EffectParams.string(command, "effect");
        if (effectId == null) {
            LOGGER.warn("apply_status_effect is missing its effect id; skipping.");
            return;
        }
        PotionEffectType effect = GameIds.effect(effectId);
        if (effect == null) {
            LOGGER.warn("Unknown status effect {}; skipping.", effectId);
            return;
        }
        int seconds = EffectParams.integer(command, "duration", 0);
        int duration = seconds <= 0 ? PotionEffect.INFINITE_DURATION : seconds * TICKS_PER_SECOND;
        int amplifier = CatalogBounds.clampInt(command.effectId(), "amplifier",
                EffectParams.integer(command, "amplifier", DEFAULT_AMPLIFIER));
        for (Player target : targets) {
            target.addPotionEffect(new PotionEffect(effect, duration, amplifier - 1));
        }
    }
}
