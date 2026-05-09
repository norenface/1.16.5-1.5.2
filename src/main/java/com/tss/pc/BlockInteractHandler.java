package com.tss.pc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class BlockInteractHandler {

    @ForgeSubscribe
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        EntityPlayer player = event.entityPlayer;
        World world = player.field_70170_p;
        if (world == null) return;

        int blockId = world.func_72798_a(event.x, event.y, event.z);
        if (blockId != TSSPCMod.COMPUTER_BLOCK_ID) return;

        if (!world.field_72995_K && TSSPCMod.instance != null) {
            player.openGui(TSSPCMod.instance, TSSPCMod.GUI_ID, world, event.x, event.y, event.z);
            event.setCanceled(true);
        }
    }
}
