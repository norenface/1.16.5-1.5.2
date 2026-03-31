package net.minecraft.client.gui;
public class FontRenderer {
    public int drawString(String text, int x, int y, int color) { return x + text.length() * 6; }
    public int drawStringWithShadow(String text, int x, int y, int color) { return x + text.length() * 6; }
    public int getStringWidth(String text) { return text.length() * 6; }
    public int FONT_HEIGHT = 9;
    public void drawSplitString(String text, int x, int y, int wrapWidth, int color) {}
}
