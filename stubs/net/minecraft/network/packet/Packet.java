package net.minecraft.network.packet;
import java.io.*;
public abstract class Packet {
    public abstract void readPacketData(DataInputStream data) throws IOException;
    public abstract void writePacketData(DataOutputStream data) throws IOException;
    public abstract int getPacketSize();
}
