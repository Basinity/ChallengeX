package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code effect.drain_hunger}: lowers hunger. With an {@code amount} it drains
 * that many hunger shanks, each two food points on the vanilla 20-point bar, so
 * {@code amount=1} empties one shank; without one it empties the whole bar.
 */
public final class DrainHungerHandler implements EffectHandler {

    private static final int MAX_FOOD = 20;
    private static final int POINTS_PER_SHANK = 2;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        boolean hasAmount = EffectParams.has(command, "amount");
        int points = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 0)) * POINTS_PER_SHANK;
        for (Player target : targets) {
            target.setFoodLevel(hasAmount
                    ? EffectParams.clamp(target.getFoodLevel() - points, 0, MAX_FOOD)
                    : 0);
        }
    }
}
