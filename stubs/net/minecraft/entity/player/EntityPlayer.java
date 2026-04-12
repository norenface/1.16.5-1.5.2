package net.minecraft.entity.player;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
public abstract class EntityPlayer implements cpw.mods.fml.common.network.Player {
    public InventoryPlayer field_71071_by = new InventoryPlayer(this); // inventory
    public Container field_71069_bj;   // openContainer
    public World field_70170_p;        // worldObj
    public boolean onGround;
    public double posX, posY, posZ;
    public String username;
    public boolean func_70093_af() { return false; }  // isSneaking
    public String func_70005_c() { return username != null ? username : "Player"; } // getEntityName
    public void openGui(Object mod, int guiId, World world, int x, int y, int z) {}
    public ItemStack func_70694_bm() { return field_71071_by.func_70445_o(); } // getCurrentEquippedItem / getHeldItem
    public void func_70998_q(ItemStack stack, boolean random) {} // dropPlayerItemWithRandomChoice
    public void func_71018_a(ItemStack stack) {}  // dropPlayerItem
    public void func_70092_c(String msg) {}       // addChatMessage
    public boolean isEntityAlive() { return true; }
    public abstract EntityPlayer getCommandSenderName();
}
