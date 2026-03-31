package net.minecraft.entity.player;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
public abstract class EntityPlayer implements cpw.mods.fml.common.network.Player {
    public InventoryPlayer inventory = new InventoryPlayer(this);
    public Container openContainer;
    public World worldObj;
    public boolean onGround;
    public double posX, posY, posZ;
    public String username;
    public boolean isSneaking() { return false; }
    public String getEntityName() { return username != null ? username : "Player"; }
    public void openGui(Object mod, int guiId, World world, int x, int y, int z) {}
    public ItemStack getCurrentEquippedItem() { return inventory.getCurrentItem(); }
    public void dropPlayerItemWithRandomChoice(ItemStack stack, boolean random) {}
    public void dropPlayerItem(ItemStack stack) {}
    public void addChatMessage(String msg) {}
    public boolean isEntityAlive() { return true; }
    public abstract EntityPlayer getCommandSenderName();
}
