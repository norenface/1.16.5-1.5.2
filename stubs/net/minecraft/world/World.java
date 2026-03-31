package net.minecraft.world;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.storage.MapStorage;
public class World {
    public boolean isRemote;
    public MapStorage mapStorage;
    public TileEntity getBlockTileEntity(int x, int y, int z) { return null; }
    public void markBlockForUpdate(int x, int y, int z) {}
    public long getTotalWorldTime() { return 0L; }
    public void playSoundAtEntity(EntityPlayer player, String sound, float volume, float pitch) {}
    public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {}
    public int getBlockId(int x, int y, int z) { return 0; }
    public int getBlockMetadata(int x, int y, int z) { return 0; }
    public boolean setBlock(int x, int y, int z, int blockId, int meta, int flags) { return false; }
}
