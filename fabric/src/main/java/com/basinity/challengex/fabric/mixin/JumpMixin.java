package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.MinecraftServer;
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
 *
 * <p>The emit is deferred to the server's task queue rather than dispatched
 * inline: vanilla calls {@code jumpFromGround} from inside the movement-packet
 * handler, and an effect that moves the player mid-handling (a teleport) is
 * seen by the rest of that handler as an illegal client move and reverted
 * ("moved wrongly!"), snapping the player back down. Queued, the effect runs
 * after the handler returns and lands like a normal teleport.
 */
@Mixin(LivingEntity.class)
public class JumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void challengex$onJump(CallbackInfo info) {
        if ((Object) this instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            GameEvent event = GameEvent.of("trigger.jumped", player.getScoreboardName());
            server.schedule(server.wrapRunnable(() -> MixinTriggerBridge.emit(event)));
        }
    }
}
