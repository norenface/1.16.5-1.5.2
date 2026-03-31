package net.minecraft.creativetab;
public abstract class CreativeTabs {
    public static final CreativeTabs tabDecorations = new CreativeTabs("decorations") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } };
    public static final CreativeTabs tabBlock = new CreativeTabs("buildingBlocks") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } };
    public static final CreativeTabs tabTools = new CreativeTabs("tools") { public net.minecraft.item.ItemStack getIconItemStack() { return null; } };
    private final String label;
    public CreativeTabs(String label) { this.label = label; }
    public String getTranslatedTabLabel() { return label; }
    public abstract net.minecraft.item.ItemStack getIconItemStack();
}
