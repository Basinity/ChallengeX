package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * {@code effect.random_effect}: applies a random status effect to each target.
 * The {@code type} parameter narrows the pool to {@code negative} (harmful) or
 * {@code positive} (beneficial); omitting it, or {@code any}, draws from all.
 * Each target rolls its own effect.
 */
public final class RandomEffectHandler implements EffectHandler {

    private static final int DURATION_TICKS = 300;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String type = EffectParams.string(command, "type");
        List<PotionEffectType> pool = Registry.EFFECT.stream()
                .filter(effect -> matchesType(effect, type))
                .toList();
        if (pool.isEmpty()) {
            return;
        }
        for (Player target : targets) {
            PotionEffectType chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            target.addPotionEffect(new PotionEffect(chosen, DURATION_TICKS, 0));
        }
    }

    private static boolean matchesType(PotionEffectType effect, String type) {
        if (type == null) {
            return true;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "negative" -> effect.getEffectCategory() == PotionEffectType.Category.HARMFUL;
            case "positive" -> effect.getEffectCategory() == PotionEffectType.Category.BENEFICIAL;
            default -> true;
        };
    }
}
