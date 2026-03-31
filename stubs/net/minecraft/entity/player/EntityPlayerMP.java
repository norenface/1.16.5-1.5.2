package net.minecraft.entity.player;
import net.minecraft.inventory.Container;
public class EntityPlayerMP extends EntityPlayer {
    public void sendContainerToPlayer(Container container) {}
    public void updateHeldItem() {}
    @Override public EntityPlayer getCommandSenderName() { return this; }
}
