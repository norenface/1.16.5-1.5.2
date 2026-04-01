package net.minecraft.entity.player;
import net.minecraft.inventory.Container;
public class EntityPlayerMP extends EntityPlayer {
    public void func_70483_a(Container container) {} // sendContainerToPlayer
    public void updateHeldItem() {}
    @Override public EntityPlayer getCommandSenderName() { return this; }
}
