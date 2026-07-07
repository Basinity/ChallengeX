package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.shield_blocked}: a player blocked an attack with a shield. It
 * rides the block-using-item call, which vanilla runs when a raised shield
 * actually absorbs a hit.
 */
@Mixin(LivingEntity.class)
public class ShieldBlockedMixin {

    @Inject(method = "blockUsingItem", at = @At("HEAD"))
    private void challengex$onBlock(ServerLevel level, LivingEntity attacker, DamageSource source,
            float amount, CallbackInfo info) {
        if ((Object) this instanceof ServerPlayer player) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.shield_blocked", player.getScoreboardName()));
        }
    }
}
