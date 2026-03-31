package com.tss.pc;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Init;    // 🌟 これを使用
import cpw.mods.fml.common.Mod.PreInit; // 🌟 これを使用
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

// 🌟 NetworkModアノテーションはGUIの同期に必須です
@Mod(modid = TSSPCMod.MOD_ID, name = "TSS PC Mod", version = "1.0.0")
@NetworkMod(clientSideRequired = true, serverSideRequired = true,channels = {"TSS_PC", "tss_pc"},packetHandler = PacketHandler.class)
public class TSSPCMod {
    public static final String MOD_ID = "tss_pc";
    public static final int GUI_ID = 1;

    @Instance(MOD_ID)
    public static TSSPCMod instance;

    public static Block computerBlock;

    @PreInit // 🌟 EventHandler ではなく PreInit
    public void preInit(FMLPreInitializationEvent event) {
        computerBlock = new ComputerBlock(2500, Material.iron)
                .setUnlocalizedName("pc_block")
                .setCreativeTab(CreativeTabs.tabDecorations);

        GameRegistry.registerBlock(computerBlock, "pc_block");
        GameRegistry.registerTileEntity(ComputerBlockEntity.class, "TSSComputerTile");

        System.out.println("DEBUG: [TSSPC] PreInit Completed.");
    }

    @Init // 🌟 EventHandler ではなく Init
    public void load(FMLInitializationEvent event) {
        // 🌟 NetworkRegistryの登録で 'instance' ではなく 'this' を試してください
        NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());

        // 🌟 これがログに出るかどうかが「生命線」です
        System.out.println("DEBUG: [TSSPC] GuiHandler REGISTERED AT INIT!");
    }
}