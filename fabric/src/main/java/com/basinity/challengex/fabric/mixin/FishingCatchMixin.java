package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.fish_caught}: a player reeled in a fishing line that had
 * caught something. It rides the hook's retrieve, which returns the amount of
 * damage the rod took: a hooked catch returns a positive value, an empty pull
 * returns zero, so a positive return is a real catch.
 */
@Mixin(FishingHook.class)
public class FishingCatchMixin {

    @Inject(method = "retrieve", at = @At("RETURN"))
    private void challengex$onRetrieve(ItemStack rod, CallbackInfoReturnable<Integer> info) {
        FishingHook hook = (FishingHook) (Object) this;
        if (info.getReturnValueI() > 0 && hook.getPlayerOwner() instanceof ServerPlayer player) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.fish_caught", player.getScoreboardName()));
        }
    }
}
