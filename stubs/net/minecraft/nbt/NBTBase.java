package net.minecraft.nbt;
public abstract class NBTBase {
    public abstract String getName();
    public abstract NBTBase copy();
    public abstract byte getId();
    public abstract void write(java.io.DataOutput output) throws java.io.IOException;
    public abstract void read(java.io.DataInput input) throws java.io.IOException;
}
