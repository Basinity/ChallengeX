package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * {@code modifier.no_hunger_drain}: tops hunger and saturation back up once
 * activated and again on any tick they're found below max, rather than
 * writing them unconditionally every tick.
 */
public final class NoHungerDrainEnforcer implements ModifierEnforcer {

    private static final int MAX_FOOD_LEVEL = 20;
    private static final float MAX_SATURATION = 20.0f;

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        topUp(player);
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < MAX_FOOD_LEVEL || food.getSaturationLevel() < MAX_SATURATION) {
            topUp(player);
        }
    }

    private void topUp(ServerPlayer player) {
        player.getFoodData().setFoodLevel(MAX_FOOD_LEVEL);
        player.getFoodData().setSaturation(MAX_SATURATION);
    }
}
