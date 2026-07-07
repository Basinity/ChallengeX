package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.projectile_shot}: a player launched a projectile, an arrow,
 * trident, snowball, egg, ender pearl, or the like. Projectiles reach the world
 * through many item paths but all funnel through the level's entity spawn, so
 * this rides that one chokepoint and filters to projectiles owned by a player.
 * The {@code projectile} parameter matches the projectile's type; it fires only
 * when the spawn actually took.
 */
@Mixin(ServerLevel.class)
public class ProjectileShotMixin {

    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void challengex$onProjectile(Entity entity, CallbackInfoReturnable<Boolean> info) {
        if (!Boolean.TRUE.equals(info.getReturnValue()) || !(entity instanceof Projectile projectile)) {
            return;
        }
        if (projectile.getOwner() instanceof ServerPlayer player) {
            String projectileId = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.projectile_shot", player.getScoreboardName(),
                    Map.of("projectile", ParamValue.of(projectileId))));
        }
    }
}
