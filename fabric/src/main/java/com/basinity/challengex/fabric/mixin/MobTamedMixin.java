package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.mob_tamed}: a player tamed an animal, a wolf, cat, parrot,
 * horse family, and the rest. It rides the tame call, which vanilla runs the
 * moment the animal accepts the player as its owner.
 */
@Mixin(TamableAnimal.class)
public class MobTamedMixin {

    @Inject(method = "tame", at = @At("TAIL"))
    private void challengex$onTame(Player player, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.mob_tamed", serverPlayer.getScoreboardName()));
        }
    }
}
