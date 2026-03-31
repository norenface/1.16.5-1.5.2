package net.minecraft.network.packet;
import java.io.*;
public class Packet250CustomPayload extends Packet {
    public String channel;
    public byte[] data;
    public int length;
    public Packet250CustomPayload() {}
    public Packet250CustomPayload(String channel, byte[] data) { this.channel = channel; this.data = data; this.length = data != null ? data.length : 0; }
    @Override public void readPacketData(DataInputStream d) throws IOException {}
    @Override public void writePacketData(DataOutputStream d) throws IOException {}
    @Override public int getPacketSize() { return 0; }
}
