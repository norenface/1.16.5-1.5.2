package net.minecraft.client;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.texture.RenderEngine;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.audio.SoundManager;
public class Minecraft {
    private static Minecraft instance;
    public EntityPlayer thePlayer;
    public RenderEngine renderEngine = new RenderEngine();
    public SoundManager sndManager = new SoundManager();
    public int displayWidth = 854;
    public int displayHeight = 480;
    public static Minecraft getMinecraft() { if (instance == null) instance = new Minecraft(); return instance; }
    public NetClientHandler getNetHandler() { return new NetClientHandler(); }
    public void displayGuiScreen(net.minecraft.client.gui.GuiScreen gui) {}
}
