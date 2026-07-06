package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.container_opened}: a player opened a container screen, a chest,
 * barrel, furnace, crafting table, and the rest. It rides menu opening, which
 * returns the new screen's id when a screen actually opened, so a menu that
 * failed to open does not trigger it.
 */
@Mixin(ServerPlayer.class)
public class ContainerOpenedMixin {

    @Inject(method = "openMenu", at = @At("RETURN"))
    private void challengex$onOpen(MenuProvider provider, CallbackInfoReturnable<OptionalInt> info) {
        if (info.getReturnValue().isPresent()) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            MixinTriggerBridge.emit(GameEvent.of("trigger.container_opened", player.getScoreboardName()));
        }
    }
}
