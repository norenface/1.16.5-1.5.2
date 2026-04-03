package net.minecraft.entity.player;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
public class InventoryPlayer implements IInventory {
    public ItemStack[] mainInventory = new ItemStack[36];
    public ItemStack[] armorInventory = new ItemStack[4];
    public int currentItem = 0;
    public EntityPlayer player;
    private ItemStack itemStack; // currently held (picked up) item
    public InventoryPlayer(EntityPlayer player) { this.player = player; }
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack stack) { this.itemStack = stack; }
    public ItemStack getCurrentItem() { return currentItem >= 0 && currentItem < mainInventory.length ? mainInventory[currentItem] : null; }
    public boolean addItemStackToInventory(ItemStack stack) { return false; }
    @Override public int getSizeInventory() { return mainInventory.length; }
    @Override public ItemStack getStackInSlot(int i) { return i < mainInventory.length ? mainInventory[i] : (i - mainInventory.length < armorInventory.length ? armorInventory[i - mainInventory.length] : null); }
    @Override public ItemStack decrStackSize(int i, int count) { return null; }
    @Override public ItemStack getStackInSlotOnClosing(int i) { return null; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i < mainInventory.length) mainInventory[i] = s; }
    @Override public String getInvName() { return "inventory"; }
    @Override public boolean isInvNameLocalized() { return false; }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void onInventoryChanged() {}
    @Override public boolean isUseableByPlayer(EntityPlayer p) { return true; }
    @Override public void openChest() {}
    @Override public void closeChest() {}
    @Override public boolean isItemValidForSlot(int i, ItemStack s) { return true; }
}
