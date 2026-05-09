package com.tss.pc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList; //
import java.util.*;

public class ComputerContainer extends Container {
    private ComputerBlockEntity tileEntity;
    private final EntityPlayer player;

    // ランタイムでのaddSlotToContainerメソッドを動的に検索する
    private static java.lang.reflect.Method containerAddSlot;
    // Container.inventoryItemStacks (List<Slot>) のSRGフィールドをリフレクションで取得
    private static java.lang.reflect.Field _containerSlotList;
    static {
        for (java.lang.reflect.Method m : net.minecraft.inventory.Container.class.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 1 && net.minecraft.inventory.Slot.class.isAssignableFrom(p[0])) {
                try { m.setAccessible(true); } catch (Exception e) {}
                containerAddSlot = m;
                System.out.println("DEBUG: Found addSlotToContainer: " + m.getName());
                break;
            }
        }
        for (java.lang.reflect.Field f : net.minecraft.inventory.Container.class.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                if (java.util.List.class.isAssignableFrom(f.getType())) {
                    _containerSlotList = f;
                    System.out.println("DEBUG: Found Container slot list: " + f.getName());
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    /** Container のスロットリストをリフレクション経由で取得 (field_75151_b の SRG 名が違う場合の代替) */
    public java.util.List getSlots() {
        if (_containerSlotList != null) try { return (java.util.List) _containerSlotList.get(this); } catch (Exception ignored) {}
        return this.field_75151_b;
    }

    private void addSlot(Slot slot) {
        if (containerAddSlot != null) {
            try {
                containerAddSlot.invoke(this, slot);
                return;
            } catch (Exception e) {
                System.err.println("DEBUG: addSlot reflection failed: " + e.getMessage());
            }
        }
        // フォールバック：スロットリストに直接追加
        java.util.List slots = getSlots();
        if (slots != null) { slot.slotNumber = slots.size(); slots.add(slot); }
    }

    // スクロール・フィルタ状態 (Screenから更新される)
    public float scrollPos = 0.0F;
    public float favScrollPos = 0.0F;
    public String searchText = "";
    public String selectedMod = "all";
    public int selectedTabIndex = 0;
    private InventoryBasic storageView = new InventoryBasic("StorageView", false, 45);
    private InventoryBasic favoriteView = new InventoryBasic("FavoriteView", false, 45);
    // コンテナにお気に入り用のスクロール変数がない場合は追加
    public float favoriteScrollPos = 0.0F;

    // メインストレージの最大行数
    public int getMaxMainRows() {
        return (int) Math.ceil(tileEntity.getBulkStorage().getFilteredItemList(selectedMod, searchText).size() / 9.0);
    }

    // プレイヤーが持っているタブの総数
    public int getTotalTabCount() {
        return tileEntity.getTabsForPlayer(player).size();
    }
    public ComputerContainer(InventoryPlayer invPlayer, EntityPlayer playerObj, ComputerBlockEntity te) {
        // 1. 基本情報のセット
        this.tileEntity = te;
        this.player = playerObj;

        // 2. 仮想インベントリの初期化（nullチェックではなく毎回新規作成でOKです）
        this.storageView = new InventoryBasic("StorageView", false, 45);
        this.favoriteView = new InventoryBasic("FavoriteView", false, 45);

        // 1. メインストレージ表示 (0-44)
        for (int i = 0; i < 45; i++) {
            this.addSlot(new SlotReadOnly(storageView, i, 8 + (i % 9) * 18, 26 + (i / 9) * 18));
        }

        // 2. お気に入り (45-89)
        for (int i = 0; i < 45; i++) {
            this.addSlot(new SlotReadOnly(favoriteView, i, 192 + (i % 5) * 18, 26 + (i / 5) * 18));
        }

        // 3. プレイヤーインベントリ (90-116)
        for (int i = 0; i < 27; i++) {
            this.addSlot(new Slot(invPlayer, i + 9, 8 + (i % 9) * 18, 140 + (i / 9) * 18));
        }

        // 4. ホットバー (117-125)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 198));
        }

        // デバッグ用：スロットがいくつ登録されたかコンソールに出す
        System.out.println("DEBUG: Container initialized. Total slots: " + this.field_75151_b.size());
        // 最初の表示を更新
        updateVisibleSlots();
    }
    // 🌟 最小限の追加：Screenからの状態（スクロール等）を同期するメソッド
    public void updateState(float scroll, float favScroll, String search, String mod, int tab) {
        this.scrollPos = scroll;
        this.favScrollPos = favScroll;
        this.searchText = (search == null) ? "" : search;
        this.selectedMod = (mod == null) ? "all" : mod;
        this.selectedTabIndex = tab;
        this.updateVisibleSlots();
    }
    public void updateVisibleSlots() {
        if (tileEntity == null || tileEntity.getBulkStorage() == null) return;
        BulkItemStorage storage = tileEntity.getBulkStorage();
        List<ItemStack> filtered = storage.getFilteredItemList(selectedMod, searchText);

        // --- 1. メインストレージ表示の更新 ---
        int rowCount = (int) Math.ceil(filtered.size() / 9.0);
        int startRow = Math.round(scrollPos * Math.max(0, rowCount - 5));

        for (int i = 0; i < 45; i++) {
            int index = (startRow * 9) + i;
            if (index >= 0 && index < filtered.size()) {
                ItemStack s = MCHelper.itemCopy(filtered.get(index));
                int realCount = storage.getItemCount(s);
                if (s.stackTagCompound == null) s.stackTagCompound = new NBTTagCompound();
                s.stackTagCompound.setInteger("RealCount", realCount);
                s.stackSize = 1;
                storageView.func_70299_a(i, s);
            } else {
                storageView.func_70299_a(i, null);
            }
        }

        // --- 2. お気に入りスロット表示の更新 ---
        List<ComputerBlockEntity.FavoriteTab> tabs = tileEntity.getTabsForPlayer(player);
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            ComputerBlockEntity.FavoriteTab currentTab = tabs.get(selectedTabIndex);

            // 🌟 修正1：お気に入りの最大行数を計算（1行5スロット）
            int favRowCount = (int) Math.ceil(currentTab.slots.size() / 5.0);
            // 🌟 修正2：現在のスクロール位置から開始行を決定（表示は9行分 = 45スロット）
            int startFavRow = Math.round(favScrollPos * Math.max(0, favRowCount - 9));

            for (int i = 0; i < 45; i++) {
                // 🌟 修正3：スクロール位置を考慮したデータ上のインデックス
                int dataIndex = (startFavRow * 5) + i;

                // 🌟 修正4：リストの範囲内かどうかをチェック（IndexOutOfBounds防止）
                if (dataIndex >= 0 && dataIndex < currentTab.slots.size()) {
                    ItemStack favItem = currentTab.slots.get(dataIndex);

                    if (favItem != null) {
                        ItemStack displayStack = MCHelper.itemCopy(favItem);
                        int realCount = storage.getItemCount(displayStack);

                        if (displayStack.stackTagCompound == null) {
                            displayStack.stackTagCompound = new NBTTagCompound();
                        }
                        displayStack.stackTagCompound.setInteger("RealCount", realCount);
                        displayStack.stackSize = 1;

                        this.favoriteView.func_70299_a(i, displayStack);
                    } else {
                        this.favoriteView.func_70299_a(i, null);
                    }
                } else {
                    // 🌟 リストの範囲外（まだ行が追加されていない場所）は空にする
                    this.favoriteView.func_70299_a(i, null);
                }
            }
        }
    }
    @Override
    public boolean func_75145_c(EntityPlayer player) {
        return true;
    }
    @Override
    public ItemStack func_75140_a(int slotId, int button, int modifier, net.minecraft.entity.player.EntityPlayer player) {
        // 🌟 'side' の定義を復活（デバッグ用）
        String side = player.field_70170_p.field_72995_K ? "[CLIENT]" : "[SERVER]";
        System.out.println(side + " Click: SlotID=" + slotId + ", Button=" + button + ", Mod=" + modifier);
        // 範囲外ガード
        if (slotId < 0 || slotId >= this.field_75151_b.size()) return null;

        // --- 🌟 ストレージ表示スロット (0-44) ---
        if (slotId >= 0 && slotId < 45) {
            if (!player.field_70170_p.field_72995_K) {
                // サーバー側
                Slot slot = (Slot) this.field_75151_b.get(slotId);
                if (slot != null && slot.func_75216_f()) {
                    System.out.println(side + " Processing Withdraw for: " + MCHelper.itemGetDisplayName(slot.func_75211_c()));
                    System.out.println(side + " Attempting Withdraw: Slot " + slotId);

                    // 右クリックなら1個、それ以外なら最大スタック数
                    int amount = (button == 1) ? 1 : slot.func_75211_c().getMaxStackSize();

                    // 実際の引き出し処理
                    this.handleWithdraw(slotId, amount, player);

                    // 🌟 サーバーからクライアントへ強制同期（これで画面に反映される）
                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player).func_70483_a(this);
                        System.out.println(side + " Sent Container update packet to player.");
                    }
                }
            }
            return null; // バニラの「アイテムを掴む」動作を防止
        }

        // --- 🌟 お気に入りスロット (45-89) ---
        if (slotId >= 45 && slotId < 90) {
            if (!player.field_70170_p.field_72995_K) {
                // 🌟 修正：スクロール位置から「実際のデータの場所」を逆算する
                List<ComputerBlockEntity.FavoriteTab> playerTabs = tileEntity.getTabsForPlayer(player);
                if (playerTabs != null && !playerTabs.isEmpty()) {
                    ComputerBlockEntity.FavoriteTab currentTab = playerTabs.get(Math.min(selectedTabIndex, playerTabs.size() - 1));

                    // 現在の表示開始行を計算（updateVisibleSlotsと同じロジック）
                    int favRowCount = (int) Math.ceil(currentTab.slots.size() / 5.0);
                    int startFavRow = Math.round(favScrollPos * Math.max(0, favRowCount - 9));

                    // 🌟 実際のインデックス = (開始行 * 5列) + (クリックされたスロットの相対ID)
                    int actualFavIndex = (startFavRow * 5) + (slotId - 45);

                    ItemStack mouseStack = player.field_71071_by.getItemStack();

                    if (mouseStack != null) {
                        // --- 登録処理 ---
                        ItemStack favStack = MCHelper.itemCopy(mouseStack);
                        favStack.stackSize = 1;

                        // リストのサイズ調整（actualFavIndexを使う）
                        while (actualFavIndex >= currentTab.slots.size()) currentTab.slots.add(null);
                        currentTab.slots.set(actualFavIndex, favStack);

                        FavoriteConfig.saveAll(tileEntity.getAllPlayerTabs());
                    } else {
                        // --- 引き出し処理 ---
                        // favoriteViewのi番目ではなく、データリストから直接取得して引き出す
                        if (actualFavIndex < currentTab.slots.size()) {
                            ItemStack favItem = currentTab.slots.get(actualFavIndex);
                            if (favItem != null) {
                                int withdrawAmount = (button == 1) ? 1 : favItem.getMaxStackSize();
                                this.tileEntity.getBulkStorage().withdrawStack(player, favItem, withdrawAmount);
                            }
                        }
                    }
                }

                // 同期処理
                this.updateVisibleSlots();
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).func_70483_a(this);
                }
            }
            return null;
        }

        // --- 🌟 プレイヤーインベントリ (90-125) ---
        try {
            // Shiftクリックでの預け入れ
            if (modifier == 1 && slotId >= 90) {
                if (!player.field_70170_p.field_72995_K) {
                    this.handleDeposit(slotId, player);
                }
                return null;
            }
            return super.func_75140_a(slotId, button, modifier, player);
        } catch (Exception e) {
            System.err.println(side + " Error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ItemStack func_75150_a(EntityPlayer player, int slotIndex) {

        return null;
    }

    // 読み取り専用スロットクラス (1.5.2内部クラス)
    private class SlotReadOnly extends Slot {
        public SlotReadOnly(IInventory inv, int id, int x, int y) { super(inv, id, x, y); }
        @Override public boolean func_75214_a(ItemStack stack) { return false; }
        @Override public boolean func_82869_a(EntityPlayer player) { return true; } // これをtrueに
        @Override public ItemStack func_75209_e(int amount) { return null; } // 直接引き抜かれるのを防ぐ
    }

    // ComputerContainer.java の適当な場所（他のメソッドの間など）に追加
    public InventoryBasic getFavoriteView() {
        return this.favoriteView;
    }

    public ComputerBlockEntity getTileEntity() {
        return this.tileEntity;
    }
    // 1.5.2用：表示専用（取り出したり置いたりできない）スロット
    public class SlotFake extends net.minecraft.inventory.Slot {
        public SlotFake(net.minecraft.inventory.IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean func_75214_a(ItemStack stack) { return false; } // アイテムを置けない

        @Override
        public boolean func_82869_a(net.minecraft.entity.player.EntityPlayer player) { return false; } // 持ち上げられない
    }

    @Override
    public void func_75142_b() {
        // 🌟 先に表示を更新してから親の処理を呼ぶ
        if (!this.player.field_70170_p.field_72995_K) {
            this.updateVisibleSlots();
        }
        super.func_75142_b();
    }

    private void handleWithdraw(int slotId, int amount, EntityPlayer player) {
        Slot slot = (Slot) this.field_75151_b.get(slotId);
        if (slot == null || !slot.func_75216_f()) return;

        ItemStack displayStack = slot.func_75211_c();

        // 🌟 引き出し実行（ここは今のままでOK）
        this.tileEntity.getBulkStorage().withdrawStack(player, displayStack, amount);
        this.tileEntity.func_70296_a();

        // 🌟 重要：updateVisibleSlots を呼ぶ前に detectAndSendChanges を呼ぶか、
        // サーバー側 Container の scrollPos が PacketHandler で更新されている必要があります。
        this.updateVisibleSlots();

        if (player instanceof EntityPlayerMP) {
            // 🌟 画面全体を強制リフレッシュして「アイテムが消えた状態」をクライアントに送る
            ((EntityPlayerMP) player).func_70483_a(this);
        }
    }


    private void handleDeposit(int slotId, EntityPlayer player) {
        Slot slot = (Slot) this.field_75151_b.get(slotId);
        if (slot == null || !slot.func_75216_f()) return;

        ItemStack stackToDeposit = slot.func_75211_c();
        int addedCount = this.tileEntity.getBulkStorage().addStack(stackToDeposit, stackToDeposit.stackSize);

        if (addedCount > 0) {
            slot.func_75212_b(null);

            // 🌟 変更箇所3: 保存と表示更新
            this.tileEntity.func_70296_a();
            this.updateVisibleSlots();
            this.func_75142_b();

            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP)player).func_70483_a(this);
            }
            System.out.println("SERVER: Deposit Success! Added: " + addedCount);
        }
    }
}