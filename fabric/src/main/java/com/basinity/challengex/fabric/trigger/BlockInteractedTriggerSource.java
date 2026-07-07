package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code trigger.block_interacted}: a player right-clicked a block, a lever,
 * button, door, or any other interactable. The {@code block} parameter matches
 * the block interacted with; omitting it fires on any block.
 */
public final class BlockInteractedTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                BlockState state = level.getBlockState(hit.getBlockPos());
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                context.emit(GameEvent.of("trigger.block_interacted", serverPlayer.getScoreboardName(),
                        Map.of("block", ParamValue.of(blockId))));
            }
            return InteractionResult.PASS;
        });
    }
}
