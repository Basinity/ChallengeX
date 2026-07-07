package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.container_opened}: a player opened a container screen, a chest,
 * barrel, furnace, crafting table, and the rest. It rides menu opening, which
 * returns the new screen's id when a screen actually opened. The {@code container}
 * parameter matches the menu type ({@code minecraft:generic_9x3}); omitting it
 * fires on any container.
 */
@Mixin(ServerPlayer.class)
public class ContainerOpenedMixin {

    @Inject(method = "openMenu", at = @At("RETURN"))
    private void challengex$onOpen(MenuProvider provider, CallbackInfoReturnable<OptionalInt> info) {
        if (info.getReturnValue().isEmpty()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        MenuType<?> type = player.containerMenu.getType();
        if (type == null) {
            return;
        }
        String containerId = BuiltInRegistries.MENU.getKey(type).toString();
        MixinTriggerBridge.emit(GameEvent.of("trigger.container_opened", player.getScoreboardName(),
                Map.of("container", ParamValue.of(containerId))));
    }
}
