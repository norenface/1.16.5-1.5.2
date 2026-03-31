package net.minecraft.client.gui;
public class GuiTextField {
    private String text = "";
    private boolean focused = false;
    private boolean visible = true;
    private int maxLength = 32;
    public GuiTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {}
    public void setText(String text) { this.text = text != null ? text : ""; }
    public String getText() { return text; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isFocused() { return focused; }
    public void drawTextBox() {}
    public boolean textboxKeyTyped(char c, int keyCode) { return false; }
    public void mouseClicked(int mouseX, int mouseY, int button) {}
    public void setMaxStringLength(int length) { this.maxLength = length; }
    public boolean getVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setCanLoseFocus(boolean canLose) {}
    public void updateCursorCounter() {}
}
