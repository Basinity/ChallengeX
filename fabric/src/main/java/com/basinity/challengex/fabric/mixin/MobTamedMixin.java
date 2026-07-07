package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.mob_tamed}: a player tamed an animal, a wolf, cat, parrot,
 * horse family, and the rest. It rides the tame call, which vanilla runs the
 * moment the animal accepts the player as its owner. The {@code mob} parameter
 * matches the tamed animal's type.
 */
@Mixin(TamableAnimal.class)
public class MobTamedMixin {

    @Inject(method = "tame", at = @At("TAIL"))
    private void challengex$onTame(Player player, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(
                    ((TamableAnimal) (Object) this).getType()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.mob_tamed", serverPlayer.getScoreboardName(),
                    Map.of("mob", ParamValue.of(mobId))));
        }
    }
}
