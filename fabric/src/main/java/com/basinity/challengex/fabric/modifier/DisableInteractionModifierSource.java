package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code modifier.disable_interaction}: blocks right-click interaction with one
 * specific block (its required {@code target} id, e.g. "no crafting table")
 * for as long as the modifier is active. Rides the same {@link
 * UseBlockCallback} {@code trigger.block_interacted} observes.
 */
public final class DisableInteractionModifierSource implements ModifierSource {

    @Override
    public void register(ModifierContext context) {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            Modifier modifier = context.find(serverPlayer.getScoreboardName(), "modifier.disable_interaction")
                    .orElse(null);
            if (modifier == null) {
                return InteractionResult.PASS;
            }
            String target = ModifierParams.string(modifier, "target");
            BlockState state = level.getBlockState(hit.getBlockPos());
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return blockId.equals(target) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }
}
