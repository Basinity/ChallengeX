package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.mounted}: a player started riding a horse, boat, minecart, or
 * any other vehicle. It rides the shared start-riding call and fires only when
 * mounting actually took. The {@code mob} parameter matches the vehicle's type.
 */
@Mixin(Entity.class)
public class MountedMixin {

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
    private void challengex$onMount(Entity vehicle, boolean force, boolean sendPacket,
            CallbackInfoReturnable<Boolean> info) {
        if (Boolean.TRUE.equals(info.getReturnValue()) && (Object) this instanceof ServerPlayer player) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.mounted", player.getScoreboardName(),
                    Map.of("mob", ParamValue.of(mobId))));
        }
    }
}
