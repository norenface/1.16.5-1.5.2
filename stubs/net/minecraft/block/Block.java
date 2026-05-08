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
    public static Block.SoundType field_111230_b = new SoundType("metal", 1.0f, 1.0f);   // soundMetalFootstep
    public static Block.SoundType field_111232_r = new SoundType("gravel", 1.0f, 1.0f);   // soundGravelFootstep
    public static Block.SoundType field_111231_a = new SoundType("stone", 1.0f, 1.0f);    // soundStoneFootstep
    public static Block.SoundType field_111233_q = new SoundType("wood", 1.0f, 1.0f);     // soundWoodFootstep
    protected Material blockMaterial;
    protected Icon blockIcon;
    protected int blockID;
    public float blockHardness;
    public float blockResistance;
    public Block(int id, Material material) { this.blockID = id; this.blockMaterial = material; }
    public Block func_71912_a(float hardness) { this.blockHardness = hardness; return this; }  // setHardness
    public Block func_71909_a(float resistance) { this.blockResistance = resistance; return this; } // setResistance
    public Block func_71903_g(SoundType soundType) { return this; }  // setStepSound
    public Block func_71908_c(String name) { return this; }          // setUnlocalizedName
    public Block func_71905_a(CreativeTabs tab) { return this; }     // setCreativeTab
    public boolean func_71880_b(int metadata) { return false; }      // hasTileEntity
    public TileEntity func_71916_a(World world) { return null; }     // createNewTileEntity (vanilla SRG)
    public TileEntity createTileEntity(World world, int metadata) { return null; } // Forge-added
    public boolean func_71930_a(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) { return false; } // onBlockActivated (SRG)
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) { return false; } // onBlockActivated (MCP)
    public boolean func_71917_g(World world, int x, int y, int z) { return true; } // canPlaceBlockAt
    public Icon func_71874_a(int side, int metadata) { return blockIcon; }  // getIcon
    public void func_94341_s(IconRegister iconRegister) {}           // registerIcons
    public static class SoundType {
        public String soundName;
        public float volume, pitch;
        public SoundType(String name, float volume, float pitch) { this.soundName = name; this.volume = volume; this.pitch = pitch; }
    }
}
