package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;

/** {@code effect.falling_anvil}: drops an anvil the given number of blocks above each target. */
public final class FallingAnvilHandler implements EffectHandler {

    private static final int DEFAULT_HEIGHT = 5;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int height = EffectParams.clamp(EffectParams.integer(command, "blocks", DEFAULT_HEIGHT), 1, 128);
        for (ServerPlayer target : targets) {
            BlockPos above = target.blockPosition().above(height);
            FallingBlockEntity.fall(target.level(), above, Blocks.ANVIL.defaultBlockState());
        }
    }
}
