package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.lifecycle.RunPersistenceBridge;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Writes the active run to disk whenever the server saves the world. Both the
 * periodic autosave and the save on shutdown route through {@code saveEverything},
 * and Fabric exposes no public world-save event, so this small hook is what
 * keeps the persisted run current between explicit lifecycle transitions. A
 * crash still loses only the clock ticks since the last save, an accepted
 * tradeoff over persisting every tick.
 */
@Mixin(MinecraftServer.class)
public class ServerSaveMixin {

    @Inject(method = "saveEverything", at = @At("HEAD"))
    private void challengex$onSave(boolean suppressLog, boolean flush, boolean forced,
            CallbackInfoReturnable<Boolean> info) {
        RunPersistenceBridge.onWorldSave((MinecraftServer) (Object) this);
    }
}
