package net.minecraft.client.gui.inventory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
public abstract class GuiContainer extends GuiScreen {
    public int xSize = 176;
    public int ySize = 166;
    public int guiLeft;
    public int guiTop;
    public Container field_73875_a; // inventorySlots
    public GuiContainer(Container container) { this.field_73875_a = container; }
    @Override public void func_73866_w() { this.guiLeft = (this.width - this.xSize) / 2; this.guiTop = (this.height - this.ySize) / 2; } // initGui
    @Override public void func_73863_a(int mouseX, int mouseY, float partialTicks) { func_74185_a(partialTicks, mouseX, mouseY); func_74184_a(mouseX, mouseY); } // drawScreen
    protected abstract void func_74185_a(float partialTicks, int mouseX, int mouseY); // drawGuiContainerBackgroundLayer
    protected void func_74184_a(int mouseX, int mouseY) {}                             // drawGuiContainerForegroundLayer
    @Override protected void func_73864_a(int mouseX, int mouseY, int button) {}       // mouseClicked
    @Override protected void func_73869_b(int mouseX, int mouseY, int button) {}       // mouseMovedOrUp
    @Override protected void func_73867_d() {}                                          // handleMouseInput
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int mode) {}
    @Override public boolean func_73871_o() { return false; }                           // doesGuiPauseGame
}
