package net.minecraft.client.gui;
import net.minecraft.client.Minecraft;
public abstract class GuiScreen {
    public int width, height;
    public FontRenderer fontRenderer = new FontRenderer();
    protected Minecraft mc = Minecraft.getMinecraft();
    public void initGui() {}
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {}
    public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {}
    public void drawRect(int x1, int y1, int x2, int y2, int color) {}
    public void drawGradientRect(int x1, int y1, int x2, int y2, int color1, int color2) {}
    public void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {}
    protected void mouseClicked(int mouseX, int mouseY, int button) {}
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {}
    public boolean doesGuiPauseGame() { return false; }
    protected void keyTyped(char typedChar, int keyCode) {}
    protected void handleMouseInput() {}
    public void onGuiClosed() {}
    public void setWorldAndResolution(Minecraft mc, int width, int height) { this.mc = mc; this.width = width; this.height = height; }
}
