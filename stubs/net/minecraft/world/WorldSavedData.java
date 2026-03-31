package net.minecraft.world;
import net.minecraft.nbt.NBTTagCompound;
public abstract class WorldSavedData {
    private final String mapName;
    private boolean dirty;
    public WorldSavedData(String name) { this.mapName = name; }
    public abstract void readFromNBT(NBTTagCompound nbt);
    public abstract void writeToNBT(NBTTagCompound nbt);
    public void markDirty() { this.dirty = true; }
    public boolean isDirty() { return dirty; }
    public String getMapName() { return mapName; }
    public String getName() { return mapName; }
}
