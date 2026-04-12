package net.minecraft.creativetab;
public abstract class CreativeTabs {
    public static final CreativeTabs field_78027_e = new CreativeTabs("decorations") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } }; // tabDecorations
    public static final CreativeTabs field_78025_g = new CreativeTabs("buildingBlocks") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } }; // tabBlock
    public static final CreativeTabs field_78030_b = new CreativeTabs("tools") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } }; // tabTools
    public static final CreativeTabs field_78026_f = new CreativeTabs("misc") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } }; // tabMisc
    private final String label;
    public CreativeTabs(String label) { this.label = label; }
    public String getTranslatedTabLabel() { return label; }
    public abstract net.minecraft.item.ItemStack getIconItemStack();
}
