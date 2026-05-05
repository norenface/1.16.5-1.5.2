package com.tss.pc;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(modid = TSSPCMod.MOD_ID, name = "TSS PC Mod", version = "1.0.0")
@NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = {"TSS_PC", "tss_pc"}, packetHandler = PacketHandler.class)
public class TSSPCMod {
    public static final String MOD_ID = "tss_pc";
    public static final int GUI_ID = 1;

    @Instance(MOD_ID)
    public static TSSPCMod instance;

    public static Block computerBlock;

    @PreInit
    public void preInit(FMLPreInitializationEvent event) {
        computerBlock = new ComputerBlock(500, Material.field_76246_e);

        safeSetBlockName(computerBlock, "pc_block");
        safeSetCreativeTab(computerBlock);

        GameRegistry.registerBlock(computerBlock, "pc_block");
        GameRegistry.registerTileEntity(ComputerBlockEntity.class, "TSSComputerTile");

        System.out.println("DEBUG: [TSSPC] PreInit Completed.");
    }

    @Init
    public void load(FMLInitializationEvent event) {
        NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
        System.out.println("DEBUG: [TSSPC] GuiHandler REGISTERED AT INIT!");
    }

    // Use reflection to call setUnlocalizedName regardless of the actual SRG method name.
    private static void safeSetBlockName(Block block, String name) {
        // 1. Try human-readable name (Forge may have added it directly)
        String[] knownNames = {"setUnlocalizedName", "func_77208_a", "func_149663_c"};
        for (String mname : knownNames) {
            try {
                Method m = Block.class.getMethod(mname, String.class);
                m.invoke(block, name);
                System.out.println("DEBUG: [TSSPC] Block name set via: " + mname);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                System.out.println("DEBUG: [TSSPC] safeSetBlockName error (" + mname + "): " + e);
            }
        }

        // 2. Fallback: find any func_* method with (String)->Block signature
        for (Method m : Block.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            Class<?> ret = m.getReturnType();
            String mname = m.getName();
            if (p.length == 1 && p[0] == String.class && mname.startsWith("func_") &&
                    (Block.class.isAssignableFrom(ret) || ret == void.class)) {
                try {
                    m.invoke(block, name);
                    System.out.println("DEBUG: [TSSPC] Block name set via (found): " + mname);
                    return;
                } catch (Exception e) {
                    System.out.println("DEBUG: [TSSPC] safeSetBlockName fallback error (" + mname + "): " + e);
                }
            }
        }

        // 3. Diagnostic: list all String-param methods found
        System.out.println("DEBUG: [TSSPC] WARNING: Could not set block name. Candidates:");
        for (Method m : Block.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 1 && p[0] == String.class) {
                System.out.println("  Block." + m.getName() + "(String) -> " + m.getReturnType().getSimpleName());
            }
        }
    }

    // Use reflection to call setCreativeTab regardless of the actual SRG method name.
    private static void safeSetCreativeTab(Block block) {
        try {
            // Find the creative tab instance (try SRG field name first, then MCP names)
            Object tab = null;
            String[] fieldNames = {"field_78025_g", "tabBlock", "field_78027_e", "tabDecorations", "field_78026_f", "tabMisc"};
            String foundFieldName = null;
            for (String fname : fieldNames) {
                try {
                    Field f = CreativeTabs.class.getField(fname);
                    tab = f.get(null);
                    foundFieldName = fname;
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }

            if (tab == null) {
                System.out.println("DEBUG: [TSSPC] WARNING: Could not find a CreativeTabs field. Candidates:");
                for (Field f : CreativeTabs.class.getFields()) {
                    if (CreativeTabs.class.isAssignableFrom(f.getType())) {
                        System.out.println("  CreativeTabs." + f.getName());
                    }
                }
                return;
            }
            System.out.println("DEBUG: [TSSPC] Using creative tab field: " + foundFieldName);

            // Call setCreativeTab (try known names, then signature search)
            String[] methodNames = {"setCreativeTab", "func_71905_a", "func_149626_f"};
            for (String mname : methodNames) {
                try {
                    Method m = Block.class.getMethod(mname, CreativeTabs.class);
                    m.invoke(block, tab);
                    System.out.println("DEBUG: [TSSPC] Creative tab set via: " + mname);
                    return;
                } catch (NoSuchMethodException ignored) {
                } catch (Exception e) {
                    System.out.println("DEBUG: [TSSPC] safeSetCreativeTab error (" + mname + "): " + e);
                }
            }

            // Fallback: find by signature
            for (Method m : Block.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                Class<?> ret = m.getReturnType();
                if (p.length == 1 && CreativeTabs.class.isAssignableFrom(p[0]) &&
                        (Block.class.isAssignableFrom(ret) || ret == void.class)) {
                    try {
                        m.invoke(block, tab);
                        System.out.println("DEBUG: [TSSPC] Creative tab set via (found): " + m.getName());
                        return;
                    } catch (Exception e) {
                        System.out.println("DEBUG: [TSSPC] safeSetCreativeTab fallback error: " + e);
                    }
                }
            }

            System.out.println("DEBUG: [TSSPC] WARNING: Could not call setCreativeTab!");
        } catch (Exception e) {
            System.out.println("DEBUG: [TSSPC] safeSetCreativeTab outer error: " + e);
        }
    }
}