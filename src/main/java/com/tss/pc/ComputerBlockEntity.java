package com.tss.pc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.*;

public class ComputerBlockEntity extends TileEntity {

    // --- クリック管理用変数 ---
    private int clickCount = 0;
    private long lastClickTime = 0;
    private int lastClickedSlotIndex = -1;
    private ItemStack lastTransferStack = null;
    private int lastTransferCount = 0;
    public net.minecraft.item.ItemStack[] tabIcons = new net.minecraft.item.ItemStack[9];
    public int totalTabs = 1;
    public String[] tabNames = new String[9];

    private BulkItemStorage clientSideStorage = null;
    // --- お気に入りタブ管理 (1.16.5 の FavoriteTab クラスを 1.5.2 用に移植) ---

    public static class FavoriteTab {
        public String name;
        public ItemStack icon;
        public List<ItemStack> slots;

        public FavoriteTab(String name) {
            this.name = name;
            this.icon = null;
            this.slots = new ArrayList<ItemStack>();
            for (int i = 0; i < 45; i++) {
                this.slots.add(null);
            }
        }
    }

    private List<FavoriteTab> tabs = new ArrayList<FavoriteTab>();
    private BulkItemStorage storage = new BulkItemStorage();

    public ComputerBlockEntity() {
        // コンストラクタで初期タブを1つ作成
        tabs.add(new FavoriteTab("Favorite"));
    }

    // --- 1.16.5 の機能を 1.5.2 の型で再現 (Getter/Setter) ---
    public int getClickCount() { return clickCount; }
    public void setClickCount(int count) { this.clickCount = count; }
    public long getLastClickTime() { return lastClickTime; }
    public void setLastClickTime(long time) { this.lastClickTime = time; }
    public int getLastClickedSlotIndex() { return lastClickedSlotIndex; }
    public void setLastClickedSlotIndex(int index) { this.lastClickedSlotIndex = index; }
    public void setLastTransferStack(ItemStack stack) { this.lastTransferStack = (stack == null) ? null : stack.copy(); }
    public ItemStack getLastTransferStack() { return lastTransferStack; }
    public void setLastTransferCount(int count) { this.lastTransferCount = count; }
    public int getLastTransferCount() { return lastTransferCount; }

    public void resetClickState() {
        this.clickCount = 0;
        this.lastTransferCount = 0;
        this.lastTransferStack = null;
        this.lastClickedSlotIndex = -1;
    }

    // --- アイテム預け入れロジックの核 (1.16.5 の BulkItemStorage との連携部分) ---
    public int addItemToStorage(ItemStack stack, int amount) {
        if (stack == null) return 0;
        int toMove = Math.min(stack.stackSize, amount);

        // 🌟 ここで 1.5.2 用に書き換えた BulkItemStorage にアクセスします
        // ※ BulkItemStorage 側も 1.5.2 用に移植されている必要があります。
        BulkItemStorage storage = getBulkStorage();
        if (storage != null) {
            int moved = storage.addStack(stack, toMove);
            if (moved > 0) {
                this.onInventoryChanged();
                return moved;
            }
        }
        return 0;
    }

    public boolean removeItemFromStorage(ItemStack type, int amount) {
        if (type == null || amount <= 0) return false;

        // BulkItemStorage (おそらく storage という変数名) の removeStack を呼び出す
        // 第1段階の厳密チェック、第2段階のID救済措置がそのまま適用されます
        boolean success = this.storage.removeStack(type, amount);

        if (success) {
            // 在庫が変わったので保存フラグを立てる
            this.onInventoryChanged();

            // クライアント側に「在庫が減ったよ」と同期するために
            // 1.5.2では世界（worldObj）のマーク更新を行うのが一般的です
            if (this.worldObj != null) {
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            }
        }

        return success;
    }

