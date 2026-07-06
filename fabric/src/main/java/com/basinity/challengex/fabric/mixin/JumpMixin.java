package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.jumped}: a player jumped. No Fabric event exposes a jump, so this
 * rides the living-entity jump that vanilla runs the instant a jump launches,
 * filtered to players. It fires once per jump, not per airborne tick.
 */
@Mixin(LivingEntity.class)
public class JumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void challengex$onJump(CallbackInfo info) {
        if ((Object) this instanceof ServerPlayer player) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.jumped", player.getScoreboardName()));
        }
    }
}
