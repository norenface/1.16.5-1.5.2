package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
public class Slot {
    public final IInventory inventory;
    public int slotNumber;
    public int xDisplayPosition;
    public int yDisplayPosition;
    public Slot(IInventory inventory, int slotIndex, int xPos, int yPos) {
        this.inventory = inventory; this.slotNumber = slotIndex; this.xDisplayPosition = xPos; this.yDisplayPosition = yPos;
    }
    public boolean isItemValid(ItemStack stack) { return true; }
    public ItemStack getStack() { return inventory.getStackInSlot(slotNumber); }
    public boolean getHasStack() { return getStack() != null; }
    public void putStack(ItemStack stack) { inventory.setInventorySlotContents(slotNumber, stack); onSlotChanged(); }
    public void onSlotChanged() { inventory.onInventoryChanged(); }
    public boolean canTakeStack(EntityPlayer player) { return true; }
    public ItemStack decrStackSize(int amount) { return inventory.decrStackSize(slotNumber, amount); }
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {}
    public int getSlotIndex() { return slotNumber; }
    public int getSlotStackLimit() { return 64; }
}
