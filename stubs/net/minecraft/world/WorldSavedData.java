package net.minecraft.world;
import net.minecraft.nbt.NBTTagCompound;
public abstract class WorldSavedData {
    private final String mapName;
    private boolean dirty;
    public WorldSavedData(String name) { this.mapName = name; }
    public abstract void func_70307_a(NBTTagCompound nbt);  // readFromNBT
    public abstract void func_70310_b(NBTTagCompound nbt);  // writeToNBT
    public void markDirty() { this.dirty = true; }
    public boolean isDirty() { return dirty; }
    public String getMapName() { return mapName; }
    public String getName() { return mapName; }
}
