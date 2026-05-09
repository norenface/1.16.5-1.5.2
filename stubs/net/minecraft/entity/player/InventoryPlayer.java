package net.minecraft.entity.player;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
public class InventoryPlayer implements IInventory {
    public ItemStack[] field_70462_a = new ItemStack[36]; // mainInventory
    public ItemStack[] armorInventory = new ItemStack[4];
    public int field_70461_c = 0;  // currentItem
    private ItemStack itemStack; // currently held (picked up) item
    public InventoryPlayer(EntityPlayer player) {}
    public ItemStack func_70445_o() { return field_70461_c >= 0 && field_70461_c < field_70462_a.length ? field_70462_a[field_70461_c] : null; } // getCurrentItem
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack stack) { this.itemStack = stack; }
    public boolean func_70441_a(ItemStack stack) { return false; } // addItemStackToInventory
    @Override public int func_70302_a() { return field_70462_a.length; }          // getSizeInventory
    @Override public ItemStack func_70301_a(int i) { return i < field_70462_a.length ? field_70462_a[i] : (i - field_70462_a.length < armorInventory.length ? armorInventory[i - field_70462_a.length] : null); } // getStackInSlot
    @Override public ItemStack func_70298_a(int i, int count) { return null; }    // decrStackSize
    @Override public ItemStack func_70304_a(int i) { return null; }               // getStackInSlotOnClosing
    @Override public void func_70299_a(int i, ItemStack s) { if (i < field_70462_a.length) field_70462_a[i] = s; } // setInventorySlotContents
    @Override public String func_70303_a() { return "inventory"; }                // getInvName
    @Override public boolean func_71125_a() { return false; }                     // isInvNameLocalized
    @Override public int func_70297_a() { return 64; }                            // getInventoryStackLimit
    @Override public void func_70296_a() {}                                        // onInventoryChanged
    @Override public boolean func_70300_a(EntityPlayer p) { return true; }        // isUseableByPlayer
    @Override public void func_70295_a() {}                                        // openChest
    @Override public void func_70305_a() {}                                        // closeChest
    @Override public boolean func_94041_b(int i, ItemStack s) { return true; }    // isItemValidForSlot
}
