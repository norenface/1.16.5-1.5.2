package net.minecraft.item;
import net.minecraft.nbt.NBTTagCompound;
public class ItemStack {
    public int itemID;
    public int stackSize;
    public int itemDamage;
    public NBTTagCompound stackTagCompound;
    public ItemStack() {}
    public ItemStack(int id, int count, int damage) { this.itemID = id; this.stackSize = count; this.itemDamage = damage; }
    public ItemStack(Item item) { this.itemID = item.itemID; this.stackSize = 1; }
    public ItemStack(Item item, int count, int damage) { this.itemID = item.itemID; this.stackSize = count; this.itemDamage = damage; }
    public ItemStack copy() { ItemStack s = new ItemStack(); s.itemID = itemID; s.stackSize = stackSize; s.itemDamage = itemDamage; if (stackTagCompound != null) s.stackTagCompound = (NBTTagCompound) stackTagCompound.copy(); return s; }
    public void writeToNBT(NBTTagCompound nbt) {}
    public static ItemStack loadItemStackFromNBT(NBTTagCompound nbt) { return new ItemStack(); }
    public String getDisplayName() { return "Unknown"; }
    public int getItemDamage() { return itemDamage; }
    public int getMaxStackSize() { return 64; }
    public boolean isItemEqual(ItemStack other) { return other != null && itemID == other.itemID && itemDamage == other.itemDamage; }
    public static boolean areItemStackTagsEqual(ItemStack s1, ItemStack s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        if (s1.stackTagCompound == null && s2.stackTagCompound == null) return true;
        if (s1.stackTagCompound == null || s2.stackTagCompound == null) return false;
        return s1.stackTagCompound.equals(s2.stackTagCompound);
    }
    public boolean hasTagCompound() { return stackTagCompound != null; }
    public NBTTagCompound getTagCompound() { return stackTagCompound; }
    public void setTagCompound(NBTTagCompound tag) { this.stackTagCompound = tag; }
}
