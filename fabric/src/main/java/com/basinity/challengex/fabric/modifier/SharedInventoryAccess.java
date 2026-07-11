package com.basinity.challengex.fabric.modifier;

/**
 * The seam {@code SharedInventoryEnforcer} drives a player's inventory through.
 * A Mixin implements it on {@code net.minecraft.world.entity.player.Inventory},
 * so casting a live {@code Inventory} to this interface reaches the by-reference
 * wiring: pointing the inventory's item and equipment stores at a group's shared
 * objects, or detaching it back to private copies.
 */
public interface SharedInventoryAccess {

    /**
     * Copies this inventory's current main slots and armor/offhand into {@code
     * shared}, seeding an empty group from its first member. Leaves this
     * inventory untouched; {@link #challengex$share} then points it at the seeded
     * objects.
     */
    void challengex$seedInto(SharedInventory shared);

    /**
     * Points this inventory's main-item list and its equipment's armor/offhand
     * map at {@code shared}'s objects by reference, so mutations are shared with
     * every other member pointed at the same {@link SharedInventory}. The
     * selected hotbar slot stays this player's own index into the shared list.
     */
    void challengex$share(SharedInventory shared);

    /**
     * Detaches this inventory from any shared objects, giving it fresh private
     * copies of its current contents, so leaving a group is non-destructive and
     * the shared objects keep living for the remaining members.
     */
    void challengex$detach();
}
