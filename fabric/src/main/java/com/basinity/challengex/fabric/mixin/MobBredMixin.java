package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.mob_bred}: a player bred two animals into a baby. It rides the
 * breeding spawn, crediting the player recorded as the love cause, so only a
 * player-initiated breed triggers it. The {@code mob} parameter matches the bred
 * species; animals born by other means carry no love cause and are skipped.
 */
@Mixin(Animal.class)
public class MobBredMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("TAIL"))
    private void challengex$onBred(ServerLevel level, Animal mate, CallbackInfo info) {
        ServerPlayer breeder = ((Animal) (Object) this).getLoveCause();
        if (breeder != null) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(
                    ((Animal) (Object) this).getType()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.mob_bred", breeder.getScoreboardName(),
                    Map.of("mob", ParamValue.of(mobId))));
        }
    }
}
