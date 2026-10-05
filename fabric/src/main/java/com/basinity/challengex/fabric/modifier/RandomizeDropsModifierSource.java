package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.common.modifier.ModifierContext;
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
 * The two loot randomizers: {@code modifier.randomize_block_drops} and
 * {@code modifier.randomize_mob_drops}. Both ride
 * {@link LootTableEvents#MODIFY_DROPS}, the one Fabric API event that fires for
 * every loot table category with its generated drops still mutable, rather than
 * a Mixin, since the drops are already computed by the time this fires and just
 * need swapping.
 *
 * <p>Which randomizer a roll belongs to is read off the loot context. Block
 * drops are recognized by the presence of {@code BLOCK_STATE} and scoped to the
 * breaking player ({@code THIS_ENTITY}); mob drops are recognized by
 * {@code DAMAGE_SOURCE} and scoped to whoever last damaged the mob
 * ({@code LAST_DAMAGE_PLAYER}, the same param vanilla itself uses for
 * player-only drops). A drop generated with no identifiable player (an
 * explosion, a mob dying to fall damage) is left alone rather than guessed at.
 *
 * <p>Both randomizers replace each dropped stack in place with a same-count
 * substitute from {@link RandomizedItemSubstitution}.
 */
public final class RandomizeDropsModifierSource implements ModifierSource {

    @Override
    public void register(ModifierContext context) {
        LootTableEvents.MODIFY_DROPS.register((table, lootContext, drops) -> {
            if (lootContext.hasParameter(LootContextParams.BLOCK_STATE)) {
                ServerPlayer player = lootContext.getOptional(LootContextParams.THIS_ENTITY)
                        instanceof ServerPlayer breaker ? breaker : null;
                randomizeFor(context, player, "modifier.randomize_block_drops", drops);
            } else if (lootContext.hasParameter(LootContextParams.DAMAGE_SOURCE)) {
                ServerPlayer player = lootContext.getOptional(LootContextParams.LAST_DAMAGE_PLAYER)
                        instanceof ServerPlayer killer ? killer : null;
                randomizeFor(context, player, "modifier.randomize_mob_drops", drops);
            }
        });
    }

    /** Randomizes the drops when this player is in a run with the modifier in force. */
    private void randomizeFor(ModifierContext context, ServerPlayer player, String modifierId, List<ItemStack> drops) {
        if (player == null) {
            return;
        }
        context.find(player.getScoreboardName(), modifierId)
                .ifPresent(modifier -> randomizeItems(drops, modifier, player.getScoreboardName()));
    }

    private void randomizeItems(List<ItemStack> drops, Modifier modifier, String playerId) {
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
