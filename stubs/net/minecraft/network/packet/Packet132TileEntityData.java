package net.minecraft.network.packet;
import net.minecraft.nbt.NBTTagCompound;
import java.io.*;
public class Packet132TileEntityData extends Packet {
    public int xPosition, yPosition, zPosition;
    public int actionType;
    public NBTTagCompound customParam1;
    public Packet132TileEntityData() {}
    public Packet132TileEntityData(int x, int y, int z, int type, NBTTagCompound nbt) {
        this.xPosition = x; this.yPosition = y; this.zPosition = z;
        this.actionType = type; this.customParam1 = nbt;
    }
    @Override public void readPacketData(DataInputStream data) throws IOException {}
    @Override public void writePacketData(DataOutputStream data) throws IOException {}
    @Override public int getPacketSize() { return 0; }
}
