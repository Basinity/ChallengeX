package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.block_placed}: a player placed a block. No Fabric event covers
 * placement, so this rides {@link BlockItem#place}, firing only when the place
 * actually took effect. The {@code block} parameter matches the placed block's
 * id. Dispenser-placed blocks carry no player and are skipped.
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void challengex$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> info) {
        if (!info.getReturnValue().consumesAction() || !(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(((BlockItem) (Object) this).getBlock()).toString();
        MixinTriggerBridge.emit(GameEvent.of("trigger.block_placed", player.getScoreboardName(),
                Map.of("block", ParamValue.of(blockId))));
    }
}
