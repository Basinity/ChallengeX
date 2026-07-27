package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.core.model.Modifier;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code modifier.no_hunger_drain}: tops hunger and saturation back up once
 * activated and again on any tick they are found below max, rather than writing
 * them unconditionally every tick.
 */
public final class NoHungerDrainEnforcer implements ModifierEnforcer {

    private static final int MAX_FOOD_LEVEL = 20;
    private static final float MAX_SATURATION = 20.0f;

    @Override
    public void start(Player player, Modifier modifier, Server server) {
        topUp(player);
    }

    @Override
    public void tick(Player player, Modifier modifier, Server server) {
        if (player.getFoodLevel() < MAX_FOOD_LEVEL || player.getSaturation() < MAX_SATURATION) {
            topUp(player);
        }
    }

    private static void topUp(Player player) {
        player.setFoodLevel(MAX_FOOD_LEVEL);
        player.setSaturation(MAX_SATURATION);
    }
}
