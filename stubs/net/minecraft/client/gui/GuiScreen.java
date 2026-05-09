package net.minecraft.client.gui;
import net.minecraft.client.Minecraft;
public abstract class GuiScreen {
    public int width, height;
    public FontRenderer fontRenderer = new FontRenderer();
    protected Minecraft mc = Minecraft.getMinecraft();
    public void func_73866_w() {}                                              // initGui
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {}   // drawScreen
    public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {}
    public void drawRect(int x1, int y1, int x2, int y2, int color) {}
    public void drawGradientRect(int x1, int y1, int x2, int y2, int color1, int color2) {}
    public void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {}
    protected void func_73864_a(int mouseX, int mouseY, int button) {}        // mouseClicked
    protected void func_73869_b(int mouseX, int mouseY, int button) {}        // mouseMovedOrUp
    public boolean func_73871_o() { return false; }                            // doesGuiPauseGame
    protected void func_73869_a(char typedChar, int keyCode) {}               // keyTyped
    protected void func_73867_d() {}                                           // handleMouseInput
    public void onGuiClosed() {}
    public void setWorldAndResolution(Minecraft mc, int width, int height) { this.mc = mc; this.width = width; this.height = height; }
}
