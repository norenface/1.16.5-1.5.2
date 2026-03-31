package net.minecraft.tileentity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.world.World;
public class TileEntity {
    public World worldObj;
    public int xCoord, yCoord, zCoord;
    public boolean tileEntityInvalid;
    public void readFromNBT(NBTTagCompound nbt) {}
    public void writeToNBT(NBTTagCompound nbt) {}
    public Packet getDescriptionPacket() { return null; }
    public void onDataPacket(INetworkManager net, Packet132TileEntityData pkt) {}
    public void onInventoryChanged() {}
    public void markDirty() {}
    public boolean canUpdate() { return true; }
    public void updateEntity() {}
    public void invalidate() { tileEntityInvalid = true; }
    public void validate() { tileEntityInvalid = false; }
    public boolean isInvalid() { return tileEntityInvalid; }
}
