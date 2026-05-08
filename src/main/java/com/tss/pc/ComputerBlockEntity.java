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

    // worldObjのフィールド名は実行時にリフレクションで検索する（SRG名もMCP名も不明なため）
    private static java.lang.reflect.Field tileEntityWorldField;
    static {
        try {
            for (java.lang.reflect.Field f : TileEntity.class.getDeclaredFields()) {
                if (f.getType() == net.minecraft.world.World.class) {
                    f.setAccessible(true);
                    tileEntityWorldField = f;
                    System.out.println("DEBUG: [TSSPC] TileEntity worldObj field = " + f.getName());
                    break;
                }
            }
            if (tileEntityWorldField == null) {
                System.out.println("DEBUG: [TSSPC] WARNING: worldObj not found in TileEntity!");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: [TSSPC] worldObj search error: " + e);
        }
    }

    protected net.minecraft.world.World getWorldObj() {
        if (tileEntityWorldField == null) return null;
        try { return (net.minecraft.world.World) tileEntityWorldField.get(this); }
        catch (Exception e) { return null; }
    }

    // === NBT SRG名メソッドをリフレクションでキャッシュ ===
    private static java.lang.reflect.Method nbtSetTagMethod;
    private static java.lang.reflect.Method nbtGetTagListMethod;
    private static java.lang.reflect.Method nbtGetCompoundTagMethod;
    private static java.lang.reflect.Method nbtAppendTagMethod;
    private static java.lang.reflect.Method nbtTagAtMethod;
    private static java.lang.reflect.Method nbtTagCountMethod;
    static {
        Class<?> cNbtCompound = net.minecraft.nbt.NBTTagCompound.class;
        Class<?> cNbtList = net.minecraft.nbt.NBTTagList.class;
        Class<?> cNbtBase = net.minecraft.nbt.NBTBase.class;
        try {
            for (java.lang.reflect.Method m : cNbtCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class && p[1] == cNbtBase && m.getReturnType() == void.class) {
                    nbtSetTagMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT setTag = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] setTag err: " + e); }
        try {
            for (java.lang.reflect.Method m : cNbtCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && m.getReturnType() == cNbtList) {
                    nbtGetTagListMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT getTagList = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] getTagList err: " + e); }
        try {
            for (java.lang.reflect.Method m : cNbtCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && m.getReturnType() == cNbtCompound) {
                    nbtGetCompoundTagMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT getCompoundTag = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] getCompoundTag err: " + e); }
        try {
            for (java.lang.reflect.Method m : cNbtList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == cNbtBase && m.getReturnType() == void.class) {
                    nbtAppendTagMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT appendTag = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] appendTag err: " + e); }
        try {
            for (java.lang.reflect.Method m : cNbtList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == int.class && cNbtBase.isAssignableFrom(m.getReturnType())) {
                    nbtTagAtMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT tagAt = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] tagAt err: " + e); }
        try {
            for (java.lang.reflect.Method m : cNbtList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 && m.getReturnType() == int.class && !m.getName().equals("hashCode")) {
                    nbtTagCountMethod = m;
                    System.out.println("DEBUG: [TSSPC] NBT tagCount = " + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] tagCount err: " + e); }
    }
    private static void nbtSetTag(NBTTagCompound nbt, String key, net.minecraft.nbt.NBTBase val) {
        if (nbtSetTagMethod != null) try { nbtSetTagMethod.invoke(nbt, key, val); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtSetTag err: " + e); }
    }
    private static NBTTagList nbtGetTagList(NBTTagCompound nbt, String key) {
        if (nbtGetTagListMethod != null) try { return (NBTTagList) nbtGetTagListMethod.invoke(nbt, key); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtGetTagList err: " + e); }
        return new NBTTagList();
    }
    private static NBTTagCompound nbtGetCompoundTag(NBTTagCompound nbt, String key) {
        if (nbtGetCompoundTagMethod != null) try { return (NBTTagCompound) nbtGetCompoundTagMethod.invoke(nbt, key); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtGetCompoundTag err: " + e); }
        return new NBTTagCompound();
    }
    private static void nbtAppendTag(NBTTagList list, net.minecraft.nbt.NBTBase val) {
        if (nbtAppendTagMethod != null) try { nbtAppendTagMethod.invoke(list, val); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtAppendTag err: " + e); }
    }
    private static net.minecraft.nbt.NBTBase nbtTagAt(NBTTagList list, int i) {
        if (nbtTagAtMethod != null) try { return (net.minecraft.nbt.NBTBase) nbtTagAtMethod.invoke(list, i); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtTagAt err: " + e); }
        return null;
    }
    private static int nbtTagCount(NBTTagList list) {
        if (nbtTagCountMethod != null) try { return (Integer) nbtTagCountMethod.invoke(list); } catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtTagCount err: " + e); }
        return 0;
    }

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
    public void setLastTransferStack(ItemStack stack) { this.lastTransferStack = (stack == null) ? null : MCHelper.itemCopy(stack); }
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
                this.func_70296_a();
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
            this.func_70296_a();

            // クライアント側に「在庫が減ったよ」と同期するために
            // 1.5.2では世界（worldObj）のマーク更新を行うのが一般的です
            if (this.getWorldObj() != null) {
                this.getWorldObj().func_72698_d(this.field_70329_l, this.field_70330_k, this.field_70328_m);
            }
        }

        return success;
    }

    public BulkItemStorage getBulkStorage() {
        // worldObj が null（初期化前）の場合は空のストレージを返す
        if (this.getWorldObj() == null) {
            if (this.clientSideStorage == null) this.clientSideStorage = new BulkItemStorage();
            return this.clientSideStorage;
        }

        // 🌟 サーバー側の場合
        if (!this.getWorldObj().field_72995_K) {
            // GlobalItemStorageData.get(world) を呼ぶ
            GlobalItemStorageData data = GlobalItemStorageData.get(this.getWorldObj());
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
        // 1.5.2 では player.field_71069_bj を ComputerContainer にキャストして操作します
        this.func_70296_a();
    }

    @Override
    public void func_70307_a(net.minecraft.nbt.NBTTagCompound nbt) {
        super.func_70307_a(nbt);


        // 🌟 プレイヤーごとのタブデータを復元
        this.playerTabs.clear();
        NBTTagList playersList = nbtGetTagList(nbt, "PlayerTabData");
        for (int i = 0; i < nbtTagCount(playersList); i++) {
            NBTTagCompound playerTag = (NBTTagCompound) nbtTagAt(playersList, i);
            if (playerTag == null) continue;
            String playerName = playerTag.getString("PlayerName");

            List<FavoriteTab> tabs = new ArrayList<FavoriteTab>();
            NBTTagList tabList = nbtGetTagList(playerTag, "Tabs");
            for (int j = 0; j < nbtTagCount(tabList); j++) {
                NBTTagCompound tabTag = (NBTTagCompound) nbtTagAt(tabList, j);
                if (tabTag == null) continue;
                FavoriteTab tab = new FavoriteTab(tabTag.getString("TabName"));

                if (tabTag.hasKey("Icon")) {
                    tab.icon = MCHelper.itemLoadFromNBT(nbtGetCompoundTag(tabTag, "Icon"));
                }

                // 🌟 スロットの復元
                NBTTagList slotList = nbtGetTagList(tabTag, "Slots");
                for (int k = 0; k < nbtTagCount(slotList); k++) {
                    NBTTagCompound slotTag = (NBTTagCompound) nbtTagAt(slotList, k);
                    if (slotTag == null) continue;
                    int slotIdx = slotTag.getByte("Slot");
                    if (slotIdx >= 0 && slotIdx < tab.slots.size()) {
                        tab.slots.set(slotIdx, MCHelper.itemLoadFromNBT(slotTag));
                    }
                }
                tabs.add(tab);
            }
            playerTabs.put(playerName, tabs);
        }
    }
    @Override
    public void func_70310_b(net.minecraft.nbt.NBTTagCompound nbt) {
        super.func_70310_b(nbt);

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
                    MCHelper.itemWriteToNBT(tab.icon, iconTag);
                    nbtSetTag(tabTag, "Icon", iconTag);
                }

                // 🌟 重要：45個のスロットを保存
                NBTTagList slotList = new NBTTagList();
                for (int i = 0; i < tab.slots.size(); i++) {
                    ItemStack stack = tab.slots.get(i);
                    if (stack != null) {
                        NBTTagCompound slotTag = new NBTTagCompound();
                        slotTag.setByte("Slot", (byte) i);
                        MCHelper.itemWriteToNBT(stack, slotTag);
                        nbtAppendTag(slotList, slotTag);
                    }
                }
                nbtSetTag(tabTag, "Slots", slotList);
                nbtAppendTag(tabList, tabTag);
            }
            nbtSetTag(playerTag, "Tabs", tabList);
            nbtAppendTag(playersList, playerTag);
        }
        nbtSetTag(nbt, "PlayerTabData", playersList);
    }

    // --- サーバーからクライアントへデータを送るためのパケット作成 ---
    @Override
    public Packet func_70111_a() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.func_70310_b(nbt); // 先ほど修正した、全データを含む writeToNBT を呼び出す
        return new Packet132TileEntityData(this.field_70329_l, this.field_70330_k, this.field_70328_m, 1, nbt);
    }
    // --- クライアントがサーバーからのパケットを受け取った時の処理 ---
    @Override
    public void func_73109_a(net.minecraft.network.INetworkManager net, Packet132TileEntityData pkt) {
        // 1. パケットの中身を自分（クライアント側TileEntity）に読み込む
        this.func_70307_a(pkt.customParam1);

        // 2. もしGUIを開いていたら、スロットを最新状態にリフレッシュする
        if (this.getWorldObj().field_72995_K) { // クライアント側であることを確認
            updateGuiClient();
        }
    }

    // 🌟 クライアント側専用の更新メソッドを分ける
    @SideOnly(Side.CLIENT)
    private void updateGuiClient() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.field_71069_bj instanceof ComputerContainer) {
            ((ComputerContainer)mc.thePlayer.field_71069_bj).updateVisibleSlots();
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
                ItemStack copy = (stack == null) ? null : MCHelper.itemCopy(stack);
                if (copy != null) copy.stackSize = 1;

                tab.slots.set(slotIndex, copy);
                this.func_70296_a();

                // クライアントへ同期
                if (this.getWorldObj() != null) {
                    this.getWorldObj().func_72698_d(this.field_70329_l, this.field_70330_k, this.field_70328_m);
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
                this.func_70296_a();

                if (this.getWorldObj() != null) {
                    this.getWorldObj().func_72698_d(this.field_70329_l, this.field_70330_k, this.field_70328_m);
                }
            }
        }
    }
    // プレイヤー名をキーにして、その人のタブリストを保持する
    private Map<String, List<FavoriteTab>> playerTabs = new HashMap<String, List<FavoriteTab>>();

    public List<FavoriteTab> getTabsForPlayer(EntityPlayer player) {
        String playerName = player.func_70005_c();
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
            this.func_70296_a();
        }
    }

}