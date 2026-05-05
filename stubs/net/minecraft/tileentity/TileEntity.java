package net.minecraft.tileentity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.world.World;
public class TileEntity {
    public World worldObj;         // worldObj (Forge exposes as human-readable)
    public int field_70329_l, field_70330_k, field_70328_m; // xCoord, yCoord, zCoord
    public boolean field_70332_k; // tileEntityInvalid
    public void func_70307_a(NBTTagCompound nbt) {} // readFromNBT
    public void func_70310_b(NBTTagCompound nbt) {} // writeToNBT
    public Packet func_70111_a() { return null; }   // getDescriptionPacket
    public void func_73109_a(INetworkManager net, Packet132TileEntityData pkt) {} // onDataPacket
    public void func_70296_a() {}  // onInventoryChanged
    public void markDirty() {}
    public boolean func_70313_a() { return true; }  // canUpdate
    public void func_70316_g() {}  // updateEntity
    public void invalidate() { field_70332_k = true; }
    public void validate() { field_70332_k = false; }
    public boolean isInvalid() { return field_70332_k; }
}
