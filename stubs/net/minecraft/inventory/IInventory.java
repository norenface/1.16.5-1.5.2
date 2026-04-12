package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
public interface IInventory {
    int func_70302_a();                                     // getSizeInventory
    ItemStack func_70301_a(int index);                      // getStackInSlot
    ItemStack func_70298_a(int index, int count);           // decrStackSize
    ItemStack func_70304_a(int index);                      // getStackInSlotOnClosing
    void func_70299_a(int index, ItemStack stack);          // setInventorySlotContents
    String func_70303_a();                                  // getInvName
    boolean func_71125_a();                                 // isInvNameLocalized
    int func_70297_a();                                     // getInventoryStackLimit
    void func_70296_a();                                    // onInventoryChanged
    boolean func_70300_a(EntityPlayer player);              // isUseableByPlayer
    void func_70295_a();                                    // openChest
    void func_70305_a();                                    // closeChest
    boolean func_94041_b(int index, ItemStack stack);       // isItemValidForSlot
}
