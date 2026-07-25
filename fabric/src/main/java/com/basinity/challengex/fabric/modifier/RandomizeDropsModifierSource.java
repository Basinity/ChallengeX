package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The three loot randomizers: {@code modifier.randomize_block_drops},
 * {@code modifier.randomize_mob_drops} and {@code modifier.randomize_chest_loot}.
 * All three ride {@link LootTableEvents#MODIFY_DROPS}, the one Fabric API event
 * that fires for every loot table category with its generated drops still
 * mutable, rather than a Mixin, since the drops are already computed by the time
 * this fires and just need swapping.
 *
 * <p>Which randomizer a roll belongs to is read off the loot context. Block
 * drops are recognized by the presence of {@code BLOCK_STATE} and scoped to the
 * breaking player ({@code THIS_ENTITY}); mob drops are recognized by
 * {@code DAMAGE_SOURCE} and scoped to whoever last damaged the mob
 * ({@code LAST_DAMAGE_PLAYER}, the same param vanilla itself uses for
 * player-only drops); container loot carries neither, so the table's own id is
 * what separates a chest from fishing, gifts and the rest. A drop generated with
 * no identifiable player (an explosion, a mob dying to fall damage) is left
 * alone rather than guessed at.
 *
 * <p>The two drop randomizers replace each dropped stack in place with a
 * same-count substitute from {@link RandomizedItemSubstitution}. Chest loot
 * instead throws the rolled contents away and rolls a different container table
 * in their place, so the chest holds another structure's loot rather than a
 * village chest's shape filled with unrelated items.
 */
public final class RandomizeDropsModifierSource implements ModifierSource {

    /**
     * Set while a substitute table is being rolled. That roll fires this same
     * event for the substitute, which is a container table too and would
     * otherwise be substituted again without end.
     */
    private final ThreadLocal<Boolean> substituting = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public void register(ModifierContext context) {
        LootTableEvents.MODIFY_DROPS.register((table, lootContext, drops) -> {
            if (substituting.get()) {
                return;
            }
            if (lootContext.hasParameter(LootContextParams.BLOCK_STATE)) {
                ServerPlayer player = lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY)
                        instanceof ServerPlayer breaker ? breaker : null;
                forPlayer(context, player, "modifier.randomize_block_drops", (modifier, playerId) ->
                        randomizeItems(drops, modifier, playerId));
            } else if (lootContext.hasParameter(LootContextParams.DAMAGE_SOURCE)) {
                ServerPlayer player = lootContext.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER)
                        instanceof ServerPlayer killer ? killer : null;
                forPlayer(context, player, "modifier.randomize_mob_drops", (modifier, playerId) ->
                        randomizeItems(drops, modifier, playerId));
            } else if (containerTable(table) != null) {
                // Container loot does not always name who opened it, and unlike
                // a broken block or a killed mob there is no other candidate on
                // the context. A chest sits in the world rather than belonging
                // to anybody, so any player the modifier is in force for will do.
                ServerPlayer player = lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY)
                        instanceof ServerPlayer opener
                        ? opener : anyPlayerWith(context, lootContext, "modifier.randomize_chest_loot");
                forPlayer(context, player, "modifier.randomize_chest_loot", (modifier, playerId) ->
                        swapTable(containerTable(table), lootContext, drops, modifier, playerId));
            }
        });
    }

    /** Runs the randomizer when this player is in a run with the modifier in force. */
    private void forPlayer(ModifierContext context, ServerPlayer player, String modifierId, Randomizer randomizer) {
        if (player == null) {
            return;
        }
        context.find(player.getScoreboardName(), modifierId)
                .ifPresent(modifier -> randomizer.run(modifier, player.getScoreboardName()));
    }

    /** The first player in the run this modifier is in force for, or null. */
    private ServerPlayer anyPlayerWith(ModifierContext context, LootContext lootContext, String modifierId) {
        if (lootContext.getLevel() == null || lootContext.getLevel().getServer() == null) {
            return null;
        }
        for (ServerPlayer candidate : lootContext.getLevel().getServer().getPlayerList().getPlayers()) {
            if (context.find(candidate.getScoreboardName(), modifierId).isPresent()) {
                return candidate;
            }
        }
        return null;
    }

    /** This table's key if it is a container's, or null. */
    private ResourceKey<LootTable> containerTable(Holder<LootTable> table) {
        return table.unwrapKey()
                .filter(RandomizedLootTableSubstitution::isContainerTable)
                .orElse(null);
    }

    /**
     * Throws away what this container rolled and rolls a different container
     * table in its place, reusing the same loot context so the substitute sees
     * the same position, luck and opener the original did.
     */
    private void swapTable(ResourceKey<LootTable> originalKey, LootContext lootContext, List<ItemStack> drops,
            Modifier modifier, String playerId) {
        MinecraftServer server = lootContext.getLevel().getServer();
        ResourceKey<LootTable> substituteKey =
                RandomizedLootTableSubstitution.substituteFor(originalKey, modifier, playerId, server);
        if (substituteKey.equals(originalKey)) {
            return;
        }
        LootTable substitute = server.reloadableRegistries().getLootTable(substituteKey);
        drops.clear();
        substituting.set(Boolean.TRUE);
        try {
            substitute.getRandomItems(lootContext, drops::add);
        } finally {
            substituting.set(Boolean.FALSE);
        }
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

    /** What a randomizer does once its modifier and player are known. */
    @FunctionalInterface
    private interface Randomizer {
        void run(Modifier modifier, String playerId);
    }
}
