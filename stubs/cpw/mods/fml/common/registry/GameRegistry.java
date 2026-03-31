package cpw.mods.fml.common.registry;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
public class GameRegistry {
    public static void registerBlock(Block block, String name) {}
    public static void registerBlock(Block block, Class<?> itemClass, String name) {}
    public static void registerTileEntity(Class<? extends TileEntity> cls, String id) {}
}
