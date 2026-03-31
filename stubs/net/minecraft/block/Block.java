package net.minecraft.block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
public class Block {
    public static final Block[] blocksList = new Block[4096];
    public static Block.SoundType soundMetalFootstep = new SoundType("metal", 1.0f, 1.0f);
    public static Block.SoundType soundGravelFootstep = new SoundType("gravel", 1.0f, 1.0f);
    public static Block.SoundType soundStoneFootstep = new SoundType("stone", 1.0f, 1.0f);
    public static Block.SoundType soundWoodFootstep = new SoundType("wood", 1.0f, 1.0f);
    protected Material blockMaterial;
    protected Icon blockIcon;
    protected int blockID;
    public float blockHardness;
    public float blockResistance;
    public Block(int id, Material material) { this.blockID = id; this.blockMaterial = material; }
    public Block setHardness(float hardness) { this.blockHardness = hardness; return this; }
    public Block setResistance(float resistance) { this.blockResistance = resistance; return this; }
    public Block setStepSound(SoundType soundType) { return this; }
    public Block setUnlocalizedName(String name) { return this; }
    public Block setCreativeTab(CreativeTabs tab) { return this; }
    public boolean hasTileEntity(int metadata) { return false; }
    public TileEntity createNewTileEntity(World world) { return null; }
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) { return false; }
    public boolean canPlaceBlockAt(World world, int x, int y, int z) { return true; }
    public Icon getIcon(int side, int metadata) { return blockIcon; }
    public void registerIcons(IconRegister iconRegister) {}
    public static class SoundType {
        public String soundName;
        public float volume, pitch;
        public SoundType(String name, float volume, float pitch) { this.soundName = name; this.volume = volume; this.pitch = pitch; }
    }
}
