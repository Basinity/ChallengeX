package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.core.model.Modifier;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/**
 * {@code modifier.disable_jump}: zeroes the jump-strength attribute for as long
 * as the modifier is active, real prevention rather than a status effect (the
 * attribute is not touched by milk or anything else a player can do about it).
 * {@code MULTIPLY_SCALAR_1} at {@code -1.0} multiplies the total to zero
 * regardless of any other source's contribution.
 *
 * <p>Cancelling Paper's jump event was tried first and rejected in play.
 * Cancelling is server-side only: the client has already predicted the jump and
 * gets snapped back out of it, which reads as being yanked to the ground rather
 * than as being unable to jump. The attribute is sent to the client, so the
 * client never starts the jump and there is nothing to correct.
 *
 * <p>The attribute modifier is transient and the player object is rebuilt on
 * respawn and reconnect, so {@link #tick} re-adds it whenever it is found
 * missing rather than trusting the one from {@link #start} to stay.
 */
public final class DisableJumpEnforcer implements ModifierEnforcer {

    private static final NamespacedKey KEY = new NamespacedKey("challengex", "disable_jump");

    @Override
    public void start(Player player, Modifier modifier, Server server) {
        apply(player);
    }

    @Override
    public void tick(Player player, Modifier modifier, Server server) {
        AttributeInstance attribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (attribute == null) {
            return;
        }
        boolean present = attribute.getModifiers().stream()
                .anyMatch(existing -> existing.getKey().equals(KEY));
        if (!present) {
            apply(player);
        }
    }

    @Override
    public void stop(Player player, Modifier modifier, Server server) {
        AttributeInstance attribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (attribute != null) {
            remove(attribute);
        }
    }

    private static void apply(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (attribute == null) {
            return;
        }
        remove(attribute);
        attribute.addTransientModifier(
                new AttributeModifier(KEY, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private static void remove(AttributeInstance attribute) {
        attribute.getModifiers().stream()
                .filter(existing -> existing.getKey().equals(KEY))
                .forEach(attribute::removeModifier);
    }
}
