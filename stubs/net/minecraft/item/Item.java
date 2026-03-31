package net.minecraft.item;
public class Item {
    public int itemID;
    public static Item[] itemsList = new Item[32000];
    public int getMaxStackSize() { return 64; }
    public String getUnlocalizedName(ItemStack stack) { return "item.unknown"; }
    public String getUnlocalizedName() { return "item.unknown"; }
    public boolean isDamageable() { return false; }
    public int getMaxDamage() { return 0; }
}
