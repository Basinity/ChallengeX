package com.basinity.challengex.fabric.modifier;

import java.util.EnumMap;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * One group's shared inventory state, the single object every member of a
 * {@code modifier.share_inventory} group points at by reference. {@link #main}
 * is the 36 main-and-hotbar slots (a player's {@code Inventory.items}); {@link
 * #equipment} is the armor and offhand slots (a player's {@code
 * EntityEquipment.items} map). Both are wired into each member's inventory in
 * place of their private objects, so any mutation one member makes is
 * inherently visible to the rest with no copying or diffing.
 *
 * <p>The mainhand is deliberately absent: a player's selected hotbar slot is a
 * private index into the shared {@link #main} list, so each member holds their
 * own slot of the shared hotbar without the item itself being a separate shared
 * slot. Both fields are seeded once, from the group's first member, by {@link
 * SharedInventoryAccess#challengex$seedInto}.
 */
public final class SharedInventory {

    public NonNullList<ItemStack> main;
    public EnumMap<EquipmentSlot, ItemStack> equipment;
}
