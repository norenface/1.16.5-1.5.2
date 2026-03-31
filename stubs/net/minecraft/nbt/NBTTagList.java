package net.minecraft.nbt;
import java.io.*;
import java.util.*;
public class NBTTagList extends NBTBase {
    private List<NBTBase> list = new ArrayList<NBTBase>();
    public void appendTag(NBTBase tag) { list.add(tag); }
    public NBTBase tagAt(int index) { return list.get(index); }
    public int tagCount() { return list.size(); }
    @Override public String getName() { return ""; }
    @Override public NBTBase copy() { return new NBTTagList(); }
    @Override public byte getId() { return 9; }
    @Override public void write(DataOutput out) throws IOException {}
    @Override public void read(DataInput in) throws IOException {}
}
