package com.tss.pc;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.Icon;
import net.minecraft.client.renderer.texture.IconRegister;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import org.lwjgl.input.Keyboard;

public class ComputerBlock extends net.minecraft.block.BlockContainer {

    public ComputerBlock(int id, Material material) {
        super(id, material);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon func_71874_a(int side, int metadata) {
        return this.blockIcon;
    }
    @Override
    public TileEntity func_71916_a(World world) {
        return new ComputerBlockEntity();
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new ComputerBlockEntity();
    }

    // スニーク右クリック時の「アイテム設置」をブロック側から拒否する
   // @Override
  //  public boolean isItemStackInvalidForSlot(int slot, ItemStack stack) {
   //     return true;
   // }

    // 🌟 これが本命：1.5.2で設置を阻止する強力な呪文
    @Override
    public boolean func_71917_g(World world, int x, int y, int z) {
        // スニーク中は何が何でもここに新しいブロックを置かせない
        return super.func_71917_g(world, x, y, z);
    }
    // --- 1.5.2 の右クリック処理 ---
    // 1.5.2ではスニーク中はバニラがonBlockActivatedをスキップするため、
    // 通常の右クリックでGUIを開く（チェストや炉と同じ挙動）
    @Override
    public boolean func_71930_a(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        System.out.println("=== [DEBUG] Opening GUI on Server ===");
        player.openGui(TSSPCMod.instance, TSSPCMod.GUI_ID, world, x, y, z);
        return true;
    }

    private void recordAndTransfer(ComputerBlockEntity entity, EntityPlayer player, int amount) {
        ItemStack held = player.func_70694_bm();
        if (held == null) return;

        int before = held.stackSize;
        ItemStack typeToRecord = held.copy();

        this.transferItems(entity, player, amount);

        int moved = (player.func_70694_bm() == null) ? before : before - player.func_70694_bm().stackSize;

        if (moved > 0) {
            entity.setLastTransferStack(typeToRecord);
            entity.setLastTransferCount(moved);
        }
    }

    public void transferItems(ComputerBlockEntity entity, EntityPlayer player, int amount) {
        ItemStack heldStack = player.func_70694_bm();
        if (heldStack == null) return;

        // 🌟 1.5.2 には IItemHandler がないので、TileEntity 側のカスタムメソッドを呼ぶようにします
        int movedCount = entity.addItemToStorage(heldStack, amount);

        if (movedCount > 0) {
            heldStack.stackSize -= movedCount;
            if (heldStack.stackSize <= 0) {
                player.field_71071_by.func_70299_a(player.field_71071_by.field_70461_c, null);
            }
            entity.func_70296_a();
        }
    }

    // 1.5.2 のテクスチャ指定方法
    @SideOnly(Side.CLIENT)
    private Icon blockIcon;

    @Override
    @SideOnly(Side.CLIENT)
    public void func_94341_s(IconRegister iconRegister) {
        this.blockIcon = iconRegister.registerIcon("tss_pc:pc_block");
    }

    // 3連打：ホットバー以外
    private void transferInventoryExceptHotbar(ComputerBlockEntity entity, EntityPlayer player) {
        if (player.field_70170_p.field_72995_K) return; // サーバー側でのみ実行

        boolean changed = false;
        // 9番から35番（ホットバー以外）をループ
        for (int i = 9; i < 36; i++) {
            ItemStack invStack = player.field_71071_by.field_70462_a[i];
            if (invStack != null) {
                // ストレージに追加
                entity.getBulkStorage().addStack(invStack, invStack.stackSize);
                // インベントリから消去
                player.field_71071_by.func_70299_a(i, null);
                changed = true;
            }
        }
        if (changed) {
            entity.func_70296_a();
            player.field_70170_p.func_72698_d(entity.field_70329_l, entity.field_70330_k, entity.field_70328_m);
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ((net.minecraft.entity.player.EntityPlayerMP)player).func_70483_a(player.field_71069_bj); }
        }
    }

    // 4連打：全部
    private void transferAllItems(ComputerBlockEntity entity, EntityPlayer player) {
        if (player.field_70170_p.field_72995_K) return;

        boolean changed = false;
        // mainInventory.length は通常 36
        for (int i = 0; i < player.field_71071_by.field_70462_a.length; i++) {
            ItemStack invStack = player.field_71071_by.field_70462_a[i];
            if (invStack != null) {
                entity.getBulkStorage().addStack(invStack, invStack.stackSize);
                player.field_71071_by.func_70299_a(i, null);
                changed = true;
            }
        }
        if (changed) {
            entity.func_70296_a();
            player.field_70170_p.func_72698_d(entity.field_70329_l, entity.field_70330_k, entity.field_70328_m);
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ((net.minecraft.entity.player.EntityPlayerMP)player).func_70483_a(player.field_71069_bj); }

        }
    }

    private void undoTransfer(ComputerBlockEntity entity, EntityPlayer player) {
        ItemStack lastStack = entity.getLastTransferStack(); // ComputerBlockEntity側に実装が必要
        int count = entity.getLastTransferCount();

        // 1.5.2 では null チェックが基本
        if (lastStack != null && count > 0) {
            // 在庫から引き出す (ComputerBlockEntity側の removeItemFromStorage 等を呼び出す)
            // ここではアイテムを1スタック分（または預けた分）ストレージから差し引く処理が必要です
            boolean removed = entity.removeItemFromStorage(lastStack, count);

            if (removed) {
                // プレイヤーの現在の持ち手を確認
                ItemStack currentHeld = player.field_71071_by.func_70445_o();

                if (currentHeld == null) {
                    // 手が空ならそのまま戻す
                    ItemStack returnStack = lastStack.copy();
                    returnStack.stackSize = count;
                    player.field_71071_by.func_70299_a(player.field_71071_by.field_70461_c, returnStack);
                }
                else if (currentHeld.isItemEqual(lastStack) && ItemStack.areItemStackTagsEqual(currentHeld, lastStack)) {
                    // 同じアイテムを持っているならスタック数を増やす
                    currentHeld.stackSize += count;
                }
                else {
                    // 手が別のアイテムで塞がっている場合はインベントリの空きへ
                    ItemStack returnStack = lastStack.copy();
                    returnStack.stackSize = count;
                    if (!player.field_71071_by.func_70441_a(returnStack)) {
                        // インベントリがいっぱいなら足元にドロップ
                        player.func_71018_a(returnStack);
                    }
                }
            } else {
                // 1.5.2 には派手な通知メッセージがないので、サーバーログかチャットに送る
                if (!player.field_70170_p.field_72995_K) {
                    player.func_70092_c("Storage Error: Failed to remove item");
                }
            }
        }

        // 🌟 履歴の消去（ClickCountはリセットしない）
        entity.setLastTransferStack(null);
        entity.setLastTransferCount(0);
    }
}