package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
public class InventoryBasic implements IInventory {
    private ItemStack[] contents;
    private String name;
    private boolean customName;
    public InventoryBasic(String name, boolean customName, int size) {
        this.name = name; this.customName = customName; this.contents = new ItemStack[size];
    }
    @Override public int getSizeInventory() { return contents.length; }
    @Override public ItemStack getStackInSlot(int i) { return i >= 0 && i < contents.length ? contents[i] : null; }
    @Override public ItemStack decrStackSize(int i, int count) { if (contents[i] == null) return null; if (contents[i].stackSize <= count) { ItemStack s = contents[i]; contents[i] = null; return s; } ItemStack s = contents[i].copy(); s.stackSize = count; contents[i].stackSize -= count; return s; }
    @Override public ItemStack getStackInSlotOnClosing(int i) { return null; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i >= 0 && i < contents.length) contents[i] = s; }
    @Override public String getInvName() { return name; }
    @Override public boolean isInvNameLocalized() { return customName; }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void onInventoryChanged() {}
    @Override public boolean isUseableByPlayer(EntityPlayer p) { return true; }
    @Override public void openChest() {}
    @Override public void closeChest() {}
    @Override public boolean isItemValidForSlot(int i, ItemStack s) { return true; }
}
