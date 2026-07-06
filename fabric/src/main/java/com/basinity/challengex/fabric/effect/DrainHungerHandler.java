package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * {@code effect.drain_hunger}: lowers hunger. With an {@code amount} it drains
 * that many hunger shanks, each two food points on the vanilla 20-point bar, so
 * {@code amount=1} empties one shank; without one it empties the whole bar.
 */
public final class DrainHungerHandler implements EffectHandler {

    private static final int MAX_FOOD = 20;
    private static final int POINTS_PER_SHANK = 2;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean hasAmount = EffectParams.has(command, "amount");
        int points = Math.max(0, EffectParams.integer(command, "amount", 0)) * POINTS_PER_SHANK;
        for (ServerPlayer target : targets) {
            FoodData food = target.getFoodData();
            if (hasAmount) {
                food.setFoodLevel(EffectParams.clamp(food.getFoodLevel() - points, 0, MAX_FOOD));
            } else {
                food.setFoodLevel(0);
            }
        }
    }
}
