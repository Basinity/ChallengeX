package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

/** {@code effect.falling_anvil}: drops an anvil the given {@code height} in blocks above each target. */
public final class FallingAnvilHandler implements EffectHandler {

    private static final int DEFAULT_HEIGHT = 5;
    // Vanilla's own falling-anvil values: two damage per block fallen, up to 40.
    private static final float DAMAGE_PER_BLOCK = 2.0f;
    private static final int MAX_DAMAGE = 40;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int height = CatalogBounds.clampInt(command.effectId(), "height",
                EffectParams.integer(command, "height", DEFAULT_HEIGHT));
        for (Player target : targets) {
            Location above = target.getLocation().add(0.0, height, 0.0);
            FallingBlock anvil = target.getWorld()
                    .spawnFallingBlock(above, Material.ANVIL.createBlockData());
            // Spawning alone drops a cosmetic block; this makes it hurt on landing.
            anvil.setHurtEntities(true);
            anvil.setDamagePerBlock(DAMAGE_PER_BLOCK);
            anvil.setMaxDamage(MAX_DAMAGE);
        }
    }
}
