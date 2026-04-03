package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import java.util.*;
public abstract class Container {
    public List inventorySlots = new ArrayList();
    protected Slot addSlotToContainer(Slot slot) { slot.slotNumber = inventorySlots.size(); inventorySlots.add(slot); return slot; }
    public abstract boolean canInteractWith(EntityPlayer player);
    public ItemStack slotClick(int slotId, int button, int modifier, EntityPlayer player) { return null; }
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) { return null; }
    public void detectAndSendChanges() {}
    public Slot getSlot(int slotId) { return (Slot) inventorySlots.get(slotId); }
    public void onContainerClosed(EntityPlayer player) {}
    public void addCraftingToCrafters(ICrafting crafter) {}
}
