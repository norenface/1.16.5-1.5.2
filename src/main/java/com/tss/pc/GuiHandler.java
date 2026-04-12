package com.tss.pc;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        // 🌟 サーバー側が呼ばれたら絶対に出るデバッグ
        System.out.println("=== [CRITICAL SERVER] getServerGuiElement called! ID: " + ID + " ===");

        TileEntity te = world.func_72796_p(x, y, z);
        if (te instanceof ComputerBlockEntity) {
            System.out.println("=== [SERVER] Success: ComputerContainer created. ===");
            return new ComputerContainer(player.field_71071_by, (ComputerBlockEntity) te);
        }
        System.out.println("=== [SERVER ERROR] TileEntity not found at " + x + "," + y + "," + z + " ===");
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.func_72796_p(x, y, z);
        if (te instanceof ComputerBlockEntity) {
            return new ComputerScreen(player.field_71071_by, (ComputerBlockEntity) te);
        }
        return null;
    }
}