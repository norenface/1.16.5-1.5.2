package net.minecraft.block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
public abstract class BlockContainer extends Block {
    public BlockContainer(int id, Material material) { super(id, material); }
    @Override public boolean hasTileEntity(int metadata) { return true; }
    @Override public abstract TileEntity createNewTileEntity(World world);
}
