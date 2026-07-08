package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * {@code modifier.randomize_block_drops} and {@code modifier.randomize_mob_drops}:
 * both ride {@link LootTableEvents#MODIFY_DROPS}, the one Fabric API event that
 * fires for every loot table category with its generated drops still mutable,
 * rather than a Mixin, since the drops are already computed by the time this
 * fires and just need swapping. Block drops are recognized by the presence of
 * {@code BLOCK_STATE} in the loot context and scoped to the breaking player
 * ({@code THIS_ENTITY}); mob drops are recognized by {@code DAMAGE_SOURCE} and
 * scoped to whoever last damaged the mob ({@code LAST_DAMAGE_PLAYER}, the same
 * param vanilla itself uses for player-only drops). A drop generated with no
 * identifiable player (an explosion, a mob dying to fall damage) is left alone
 * rather than guessed at. Each dropped stack is replaced in place with a
 * same-count substitute from {@link RandomizedItemSubstitution}.
 */
public final class RandomizeDropsModifierSource implements ModifierSource {

    @Override
    public void register(ModifierContext context) {
        LootTableEvents.MODIFY_DROPS.register((table, lootContext, drops) -> {
            String modifierId;
            ServerPlayer player;
            if (lootContext.hasParameter(LootContextParams.BLOCK_STATE)) {
                modifierId = "modifier.randomize_block_drops";
                player = lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof ServerPlayer sp
                        ? sp : null;
            } else if (lootContext.hasParameter(LootContextParams.DAMAGE_SOURCE)) {
                modifierId = "modifier.randomize_mob_drops";
                player = lootContext.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer sp
                        ? sp : null;
            } else {
                return;
            }
            if (player == null) {
                return;
            }
            context.find(player.getScoreboardName(), modifierId).ifPresent(modifier ->
                    randomize(drops, modifier, player.getScoreboardName()));
        });
    }

    private void randomize(List<ItemStack> drops, Modifier modifier, String playerId) {
        for (int i = 0; i < drops.size(); i++) {
            ItemStack original = drops.get(i);
            if (original.isEmpty()) {
                continue;
            }
            Identifier originalId = BuiltInRegistries.ITEM.getKey(original.getItem());
            Identifier substituteId = RandomizedItemSubstitution.substituteFor(originalId, modifier, playerId);
            Item substitute = BuiltInRegistries.ITEM.getValue(substituteId);
            drops.set(i, new ItemStack(substitute, original.getCount()));
        }
    }
}
