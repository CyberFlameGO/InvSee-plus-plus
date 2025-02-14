package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * A spectator inventory that contains all the items of the target player's 'normal' inventory.
 * This includes the player's armour, items in his crafting grid, and even the item on his cursor!
 */
public interface MainSpectatorInventory extends SpectatorInventory<PlayerInventorySlot> {

    /** internal use only */
    void watch(InventoryView targetPlayerView);

    /** internal use only */
    void unwatch();

    /** Get the items in the player's armour slots. */
    ItemStack[] getArmourContents();

    /** Change the items in the player's armour slots. */
    void setArmourContents(ItemStack[] armourContents);

    /** Get the items in the player's offhand. The length of array is usually 1.*/
    ItemStack[] getOffHandContents();

    /** Set the items int he player's offhand. The length of this array must be equal to {@code getOffHandContents().length}. */
    void setOffHandContents(ItemStack[] offHand);

    /** Set the item that is on the player's cursor. */
    void setCursorContents(ItemStack cursor);

    /** Get the item that is on the player's cursor. */
    ItemStack getCursorContents();

    /** Set the items that are in the player's crafting slots.
     * If the target player has an anvil/crafting table/enchanting table/villager workstation opened,
     * then the contents of that temporary container will be updated instead. */
    void setPersonalContents(ItemStack[] craftingContents);

    /** Get the items that are in the player's crafting slots.
     * If the target player has an anvil/crafting/table/enchanting table/villager workstation opened,
     * then the contents of that temporary container will be retrieved instead.
     */
    ItemStack[] getPersonalContents();

    /** Get the length of the {@link #getPersonalContents()} array. */
    int getPersonalContentsSize();

    /** Get the mirror this inventory is viewed through. */
    public default Mirror<PlayerInventorySlot> getMirror() {
        return Mirror.defaultPlayerInventory();
    }

}
