package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.block_broken}: a player finished breaking a block. The
 * {@code block} parameter matches the block's id.
 */
public final class BlockBrokenTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            context.emit(GameEvent.of("trigger.block_broken", serverPlayer.getScoreboardName(),
                    Map.of("block", ParamValue.of(blockId))));
        });
    }
}
