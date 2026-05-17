package com.tss.pc;

import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
public class PacketHandler implements IPacketHandler {

    // packet.data は本番jarでSRG名に変換されているためリフレクションでアクセス
    private static java.lang.reflect.Field _p250Data;
    static {
        for (java.lang.reflect.Field f : Packet250CustomPayload.class.getDeclaredFields()) {
            if (f.getType() == byte[].class) {
                try { f.setAccessible(true); } catch (Throwable ig) {}
                _p250Data = f;
                break;
            }
        }
    }

    private static byte[] getPacketData(Packet250CustomPayload packet) {
        if (_p250Data != null) {
            try { return (byte[]) _p250Data.get(packet); } catch (Throwable ig) {}
        }
        try { return packet.data; } catch (Throwable ig) { return new byte[0]; }
    }

    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
        {
            EntityPlayer entityPlayer = (EntityPlayer) player;
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(getPacketData(packet)));

            try {
                int packetId = dis.readByte();

                if (!(entityPlayer.field_71069_bj instanceof ComputerContainer)) return;
                ComputerContainer container = (ComputerContainer) entityPlayer.field_71069_bj;
                ComputerBlockEntity te = container.getTileEntity();

                if (packetId == 6) { // 預け入れ
                    int slotIndex = dis.readInt();
                    handleDeposit(slotIndex, entityPlayer, te, container);

                } // onPacketData メソッドの中
                if (packetId == 1) { // タブ・ドラッグ操作
                    int actionType = dis.readInt();
                    int index = dis.readInt();
                    String name = dis.readUTF();
                    ItemStack stack = readItemStack(dis); // 自作の読み込みメソッド

                    // 🌟 追記：末尾に追加した float を読み取って Container にセット
                    container.scrollPos = dis.readFloat();
                    container.favScrollPos = dis.readFloat();
                    // 🌟 ここで handleTabAction を呼び出す！
                    // これにより、下のグレーアウトが解消され、実際の登録処理が動くようになります。
                    handleTabAction(actionType, index, name, stack, 1, entityPlayer, te, container, dis);

                } else if (packetId == 2) { // 引き出し
                    try {
                        short id = dis.readShort();
                        byte count = dis.readByte();
                        short damage = dis.readShort();
                        NBTTagCompound nbt = readNBT(dis);

                        ItemStack receivedStack = new ItemStack(id, count, damage);
                        receivedStack.stackTagCompound = nbt;

                        int amount = dis.readInt();

                        // 🌟 以下の2行を追加：クライアントから送られてきた最新のスクロール位置をコンテナにセット
                        container.scrollPos = dis.readFloat();    // メインストレージのスクロール位置
                        container.favScrollPos = dis.readFloat(); // お気に入りのスクロール位置

                        if (receivedStack != null) {
                            System.out.println("SERVER: Withdrawal request. Scroll:" + container.scrollPos);
                            handleWithdraw(receivedStack, amount, entityPlayer, te, container);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) { // ここで全体の try を閉じる
                System.err.println("SERVER ERROR: Packet processing failed!");
                e.printStackTrace();
            }
        }
    }
    /**
     * 🌟 ItemStack読み込み用 (readItemStack エラーをこれで解決)
     */
    private ItemStack readItemStack(DataInputStream dis) throws IOException {
        short itemID = dis.readShort();
        if (itemID == -1) return null;

        byte stackSize = dis.readByte();
        short damage = dis.readShort();
        ItemStack stack = new ItemStack(itemID, stackSize, damage);
        stack.stackTagCompound = readNBT(dis);
        return stack;
    }
    // onPacketData の終了
    private void handleTabAction(int actionType, int index, String name, net.minecraft.item.ItemStack icon, int amount, net.minecraft.entity.player.EntityPlayer player, ComputerBlockEntity te, ComputerContainer container, DataInputStream dis) throws Exception {
        EntityPlayer entityPlayer = (EntityPlayer) player;
        // 🌟 1.16.5の getTabsForPlayer(player) 相当
        // ここで te.getTabsForPlayer(player) を呼びます（後述の修正が必要）
        List<ComputerBlockEntity.FavoriteTab> tabs = te.getTabsForPlayer(player);

        switch (actionType) {
            case 0: // タブの追加
                int nextNumber = 1;
                boolean exists;
                String candidateName;
                do {
                    exists = false;
                    candidateName = (nextNumber == 1) ? "Favorite" : "Favorite" + nextNumber;
                    for (ComputerBlockEntity.FavoriteTab tab : tabs) {
                        if (tab.name != null && tab.name.equalsIgnoreCase(candidateName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) nextNumber++;
                } while (exists);

                tabs.add(new ComputerBlockEntity.FavoriteTab(candidateName));
                break;

            case 1: // タブの削除
                if (tabs.size() > 1) {
                    int oldIndex = container.selectedTabIndex;
                    if (oldIndex >= 0 && oldIndex < tabs.size()) {
                        tabs.remove(oldIndex);
                        container.selectedTabIndex = Math.max(0, Math.min(oldIndex, tabs.size() - 1));
                    }
                }
                break;

            case 2: // タブ切替
                if (index >= 0 && index < tabs.size()) {
                    container.selectedTabIndex = index;
                }
                break;

            case 3: // 名前更新
                if (container.selectedTabIndex >= 0 && container.selectedTabIndex < tabs.size()) {
                    tabs.get(container.selectedTabIndex).name = name;
                }
                break;

            case 4: // タブアイコン更新
                if (index >= 0 && index < tabs.size()) {
                    // 🌟 修正：iconがnull（解除）ならそのままnullをセット、あればコピー
                    if (icon != null) {
                        ItemStack iconStack = MCHelper.itemCopy(icon);
                        iconStack.stackSize = 1;
                        tabs.get(index).icon = iconStack;
                        System.out.println("SERVER: Tab Icon updated for index " + index);
                    } else {
                        tabs.get(index).icon = null;
                        System.out.println("SERVER: Tab Icon CLEARED for index " + index);
                    }

                    if (te.getWorldObj() != null) {
                        te.func_70296_a();
                        te.getWorldObj().func_72698_d(te.field_70329_l, te.field_70330_k, te.field_70328_m);
                    }
                }
                break;

            case 5: // お気に入り登録
                if (container.selectedTabIndex >= 0 && container.selectedTabIndex < tabs.size()) {
                    ComputerBlockEntity.FavoriteTab tab = tabs.get(container.selectedTabIndex);
                    if (index >= 0) {
                        // 🌟 リストが足りなければ自動で拡張する（重要！）
                        while (tab.slots.size() <= index) {
                            tab.slots.add(null);
                        }

                        tab.slots.set(index, icon != null ? MCHelper.itemCopy(icon) : null);

                        // 保存と同期
                        te.func_70296_a();
                        container.updateVisibleSlots();
                        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                            ((net.minecraft.entity.player.EntityPlayerMP) player).func_70483_a(container);
                        }
                    }
                }
                break;

            case 6: // 引き出し（1.16.5のpullFromStorage相当）
                // 定義に合わせて 5つの引数を渡す
                // icon: どのアイテムか, amount: いくつか, player, te, container
                handleWithdraw(icon, amount, player, te, container);
                return; // 早期リターン

            case 7: // 行追加
                if (container.selectedTabIndex >= 0 && container.selectedTabIndex < tabs.size()) {
                    ComputerBlockEntity.FavoriteTab tab = tabs.get(container.selectedTabIndex);
                    for (int i = 0; i < 5; i++) tab.slots.add(null);

                    // 🌟 1. サーバーのデータを確定保存
                    FavoriteConfig.saveAll(te.getAllPlayerTabs());

                    // 🌟 2. TileEntity の変更を通知（これがないとクライアントにパケットが飛ばない）
                    te.func_70296_a();
                    if (te.getWorldObj() != null) {
                        // クライアントへ TileEntity の NBT データを再送させる
                        te.getWorldObj().func_72698_d(te.field_70329_l, te.field_70330_k, te.field_70328_m);
                    }

                    System.out.println("SERVER: Added Row. Current Total Slots: " + tab.slots.size());

                    // 🌟 3. コンテナの表示内容を更新
                    container.updateVisibleSlots();
                    if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                        ((net.minecraft.entity.player.EntityPlayerMP) player).func_70483_a(container);
                    }
                }
                break;

            case 8: // 行削除
                if (container.selectedTabIndex >= 0 && container.selectedTabIndex < tabs.size()) {
                    ComputerBlockEntity.FavoriteTab tab = tabs.get(container.selectedTabIndex);
                    if (tab.slots.size() > 45) {
                        for (int i = 0; i < 5; i++) tab.slots.remove(tab.slots.size() - 1);

                        // 🌟 ログを追加
                        System.out.println("SERVER: Removed Row. Current Total Slots: " + tab.slots.size());

                        // 同期処理
                        container.updateVisibleSlots();
                        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                            ((net.minecraft.entity.player.EntityPlayerMP) player).func_70483_a(container);
                        }
                    } else {
                        System.out.println("SERVER: Cannot remove row. Already at minimum (45).");
                    }
                }
                break;
        }

        FavoriteConfig.saveAll(te.getAllPlayerTabs());

        // 🌟 1.5.2での同期 (te. 経由で呼ぶことで worldObj エラーを回避)
        te.func_70296_a();

        if (te.getWorldObj() != null) {
            // サーバーからクライアントへ「データ送って！」と通知する命令
            te.getWorldObj().func_72698_d(te.field_70329_l, te.field_70330_k, te.field_70328_m);
        }

        // UIの表示更新
        container.updateVisibleSlots();
        container.func_75142_b();
    }
    /**
     * ItemStack読み込み用の補助メソッド
     */
    private void handleDeposit(int slotIndex, EntityPlayer player, ComputerBlockEntity te, ComputerContainer container) {
        ItemStack stackInSlot = null;

        // 🌟 スロット番号からアイテムを特定する
        if (slotIndex == -999) {
            // マウスで掴んでいるアイテム
            stackInSlot = MCHelper.invGetItemStack(player.field_71071_by);
        } else if (slotIndex >= 0 && slotIndex < player.field_71071_by.field_70462_a.length) {
            // インベントリ内の指定スロットのアイテム
            stackInSlot = player.field_71071_by.func_70301_a(slotIndex);
        }

        // アイテムが存在する場合のみ処理
        if (stackInSlot != null && stackInSlot.stackSize > 0) {
            // 1. ストレージに追加
            te.getBulkStorage().addStack(stackInSlot, stackInSlot.stackSize);

            // 2. インベントリ側を空にする
            if (slotIndex == -999) {
                MCHelper.invSetItemStack(player.field_71071_by, null);
            } else {
                player.field_71071_by.func_70299_a(slotIndex, null);
            }

            // 3. 同期処理
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                net.minecraft.entity.player.EntityPlayerMP playerMP = (net.minecraft.entity.player.EntityPlayerMP) player;
                playerMP.func_70483_a(container);
                try { playerMP.updateHeldItem(); } catch (Throwable ignored) {}
            }
            te.func_70296_a();
        }
    }

    /**
     * 🌟 スタック数に関係なく「同じ種類のアイテムか」を判定する補助メソッド
     */
    private boolean isSameItem(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.itemID != s2.itemID || s1.itemDamage != s2.itemDamage) return false;
        // NBTタグ比較: equalsはObjectから継承されており安全に呼べる
        NBTTagCompound t1 = s1.stackTagCompound;
        NBTTagCompound t2 = s2.stackTagCompound;
        if (t1 == null && t2 == null) return true;
        if (t1 == null || t2 == null) return false;
        return t1.equals(t2);
    }

    private void handleWithdraw(ItemStack stack, int amount, EntityPlayer player, ComputerBlockEntity te, ComputerContainer container) {
        if (stack == null || player == null) return;

        // 1. ストレージから実体を引き出す
        ItemStack result = te.getBulkStorage().extractActual(stack, amount);

        if (result != null) {
            // 2. プレイヤーに渡す
            if (!player.field_71071_by.func_70441_a(result)) {
                player.func_71018_a(result);
            }

            // 🌟 3. お気に入りデータの同期 (タイルエンティティ側の実データを更新)
            List<ComputerBlockEntity.FavoriteTab> tabs = te.getTabsForPlayer(player);
            if (container.selectedTabIndex >= 0 && container.selectedTabIndex < tabs.size()) {
                ComputerBlockEntity.FavoriteTab currentTab = tabs.get(container.selectedTabIndex);

                for (int i = 0; i < currentTab.slots.size(); i++) {
                    ItemStack favStack = currentTab.slots.get(i);
                    if (favStack != null && isSameItem(favStack, stack)) {
                        // 在庫数を再取得してお気に入りの個数にセット
                        int totalInStorage = te.getBulkStorage().getCountOf(favStack);
                        favStack.stackSize = (totalInStorage > 0) ? totalInStorage : 1; // 0個でもアイコンは消さない

                        if (favStack.stackTagCompound != null) {
                            MCHelper.nbtSetInteger(favStack.stackTagCompound, "RealCount", totalInStorage);
                        }
                    }
                }
            }

            // 🌟 4. コンテナの表示を更新 (TileEntityから再コピー)
            container.updateVisibleSlots();

            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                container.func_75142_b();
                ((net.minecraft.entity.player.EntityPlayerMP) player).func_70483_a(container);
            }
            te.func_70296_a();
        }
    }


    private static NBTTagCompound readNBT(DataInputStream dis) throws IOException {
        short length = dis.readShort();
        if (length <= 0) return null;

        byte[] compressed = new byte[length];
        dis.readFully(compressed);

        try {
            // 読み込んだNBTをそのまま返す（nullならnull、空なら空のまま）
            return net.minecraft.nbt.CompressedStreamTools.decompress(compressed);
        } catch (Exception e) {
            return null;
        }
    }
}
