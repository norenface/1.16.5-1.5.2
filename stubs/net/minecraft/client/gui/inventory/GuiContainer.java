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
    @Override public void initGui() { this.guiLeft = (this.width - this.xSize) / 2; this.guiTop = (this.height - this.ySize) / 2; }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) { drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY); drawGuiContainerForegroundLayer(mouseX, mouseY); }
    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY);
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}
    @Override protected void mouseClicked(int mouseX, int mouseY, int button) {}
    @Override protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {}
    @Override protected void handleMouseInput() {}
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int mode) {}
    @Override public boolean doesGuiPauseGame() { return false; }
}
