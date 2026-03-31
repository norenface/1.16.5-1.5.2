package net.minecraft.nbt;
import java.io.*;
public class CompressedStreamTools {
    public static NBTTagCompound readCompressed(InputStream stream) throws IOException { return new NBTTagCompound(); }
    public static void writeCompressed(NBTTagCompound tag, OutputStream stream) throws IOException {}
    public static NBTTagCompound read(DataInput input) throws IOException { return new NBTTagCompound(); }
    public static void write(NBTTagCompound tag, DataOutput output) throws IOException {}
    public static byte[] compress(NBTTagCompound tag) throws IOException { return new byte[0]; }
    public static NBTTagCompound decompress(byte[] data) throws IOException { return new NBTTagCompound(); }
}
