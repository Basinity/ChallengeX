package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.fabric.modifier.ModifierBridge;
import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.fabric.modifier.RandomizedItemSubstitution;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code modifier.randomize_crafting}: every recipe produces something else.
 *
 * <p>By default it rides the one place the crafting result is worked out and
 * put into the result slot, which is both what the client is shown and what the
 * player then takes, so the preview tells the truth: you see the rabbit's foot
 * before you commit to crafting it.
 *
 * <p>{@code hide_result} turns that around. The slot is left holding the
 * recipe's own item so the preview gives nothing away, and {@link
 * CraftingTakeMixin} does the swap at the moment the result is taken, so what
 * comes out is a surprise.
 *
 * <p>The substitution is the same seeded, deterministic mapping the drop
 * randomizers use, so one recipe always produces the same thing for a given
 * seed and the run stays learnable. The count the recipe asked for is kept: a
 * recipe yielding four planks yields four of whatever it now makes.
 */
@Mixin(CraftingMenu.class)
public class CraftingResultMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void challengex$randomizeResult(AbstractContainerMenu menu, ServerLevel level,
            Player player, CraftingContainer grid, ResultContainer result,
            RecipeHolder<CraftingRecipe> recipe, CallbackInfo info) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack crafted = result.getItem(0);
        if (crafted.isEmpty()) {
            return;
        }
        Modifier modifier = ModifierBridge
                .find(serverPlayer.getScoreboardName(), "modifier.randomize_crafting")
                .orElse(null);
        if (modifier == null || ModifierParams.bool(modifier, "hide_result", false)) {
            // With the result hidden the slot keeps the recipe's own item, so
            // the preview gives nothing away; the swap happens as it is taken.
            return;
        }
        Identifier original = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        Identifier substitute = RandomizedItemSubstitution.substituteFor(
                original, modifier, serverPlayer.getScoreboardName());
        BuiltInRegistries.ITEM.getOptional(substitute).ifPresent(item ->
                result.setItem(0, new ItemStack(item, crafted.getCount())));
    }
}