    public BulkItemStorage getBulkStorage() {
        // worldObj が null（初期化前）の場合は空のストレージを返す
        if (this.worldObj == null) {
            if (this.clientSideStorage == null) this.clientSideStorage = new BulkItemStorage();
            return this.clientSideStorage;
        }

        // 🌟 サーバー側の場合
        if (!this.worldObj.isRemote) {
            // GlobalItemStorageData.get(world) を呼ぶ
            GlobalItemStorageData data = GlobalItemStorageData.get(this.worldObj);
            if (data != null) {
                return data.getStorage();
            }
        }

        // 🌟 クライアント側（isRemote == true）の場合
        if (this.clientSideStorage == null) {
            this.clientSideStorage = new BulkItemStorage();
        }
        return this.clientSideStorage;
    }
    // --- タブ操作ロジック (handleTabAction の移植) ---
 public void handleTabAction(int actionType, int index, String name, ItemStack icon, int amount, EntityPlayer player) {
        // 1.16.5 の handleTabAction 内の switch 文の内容をここに移植します
        // 1.5.2 では player.openContainer を ComputerContainer にキャストして操作します
        this.onInventoryChanged();
    }

    @Override
    public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        super.readFromNBT(nbt);


        // 🌟 プレイヤーごとのタブデータを復元
        this.playerTabs.clear();
        NBTTagList playersList = nbt.getTagList("PlayerTabData");
        for (int i = 0; i < playersList.tagCount(); i++) {
            NBTTagCompound playerTag = (NBTTagCompound) playersList.tagAt(i);
            String playerName = playerTag.getString("PlayerName");

            List<FavoriteTab> tabs = new ArrayList<FavoriteTab>();
            NBTTagList tabList = playerTag.getTagList("Tabs");
            for (int j = 0; j < tabList.tagCount(); j++) {
                NBTTagCompound tabTag = (NBTTagCompound) tabList.tagAt(j);
                FavoriteTab tab = new FavoriteTab(tabTag.getString("TabName"));

                if (tabTag.hasKey("Icon")) {
                    tab.icon = ItemStack.loadItemStackFromNBT(tabTag.getCompoundTag("Icon"));
                }

                // 🌟 スロットの復元
                NBTTagList slotList = tabTag.getTagList("Slots");
                for (int k = 0; k < slotList.tagCount(); k++) {
                    NBTTagCompound slotTag = (NBTTagCompound) slotList.tagAt(k);
                    int slotIdx = slotTag.getByte("Slot");
                    if (slotIdx >= 0 && slotIdx < tab.slots.size()) {
                        tab.slots.set(slotIdx, ItemStack.loadItemStackFromNBT(slotTag));
                    }
                }
                tabs.add(tab);
            }
            playerTabs.put(playerName, tabs);
        }
    }
    @Override
    public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.worldObj != null && !this.worldObj.isRemote) {
            GlobalItemStorageData.get(this.worldObj).markDirty();
        }

        // 🌟 プレイヤーごとのタブデータを保存
        NBTTagList playersList = new NBTTagList();
        for (Map.Entry<String, List<FavoriteTab>> entry : playerTabs.entrySet()) {
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setString("PlayerName", entry.getKey());

            NBTTagList tabList = new NBTTagList();
            for (FavoriteTab tab : entry.getValue()) {
                NBTTagCompound tabTag = new NBTTagCompound();
                tabTag.setString("TabName", tab.name);

                // アイコンの保存
                if (tab.icon != null) {
                    NBTTagCompound iconTag = new NBTTagCompound();
                    tab.icon.writeToNBT(iconTag);
                    tabTag.setTag("Icon", iconTag);
                }

                // 🌟 重要：45個のスロットを保存
                NBTTagList slotList = new NBTTagList();
                for (int i = 0; i < tab.slots.size(); i++) {
                    ItemStack stack = tab.slots.get(i);
                    if (stack != null) {
                        NBTTagCompound slotTag = new NBTTagCompound();
                        slotTag.setByte("Slot", (byte) i);
                        stack.writeToNBT(slotTag);
                        slotList.appendTag(slotTag);
                    }
                }
                tabTag.setTag("Slots", slotList);
                tabList.appendTag(tabTag);
            }
            playerTag.setTag("Tabs", tabList);
            playersList.appendTag(playerTag);
        }
        nbt.setTag("PlayerTabData", playersList);
    }

    // --- サーバーからクライアントへデータを送るためのパケット作成 ---
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt); // 先ほど修正した、全データを含む writeToNBT を呼び出す
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
    }
    // --- クライアントがサーバーからのパケットを受け取った時の処理 ---
    @Override
    public void onDataPacket(net.minecraft.network.INetworkManager net, Packet132TileEntityData pkt) {
        // 1. パケットの中身を自分（クライアント側TileEntity）に読み込む
        this.readFromNBT(pkt.customParam1);

        // 2. もしGUIを開いていたら、スロットを最新状態にリフレッシュする
        if (this.worldObj.isRemote) { // クライアント側であることを確認
            updateGuiClient();
        }
    }

    // 🌟 クライアント側専用の更新メソッドを分ける
    @SideOnly(Side.CLIENT)
    private void updateGuiClient() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.openContainer instanceof ComputerContainer) {
            ((ComputerContainer)mc.thePlayer.openContainer).updateVisibleSlots();
        }
    }
    // 🌟 Containerからタブ一覧を取得するためのメソッドを追加
    public List<FavoriteTab> getTabs() {
        // もし tabs が null なら初期化する（1.16.5のロジックを流用）
        if (this.tabs == null || this.tabs.isEmpty()) {
            this.tabs = new ArrayList<FavoriteTab>();
            this.tabs.add(new FavoriteTab("Favorite"));
        }
        return this.tabs;
    }

    // 🌟 お気に入りを特定のスロットに設定（ドラッグ登録用）
    public void setFavorite(int tabIndex, int slotIndex, ItemStack stack) {
        List<FavoriteTab> tabList = getTabs();
        if (tabIndex >= 0 && tabIndex < tabList.size()) {
            FavoriteTab tab = tabList.get(tabIndex);
            if (slotIndex >= 0 && slotIndex < 45) {
                // アイテムを1個だけコピーして登録（1.16.5の仕様）
                ItemStack copy = (stack == null) ? null : stack.copy();
                if (copy != null) copy.stackSize = 1;

                tab.slots.set(slotIndex, copy);
                this.onInventoryChanged();

                // クライアントへ同期
                if (this.worldObj != null) {
                    this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                }
            }
        }
    }

    // 🌟 お気に入りを削除（ホイールクリック解除用）
    public void removeFavorite(int tabIndex, int slotIndex) {
        List<FavoriteTab> tabList = getTabs();
        if (tabIndex >= 0 && tabIndex < tabList.size()) {
            FavoriteTab tab = tabList.get(tabIndex);
            if (slotIndex >= 0 && slotIndex < tab.slots.size()) {
                tab.slots.set(slotIndex, null);
                this.onInventoryChanged();

                if (this.worldObj != null) {
                    this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                }
            }
        }
    }
    // プレイヤー名をキーにして、その人のタブリストを保持する
    private Map<String, List<FavoriteTab>> playerTabs = new HashMap<String, List<FavoriteTab>>();

    public List<FavoriteTab> getTabsForPlayer(EntityPlayer player) {
        String playerName = player.getEntityName();
        if (!playerTabs.containsKey(playerName)) {
            // 新規プレイヤーなら初期タブを作成
            List<FavoriteTab> newList = new ArrayList<FavoriteTab>();
            newList.add(new FavoriteTab("Favorite"));
            playerTabs.put(playerName, newList);
        }
        return playerTabs.get(playerName);
    }

    public Map<String, List<FavoriteTab>> getAllPlayerTabs() {
        return this.playerTabs;
    }

    /**
     * 🌟 1.16.5のロジックを再現した新規タブ追加
     */
    public void addNewTab(EntityPlayer player) {
        List<FavoriteTab> tabs = getTabsForPlayer(player);
        if (tabs.size() < 9) {
            int nextNumber = tabs.size() + 1;
            tabs.add(new FavoriteTab("Favorite " + nextNumber));

            // サーバー側のデータを確定
            this.onInventoryChanged();
        }
    }

}