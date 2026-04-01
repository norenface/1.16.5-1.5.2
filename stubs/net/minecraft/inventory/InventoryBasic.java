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
    @Override public int func_70302_a() { return contents.length; }
    @Override public ItemStack func_70301_a(int i) { return i >= 0 && i < contents.length ? contents[i] : null; }
    @Override public ItemStack func_70298_a(int i, int count) { if (contents[i] == null) return null; if (contents[i].stackSize <= count) { ItemStack s = contents[i]; contents[i] = null; return s; } ItemStack s = contents[i].copy(); s.stackSize = count; contents[i].stackSize -= count; return s; }
    @Override public ItemStack func_70304_a(int i) { return null; }
    @Override public void func_70299_a(int i, ItemStack s) { if (i >= 0 && i < contents.length) contents[i] = s; }
    @Override public String func_70303_a() { return name; }
    @Override public boolean func_71125_a() { return customName; }
    @Override public int func_70297_a() { return 64; }
    @Override public void func_70296_a() {}
    @Override public boolean func_70300_a(EntityPlayer p) { return true; }
    @Override public void func_70295_a() {}
    @Override public void func_70305_a() {}
    @Override public boolean func_94041_b(int i, ItemStack s) { return true; }
}
