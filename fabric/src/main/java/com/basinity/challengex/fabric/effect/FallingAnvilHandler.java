package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;

/** {@code effect.falling_anvil}: drops an anvil the given {@code height} in blocks above each target. */
public final class FallingAnvilHandler implements EffectHandler {

    private static final int DEFAULT_HEIGHT = 5;
    // Vanilla's own falling-anvil values: two damage per block fallen, up to 40.
    private static final float DAMAGE_PER_BLOCK = 2.0f;
    private static final int MAX_DAMAGE = 40;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int height = EffectParams.clamp(EffectParams.integer(command, "height", DEFAULT_HEIGHT), 1, 128);
        for (ServerPlayer target : targets) {
            BlockPos above = target.blockPosition().above(height);
            FallingBlockEntity anvil = FallingBlockEntity.fall(target.level(), above,
                    Blocks.ANVIL.defaultBlockState());
            // fall() alone drops a cosmetic block; this makes it hurt on landing.
            anvil.setHurtsEntities(DAMAGE_PER_BLOCK, MAX_DAMAGE);
        }
    }
}
