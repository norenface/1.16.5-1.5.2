package net.minecraft.world;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.storage.MapStorage;
public class World {
    public boolean field_72995_K;   // isRemote
    public MapStorage field_72988_C; // mapStorage
    public TileEntity func_72796_p(int x, int y, int z) { return null; } // getBlockTileEntity
    public void func_72698_d(int x, int y, int z) {}  // markBlockForUpdate
    public long func_72820_w() { return 0L; }          // getTotalWorldTime
    public void func_72956_a(EntityPlayer player, String sound, float volume, float pitch) {} // playSoundAtEntity
    public void func_72901_a(double x, double y, double z, String sound, float volume, float pitch) {} // playSoundEffect
    public int func_72798_a(int x, int y, int z) { return 0; }  // getBlockId
    public int func_72805_g(int x, int y, int z) { return 0; }  // getBlockMetadata
    public boolean func_72892_a(int x, int y, int z, int blockId, int meta, int flags) { return false; } // setBlock
}
