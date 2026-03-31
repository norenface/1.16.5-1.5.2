package net.minecraft.nbt;
import java.util.*;
import java.io.*;
public class NBTTagCompound extends NBTBase {
    private Map<String, NBTBase> tagMap = new HashMap<String, NBTBase>();
    public void setString(String key, String value) {}
    public String getString(String key) { return ""; }
    public void setTag(String key, NBTBase value) {}
    public NBTBase getTag(String key) { return null; }
    public NBTTagCompound getCompoundTag(String key) { return new NBTTagCompound(); }
    public void setCompoundTag(String key, NBTTagCompound value) {}
    public void setInteger(String key, int value) {}
    public int getInteger(String key) { return 0; }
    public void setLong(String key, long value) {}
    public long getLong(String key) { return 0L; }
    public void setByte(String key, byte value) {}
    public byte getByte(String key) { return 0; }
    public void setShort(String key, short value) {}
    public short getShort(String key) { return 0; }
    public void setFloat(String key, float value) {}
    public float getFloat(String key) { return 0f; }
    public void setBoolean(String key, boolean value) {}
    public boolean getBoolean(String key) { return false; }
    public boolean hasKey(String key) { return false; }
    public void removeTag(String key) { tagMap.remove(key); }
    public Collection<NBTBase> getTags() { return tagMap.values(); }
    public NBTTagList getTagList(String key) { return new NBTTagList(); }
    public boolean isEmpty() { return tagMap.isEmpty(); }
    @Override public String getName() { return ""; }
    @Override public NBTBase copy() { return new NBTTagCompound(); }
    @Override public byte getId() { return 10; }
    @Override public void write(DataOutput out) throws IOException {}
    @Override public void read(DataInput in) throws IOException {}
    @Override public String toString() { return tagMap.toString(); }
    @Override public boolean equals(Object o) { return o instanceof NBTTagCompound && tagMap.equals(((NBTTagCompound)o).tagMap); }
    @Override public int hashCode() { return tagMap.hashCode(); }
}
