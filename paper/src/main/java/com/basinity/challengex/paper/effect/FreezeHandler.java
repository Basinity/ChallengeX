package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * {@code effect.freeze}: roots each target in place for {@code seconds} (default
 * three). It applies overwhelming slowness, which halts walking; a full lock of
 * jumping and looking is the pause control's job, not this effect's.
 */
public final class FreezeHandler implements EffectHandler {

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_SECONDS = 3;
    // High enough that the movement-speed attribute floors at zero.
    private static final int ROOT_AMPLIFIER = 250;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int seconds = CatalogBounds.clampInt(command.effectId(), "seconds",
                EffectParams.integer(command, "seconds", DEFAULT_SECONDS));
        for (Player target : targets) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    seconds * TICKS_PER_SECOND, ROOT_AMPLIFIER));
        }
    }
}
