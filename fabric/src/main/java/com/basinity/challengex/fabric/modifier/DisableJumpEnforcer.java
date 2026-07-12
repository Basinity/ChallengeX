package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * {@code modifier.disable_jump}: zeroes the jump-strength attribute for as
 * long as the modifier is active, real prevention rather than a status effect
 * (the attribute isn't touched by milk or anything else a player can do about
 * it). {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL} at {@code -1.0}
 * multiplies the total to zero regardless of any other source's contribution.
 * The attribute modifier is transient and the player object is rebuilt on
 * respawn and reconnect, so {@link #tick} re-adds it whenever it's found
 * missing rather than trusting the one from {@link #start} to stay.
 */
public final class DisableJumpEnforcer implements ModifierEnforcer {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("challengex", "disable_jump");

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        apply(player);
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        AttributeInstance attribute = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (attribute != null && !attribute.hasModifier(MODIFIER_ID)) {
            apply(player);
        }
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        AttributeInstance attribute = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }

    private void apply(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (attribute != null) {
            attribute.addOrUpdateTransientModifier(
                    new AttributeModifier(MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
