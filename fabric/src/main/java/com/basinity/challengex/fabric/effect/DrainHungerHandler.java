package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * {@code effect.drain_hunger}: lowers hunger. With an {@code amount} it drains
 * that many points (clamped so the bar stays within 0..20); without one it
 * empties the bar.
 */
public final class DrainHungerHandler implements EffectHandler {

    private static final int MAX_FOOD = 20;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean hasAmount = EffectParams.has(command, "amount");
        int amount = Math.max(0, EffectParams.integer(command, "amount", 0));
        for (ServerPlayer target : targets) {
            FoodData food = target.getFoodData();
            if (hasAmount) {
                food.setFoodLevel(EffectParams.clamp(food.getFoodLevel() - amount, 0, MAX_FOOD));
            } else {
                food.setFoodLevel(0);
            }
        }
    }
}
