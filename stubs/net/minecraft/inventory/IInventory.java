package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
public interface IInventory {
    int getSizeInventory();
    ItemStack getStackInSlot(int index);
    ItemStack decrStackSize(int index, int count);
    ItemStack getStackInSlotOnClosing(int index);
    void setInventorySlotContents(int index, ItemStack stack);
    String getInvName();
    boolean isInvNameLocalized();
    int getInventoryStackLimit();
    void onInventoryChanged();
    boolean isUseableByPlayer(EntityPlayer player);
    void openChest();
    void closeChest();
    boolean isItemValidForSlot(int index, ItemStack stack);
}
