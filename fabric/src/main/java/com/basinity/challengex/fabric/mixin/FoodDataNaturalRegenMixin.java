package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ModifierBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * {@code modifier.no_natural_regen}: forces the "is natural regeneration
 * allowed" boolean {@link FoodData#tick} reads off the world's gamerule to
 * false for a player carrying the modifier. That boolean gates both the fast
 * saturation-driven and slow food-driven regen branches as a unit, each
 * bundling its own exhaustion cost with its heal, so forcing the shared gate
 * rather than skipping the heal calls alone avoids draining hunger for a
 * regen that never happened. Hunger drain and starvation damage sit in
 * branches this boolean doesn't gate, so neither is touched.
 */
@Mixin(FoodData.class)
public class FoodDataNaturalRegenMixin {

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private boolean challengex$gateNaturalRegen(boolean naturalRegenAllowed, ServerPlayer player) {
        return naturalRegenAllowed && !ModifierBridge.isActive(player.getScoreboardName(), "modifier.no_natural_regen");
    }
}
