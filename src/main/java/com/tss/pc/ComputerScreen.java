package com.tss.pc;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.inventory.IInventory;


public class ComputerScreen extends GuiContainer {
    // 1.5.2用のテクスチャ指定
    private static final String texturePath = "/mods/tss_pc/textures/gui/computer.png";
    private EntityPlayer thePlayer; // 🌟 プレイヤーを保持する変数
    private ComputerBlockEntity tileEntity;
    private GuiTextField searchBox;
    private GuiTextField tabNameField;


    // スクロールバー制御用
    private boolean isScrolling = false;
    private boolean isFavoriteScrolling  = false;
    private boolean isTabScrolling = false;
    // スクロールバーの表示フラグ
    private boolean showMainScroll = true;
    private boolean showFavScroll = true;
    private boolean showTabScroll = true;

    // ドラッグ・操作用の変数
    private boolean isDraggingItem = false;
    private int pressedSlotIndex = -1;
    private ItemStack draggingStack = null;

    // スクロール位置管理用
    private int tabScrollOffset = 0;
    private float favoriteScrollOffs = 0.0F;

    // スクロール中かどうかのフラグ



    // 定数 (あなたのGUIに合わせて調整してください)
    private static final int MAX_VISIBLE_TABS = 9;

    // 1.16.5風のタブボタン用の座標（画面右外側）
    private int tabX;
    private int selectedTabIndex = 0; // 現在選択中のタブ番号 (0～8)

        private final int MAX_TABS = 9; // 最大タブ数
        // アイテム描画用のスタティックなヘルパー
        private static net.minecraft.client.renderer.entity.RenderItem itemRenderer = new net.minecraft.client.renderer.entity.RenderItem();


    public ComputerScreen(InventoryPlayer inventory, ComputerBlockEntity te) {
            super(new ComputerContainer(inventory, te));
              this.thePlayer = inventory.player; //
            this.tileEntity = te;
            this.xSize = 300; // 1.16.5のimageWidth
        this.ySize = 222; // 1.16.5のimageHeight
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        // 検索ボックス (1.5.2のGuiTextField)
        this.searchBox = new GuiTextField(this.fontRenderer, left + 66, top + 10, 100, 12);
        this.searchBox.setFocused(false);
        this.searchBox.setCanLoseFocus(true);

        // タブ名入力欄
        this.tabNameField = new GuiTextField(this.fontRenderer, left + 192, top + 10, 80, 12);
        List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
        if (this.selectedTabIndex < tabs.size()) {
            this.tabNameField.setText(tabs.get(this.selectedTabIndex).name);
        }
        // ボタン登録 (1.5.2のbuttonList)
        // this.buttonList.add(new GuiButton(...));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.renderEngine.bindTexture("/font/default.png");
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.tabX = left + this.xSize; // タブは画面の右端から始まる
        ComputerContainer container = (ComputerContainer) this.field_73875_a;


        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // --- メイン背景とスロット枠線（前回と同じ） ---
        drawRect(left, top, left + this.xSize, top + this.ySize, 0xCC1E1E1E);

        // 2. 外枠 (0xFF3E3E42)
        int borderColor = 0xFF3E3E42;
        drawRect(left, top, left + this.xSize, top + 1, borderColor);
        drawRect(left, top + this.ySize - 1, left + this.xSize, top + this.ySize, borderColor);
        drawRect(left, top, left + 1, top + this.ySize, borderColor);
        drawRect(left + this.xSize - 1, top, left + this.xSize, top + this.ySize, borderColor);

        // 3. スロットに「枠線」をつけるループ
        // 1.16.5のように、背景(0xFF252526)の上に少し小さい四角を描くことで枠線を表現します
        for (int s = 0; s < this.field_73875_a.field_75151_b.size(); s++) {
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) this.field_73875_a.field_75151_b.get(s);
            int slotX = left + slot.xDisplayPosition - 1;
            int slotY = top + slot.yDisplayPosition - 1;

            // スロットの枠線 (0xFF3E3E42)
            drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF3E3E42);
            // スロットの内側 (0xFF252526) 1ピクセル内側を塗る
            drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF252526);
        }

        // --- 1. メインスクロールバー (中央) ---
        if (showMainScroll) {
            int barX = left + 172;
            int barY = top + 26;
            int barHeight = 90; // レールの全長

            // 全体の行数（5行より多ければ伸縮）
            int totalRows = container.getMaxMainRows();
            // 表示範囲(5行) / 全体行数 の割合で長さを決める
            int knobHeight = (totalRows > 5) ? (int)(barHeight * (5.0 / totalRows)) : barHeight;
            knobHeight = Math.max(knobHeight, 15); // 短くなりすぎないよう最小15px

            // つまみの移動可能範囲内でのY座標
            int scrollableArea = barHeight - knobHeight;
            int knobY = (int) (scrollableArea * container.scrollPos);

            drawRect(barX, barY, barX + 12, barY + barHeight, 0xFF121212); // レール
            drawRect(barX, barY + knobY, barX + 12, barY + knobY + knobHeight, 0xFF007ACC); // つまみ
        }

// ComputerScreen.java: 147行目付近
// --- 2. お気に入りスクロールバー (右側) ---
        List<ComputerBlockEntity.FavoriteTab> tabsList = this.tileEntity.getTabsForPlayer(this.thePlayer);

        int currentTotalSlots = 45;
        // 🌟 ここも tabsList に合わせる
        if (tabsList != null && this.selectedTabIndex < tabsList.size()) {
            currentTotalSlots = tabsList.get(this.selectedTabIndex).slots.size();
        }

        // 行数に応じた表示判定
        this.showFavScroll = currentTotalSlots > 45;

        if (this.showFavScroll) {
            int barX = left + 285;
            int barY = top + 26;
            int barHeight = 162;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            // レール描画
            drawRect(barX, barY, barX + 10, barY + barHeight, 0xFF121212);

            int totalFavRows = (int) Math.ceil(currentTotalSlots / 5.0);
            int knobHeight = (totalFavRows > 9) ? (int)(barHeight * (9.0 / totalFavRows)) : barHeight;
            knobHeight = Math.max(knobHeight, 15);

            int knobY = (int) ((barHeight - knobHeight) * this.favoriteScrollOffs);

            // つまみ描画
            drawRect(barX, barY + knobY, barX + 10, barY + knobY + knobHeight, 0xFF007ACC);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        // --- 3. 右端のタブ切り替えと、そのスクロールバー ---
        // タブ背景板：GUIの最上部から最下部まで
        drawRect(tabX, top + 1, tabX + 25, top + this.ySize - 1, 0xFF2D2D30);
        if (showTabScroll) {
            int barX = tabX + 22;
            int barY = top + 1;
            int barHeight = 180;

            int totalTabs = container.getTotalTabCount();
            // MAX_VISIBLE_TABS(9個) に対して何個あるか
            int knobHeight = (totalTabs > MAX_VISIBLE_TABS) ? (int)(barHeight * ((double)MAX_VISIBLE_TABS / totalTabs)) : barHeight;
            knobHeight = Math.max(knobHeight, 20);

            // 隠れている個数に応じた移動
            int maxOffset = Math.max(1, totalTabs - MAX_VISIBLE_TABS);
            float scrollPct = (float)this.tabScrollOffset / maxOffset;
            int knobY = (int) ((barHeight - knobHeight) * scrollPct);

            drawRect(barX, barY, barX + 2, barY + barHeight, 0xFF121212);
            drawRect(barX, barY + knobY, barX + 2, barY + knobY + knobHeight, 0xFF007ACC);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // --- 4. タブアイコン（数字 or アイテム）の描画 ---
        int tabSpacing = 20;
        java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);

        for (int i = 0; i < MAX_VISIBLE_TABS; i++) {
            // 🌟 実際に表示するデータの番号を計算
            int actualIndex = i + this.tabScrollOffset;

            // データの終端に達したら描画終了
            if (actualIndex >= tabs.size()) break;

            int iconX = left + this.xSize + 4;
            int iconY = top + 10 + (i * tabSpacing); // 描画位置は常に上から i 番目の位置

            ComputerBlockEntity.FavoriteTab currentTab = tabs.get(actualIndex);

            // 🌟 選択中のタブのハイライト判定 (actualIndexを使用)
            if (actualIndex == ((ComputerContainer)this.field_73875_a).selectedTabIndex) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawRect(left + this.xSize, iconY - 2, left + this.xSize + 22, iconY + 18, 0xFF007ACC);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            // アイコンまたは数字の描画
            if (currentTab.icon != null) {
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                itemRenderer.renderItemAndEffectIntoGUI(this.fontRenderer, this.mc.renderEngine, currentTab.icon, iconX, iconY);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            } else {
                String numStr = String.valueOf(actualIndex + 1);
                this.fontRenderer.drawString(numStr, iconX + (numStr.length() > 1 ? 0 : 4), iconY + 4, 0xAAAAAA);
            }
        }
        this.searchBox.drawTextBox();
        this.tabNameField.drawTextBox();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. マウスの押し下げ状態を確認 (0: 左クリック)
        boolean isMouseDown = org.lwjgl.input.Mouse.isButtonDown(0);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        ComputerContainer container = (ComputerContainer) this.field_73875_a;

        // --- スクロールドラッグ処理 ---
        if (isMouseDown) {
            // A. メインスクロールバー (172, 26) 〜 (184, 166)
            if (mouseX >= left + 172 && mouseX <= left + 184 && mouseY >= top + 26 && mouseY <= top + 116) {
                float relY = (float)(mouseY - (top + 26)) / 140.0F; // 140 = バーの全長
                container.scrollPos = Math.max(0.0F, Math.min(1.0F, relY));
                container.updateVisibleSlots();
            }

            // B. お気に入りスクロールバーのドラッグ
            else if (mouseX >= left + 285 && mouseX <= left + 295 && mouseY >= top + 26 && mouseY <= top + 188) {
                // 🌟 修正：レールの全長 (188 - 26 = 162px) で計算
                float relY = (float)(mouseY - (top + 26)) / 162.0F;
                this.favoriteScrollOffs = Math.max(0.0F, Math.min(1.0F, relY));

                // 🌟 修正：コンテナ側の変数も同期させないと、中身が入れ替わりません
                container.favScrollPos = this.favoriteScrollOffs;
                container.updateVisibleSlots();
            }

            // C. 右端タブスクロールバー (tabX + 22, 1) 〜 (+ 24, ySize)
            else if (mouseX >= tabX + 22 && mouseX <= tabX + 24 && mouseY >= top + 10 && mouseY <= top + 190) {
                int totalTabs = container.getTotalTabCount();
                if (totalTabs > MAX_VISIBLE_TABS) {
                    // マウスの相対位置からオフセットを計算
                    float relY = (float)(mouseY - (top + 10)) / 180.0F;
                    this.tabScrollOffset = (int)(relY * (totalTabs - MAX_VISIBLE_TABS));

                    // 範囲外にいかないようクランプ
                    this.tabScrollOffset = Math.max(0, Math.min(totalTabs - MAX_VISIBLE_TABS, this.tabScrollOffset));
                }
            }
        }

        // 2. 基本の描画（背景やスロット、そして super で呼ばれる Foreground 描画）
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 3. ドラッグ中のアイテムアイコン描画 (あなたの既存コード)
        if (this.isDraggingItem && this.draggingStack != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 500);
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            itemRenderer.zLevel = 200.0F;
            itemRenderer.renderItemAndEffectIntoGUI(this.fontRenderer, this.mc.renderEngine, this.draggingStack, mouseX - 8, mouseY - 8);
            itemRenderer.zLevel = 0.0F;
            GL11.glPopMatrix();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString("TSS_PC", 8, 12, 0xAAAAAA);
        this.fontRenderer.drawString("Inventory", 8, 126, 0xAAAAAA);

        // ボタン表示
        this.fontRenderer.drawString("[+Row]", 233, 190, 0xAAAAAA);
        this.fontRenderer.drawString("[-Row]", 260, 190, 0xAAAAAA);
        int buttonsY = 190;
        this.fontRenderer.drawString("[+]", this.xSize + 5, buttonsY, 0x00FF00);
        this.fontRenderer.drawString("[-]", this.xSize + 5, buttonsY + 12, 0xFF0000);

        // 🌟 全スロット（ストレージ0-44 + お気に入り45-89）を対象にするならループを90まで
        for (int i = 0; i < 90; i++) {
            Slot slot = (Slot) this.field_73875_a.field_75151_b.get(i);
            if (slot != null && slot.func_75216_f()) {
                ItemStack stackInSlot = slot.func_75211_c();

                // NBTから "RealCount" を取り出す
                int realCount = 0;
                boolean hasRealCount = false;
                if (stackInSlot.hasTagCompound() && stackInSlot.getTagCompound().hasKey("RealCount")) {
                    realCount = stackInSlot.getTagCompound().getInteger("RealCount");
                    hasRealCount = true;
                } else {
                    realCount = stackInSlot.stackSize;
                }

                // 🌟 修正：数字を描画する条件の変更
                // 1. 通常スロット(0-44)は 1 より大きい場合のみ表示
                // 2. お気に入りスロット(45-89)は、RealCountタグがあれば 0 でも表示
                boolean isFavoriteSlot = (i >= 45 && i < 90);
                if (realCount > 1 || (isFavoriteSlot && hasRealCount)) {

                    String displayStr = formatCount(realCount);
                    float scale = 0.7f;

                    // 🌟 在庫が0の場合は赤色、それ以外は白色
                    int color = (isFavoriteSlot && realCount == 0) ? 0xFF5555 : 0xFFFFFF;

                    org.lwjgl.opengl.GL11.glPushMatrix();
                    org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                    org.lwjgl.opengl.GL11.glScalef(scale, scale, scale);

                    int textWidth = this.fontRenderer.getStringWidth(displayStr);
                    float x = (slot.xDisplayPosition + 16 - 1) / scale - textWidth;
                    float y = (slot.yDisplayPosition + 16 - (7 * scale)) / scale;

                    this.fontRenderer.drawStringWithShadow(displayStr, (int)x, (int)y, color);

                    org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                    org.lwjgl.opengl.GL11.glPopMatrix();
                }
            }
        }
    }
    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0f);
        }
        if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0f);
        }
        return String.valueOf(count);
    }
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        this.searchBox.mouseClicked(mouseX, mouseY, button);
        this.tabNameField.mouseClicked(mouseX, mouseY, button);

        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int relX = mouseX - left;
        int relY = mouseY - top;

        Slot slot = this.getSlotAtPositionEx(mouseX, mouseY);

        if (slot != null) {
            int id = slot.slotNumber;

            // --- 2. ストレージスロット (0-44) ---
            if (id >= 0 && id < 45) {
                if (this.mc.thePlayer.field_71071_by.getItemStack() == null && slot.func_75216_f()) {
                    if (button == 0) {
                        this.isDraggingItem = true;
                        this.draggingStack = MCHelper.itemCopy(slot.func_75211_c());
                        this.pressedSlotIndex = id;

                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slot.func_75211_c(), amount);
                        this.mc.sndManager.playSoundFX("random.click", 0.6F, 1.2F);
                        return;
                    }
                }
            }

            // --- 3. お気に入りスロット (45-89) ---
            else if (id >= 45 && id < 90) {
                // 🌟 修正：どのような操作であっても、お気に入りスロットならここで処理を完結させる
                if (this.mc.thePlayer.field_71071_by.getItemStack() == null && slot.func_75216_f()) {
                    // 左(0)または右(1)クリックで引き出し
                    if (button == 0 || button == 1) {
                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slot.func_75211_c(), amount);
                        this.mc.sndManager.playSoundFX("random.click", 0.6F, 1.2F);
                    }
                }

                // 中クリック(2)で削除などの処理をここに追加可能

                // 🌟 重要：お気に入りスロットの範囲内なら、何があっても return する！
                // これがないと最後に super.mouseClicked が呼ばれ、バニラが「アイテムを掴もう」として
                // お気に入りスロットを空にしてしまいます。
                return;
            }
        }
        if (relY >= 190 && relY <= 200) {
            if (relX >= 233 && relX <= 258) { // [+Row] ボタン
                this.mc.sndManager.playSoundFX("random.click", 1.0F, 1.2F);

                // 🌟 サーバーへパケットを送るのをやめる（または通知のみにする）
                // 🌟 代わりにクライアント側のリストを直接増やす
                List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                if (this.selectedTabIndex < tabs.size()) {
                    for(int i = 0; i < 5; i++) {
                        tabs.get(this.selectedTabIndex).slots.add(null);
                    }
                    // 🌟 ローカルのConfigファイルに即座に書き出す
                    // これにより、自分だけの「行数」が保存され、他のプレイヤーには影響しません
                    FavoriteConfig.saveAll(this.tileEntity.getAllPlayerTabs());
                }

                // 描画フラグを更新
                this.showFavScroll = tabs.get(this.selectedTabIndex).slots.size() > 45;
                return;
            }

            // B. [-Row] ボタンの範囲 (X: 260〜285付近)
            if (relX >= 260 && relX <= 285) {
                this.mc.sndManager.playSoundFX("random.click", 1.0F, 0.8F);

                // 🌟 Client主導: サーバーへパケットを送る必要はありません（または通知のみ）
                // this.sendComputerPacket(1, 8, this.selectedTabIndex, null, 1); // 不要ならコメントアウト

                List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                if (tabs != null && this.selectedTabIndex < tabs.size()) {
                    List<ItemStack> slots = tabs.get(this.selectedTabIndex).slots;

                    // 最低9行（45スロット）は維持するようにガードをかける
                    if (slots.size() > 45) {
                        for (int i = 0; i < 5; i++) {
                            if (!slots.isEmpty()) {
                                slots.remove(slots.size() - 1);
                            }
                        }

                        // 🌟 非常に重要: 自分自身のConfigファイルに即座に保存
                        // これにより「自分だけの行数」が確定し、他人の画面には影響しません
                        FavoriteConfig.saveAll(this.tileEntity.getAllPlayerTabs());

                        // 🌟 表示の更新
                        // 行が減ったことでスクロール位置がはみ出さないよう調整
                        int totalRows = (int) Math.ceil(slots.size() / 5.0);
                        if (totalRows <= 9) {
                            this.favoriteScrollOffs = 0.0F;
                        }

                        // コンテナに最新の状態を反映
                        ComputerContainer container = (ComputerContainer) this.field_73875_a;
                        container.favScrollPos = this.favoriteScrollOffs;
                        container.updateVisibleSlots();
                    }
                }
                return;
            }
        }
        // --- 3. タブのアイコン設定 / 切替 (mouseClicked内) ---
        java.util.List<ComputerBlockEntity.FavoriteTab> tabsForClick = this.tileEntity.getTabsForPlayer(this.mc.thePlayer);

        for (int i = 0; i < MAX_VISIBLE_TABS; i++) {
            // 🌟 見えている範囲のインデックス
            int actualIndex = i + this.tabScrollOffset;
            if (actualIndex >= tabsForClick.size()) break;

            int tabYStart = 10 + (i * 20);
            // マウスがその枠内にあるか判定
            if (relX >= this.xSize && relX <= this.xSize + 25 && relY >= tabYStart && relY <= tabYStart + 20) {
                net.minecraft.item.ItemStack heldItem = this.mc.thePlayer.field_71071_by.getItemStack();

                if (button == 2) { // ホイールクリックで消去
                    sendTabAction(4, actualIndex, "", null);
                } else if (heldItem != null) { // アイコン登録
                    sendTabAction(4, actualIndex, "", heldItem);
                } else { // タブ切り替え
                    this.selectedTabIndex = actualIndex;
                    ((ComputerContainer)this.field_73875_a).selectedTabIndex = actualIndex;

                    // 🌟 これを追加！
                    this.tabNameField.setText(tabsForClick.get(actualIndex).name);

                    sendTabAction(2, actualIndex, "", null);
                    this.mc.sndManager.playSoundFX("random.click", 1.0F, 0.8F);
                }
                return; // タブを処理したら終了
            }
        }

        // --- 4. [+] / [-] ボタンの処理 ---
        if (relX >= this.xSize + 5 && relX <= this.xSize + 25) {
            // --- [+] 追加ボタン ---
            if (relY >= 190 && relY <= 200) {
                this.mc.sndManager.playSoundFX("random.pop", 0.5F, 1.2F);

                java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);

                // 1. サーバーと同じロジックで名前を生成
                String autoName = "Favorite " + (tabs.size() + 1);

                // 2. クライアント側のリストに先行反映（追加）
                tabs.add(new ComputerBlockEntity.FavoriteTab(autoName));

                // 🌟 追加しても selectedTabIndex は変わらないため、setText は呼ばない。
                // これで、今見ているタブの名称が維持されます。

                sendTabAction(0, 0, "", null);
                return;
            }

            // --- [-] 削除ボタン ---
            if (relY >= 202 && relY <= 212) {
                java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);

                // 1. そもそもタブがない、または1個しかない場合は何もしない（ガード）
                if (tabs.size() <= 1) return;

                this.mc.sndManager.playSoundFX("random.click", 1.0F, 0.8F);

                // 2. サーバーへ削除パケット送信
                sendTabAction(1, this.selectedTabIndex, "", null);

                // 3. クライアント側で即座に削除
                tabs.remove(this.selectedTabIndex);

                // 4. 【重要】インデックスの調整
                // 末尾を消した場合は一つ前に戻り、それ以外は同じインデックス（実質次の項目）を指す
                if (this.selectedTabIndex >= tabs.size()) {
                    this.selectedTabIndex = tabs.size() - 1;
                }

                // 5. サーバーへ「今これを選んでいるよ」と念押し（同期）
                ((ComputerContainer)this.field_73875_a).selectedTabIndex = this.selectedTabIndex;
                sendTabAction(2, this.selectedTabIndex, "", null);

                // 6. 🌟 切り替わった先のタブ名称にテキストボックスを更新
                this.tabNameField.setText(tabs.get(this.selectedTabIndex).name);

                // 7. 表示スロットの更新
                ((ComputerContainer)this.field_73875_a).updateVisibleSlots();
                return;
            }
        }

        // --- 5. 最後に通常のクリック処理 ---
        // 🌟 これが呼ばれることで Packet102WindowClick がサーバーへ送信されます。
        super.mouseClicked(mouseX, mouseY, button);
    }
    // パケット送信用の補助メソッド
    private void sendTabPacket(int actionType, int index) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(1);          // packetId = 1 (TabAction)
            dos.writeInt(actionType); // 0:追加, 1:削除 など
            dos.writeInt(index);      // どのタブか

            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = "TSS_PC";
            packet.data = bos.toByteArray();
            packet.length = bos.size();

            // 🌟 (Packet) でキャストして送る
            this.mc.getNetHandler().addToSendQueue((Packet) packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendTabAction(int actionType, int index, String name, ItemStack icon) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // 1. パケットID (1: タブ操作)
            dos.writeByte(1);

            // 2. ActionType (Int) と Index (Int)
            dos.writeInt(actionType);
            dos.writeInt(index);

            // 3. Name (UTF)
            dos.writeUTF(name != null ? name : "");

            // 4. ItemStack の書き込み (ここがサーバー側の readShort 等と対応)
            if (icon == null) {
                dos.writeShort(-1);
            } else {
                dos.writeShort(icon.itemID);
                dos.writeByte(icon.stackSize);
                dos.writeShort(icon.getItemDamage());

                if (icon.hasTagCompound()) {
                    byte[] nbtBytes = net.minecraft.nbt.CompressedStreamTools.compress(icon.getTagCompound());
                    dos.writeShort((short)nbtBytes.length);
                    dos.write(nbtBytes);
                } else {
                    dos.writeShort(-1);
                }
            }

            // 🌟 追記：現在のスクロール位置をパケットの末尾に追加
            ComputerContainer container = (ComputerContainer)this.field_73875_a;
            dos.writeFloat(container.scrollPos);       // メインストレージ用
            dos.writeFloat(this.favoriteScrollOffs);   // お気に入り用

            // 🌟 5. サーバーへ送信 (このログが出るか確認してください！)
            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = "TSS_PC"; // サーバー側と大文字小文字を合わせる
            packet.data = bos.toByteArray();
            packet.length = bos.size();

            System.out.println("CLIENT: Sending TabAction " + actionType + " (Size: " + packet.length + ")");

            // Minecraft 1.5.2 の標準的なパケット送信
            net.minecraft.client.Minecraft.getMinecraft().getNetHandler().addToSendQueue(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void keyTyped(char typedChar, int keyCode) {

        if (this.tabNameField.isFocused()) {
            // Enterキーが押されたら名前を保存
            if (keyCode == 28 || keyCode == 156) {
                String newName = this.tabNameField.getText().trim();
                if (!newName.isEmpty()) {
                    // 🌟 ここを修正：アクションタイプ「3」、アイテム「null」を追加して4つの引数にする
                    sendTabAction(3, this.selectedTabIndex, newName, null);

                    this.tabNameField.setFocused(false);
                }
                return;
            }
        }
        // テキスト入力の処理
        if (this.searchBox.textboxKeyTyped(typedChar, keyCode)) {
            // 検索更新パケットの送信など
            updateSearch();
            return;
        }
        if (this.tabNameField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
    @Override
    protected void handleMouseClick(net.minecraft.inventory.Slot slot, int slotId, int mouseButton, int mode) {
        // --- プレイヤーインベントリからの Shift クリック預け入れ ---
        if (mode == 1 && slotId >= 90) {
            if (slot != null) {
                // 通常のスロットクリック
                sendActionPacket(6, slot.getSlotIndex());
            } else {
                // スロット外クリック（マウスでアイテムを掴んでいるかチェック）
                // 🌟 mc.thePlayer を使うのが1.5.2のGUIでは一番安全です
                if (this.mc.thePlayer.field_71071_by.getItemStack() != null) {
                    sendActionPacket(6, -999);
                }
            }
        }

        if (slotId < 0) {
            super.handleMouseClick(slot, slotId, mouseButton, mode);
            return;
        }

        // --- ストレージスロット (0-44) ---
        // --- ストレージスロット (0-44) ---
        if (slotId >= 0 && slotId < 45) {
            net.minecraft.item.ItemStack heldStack = this.mc.thePlayer.field_71071_by.getItemStack();
            if (heldStack != null) {
                this.sendComputerPacket(6, 0, slotId, heldStack, 0); // 預け入れ
                return; // 🌟 superを呼ばない
            }

            if (slot != null && slot.func_75216_f()) {
                int amount = (mode == 1) ? 64 : 1;
                this.sendComputerPacket(2, 0, slotId, slot.func_75211_c(), amount); // 引き出し
                return; // 🌟 superを呼ばない
            }
            return;
        }

        // --- お気に入りスロット (45-89) ---
        if (slotId >= 45 && slotId < 90) {
            // お気に入り登録 (PacketId: 1, ActionType: 5)
            this.sendComputerPacket(1, 5, slotId - 45, this.mc.thePlayer.field_71071_by.getItemStack(), 0);
            return;
        }

        super.handleMouseClick(slot, slotId, mouseButton, mode);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int left = (this.width - this.xSize) / 2;

        ComputerContainer container = (ComputerContainer) this.field_73875_a;

        if (mouseX < left + 185) { // メインエリア
            float step = 1.0F / Math.max(1, container.getMaxMainRows() - 5);
            container.scrollPos += (wheel > 0 ? -step : step);
            container.scrollPos = Math.max(0, Math.min(1, container.scrollPos));
            container.updateVisibleSlots();
        } else if (mouseX < left + 300) { // お気に入りエリア
            // 🌟 お気に入りのスクロールホイール処理を追加
            List<ComputerBlockEntity.FavoriteTab> fTabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
            if (fTabs != null && this.selectedTabIndex < fTabs.size()) {
                int totalSlots = fTabs.get(this.selectedTabIndex).slots.size();
                int totalRows = (int) Math.ceil(totalSlots / 5.0);
                if (totalRows > 9) {
                    float step = 1.0F / (totalRows - 9);
                    this.favoriteScrollOffs += (wheel > 0 ? -step : step);
                    this.favoriteScrollOffs = Math.max(0, Math.min(1, this.favoriteScrollOffs));
                    container.favScrollPos = this.favoriteScrollOffs;
                    container.updateVisibleSlots();
                }
            }
        } else { // タブエリア
            this.tabScrollOffset += (wheel > 0 ? -1 : 1);
            this.tabScrollOffset = Math.max(0, Math.min(container.getTotalTabCount() - MAX_VISIBLE_TABS, this.tabScrollOffset));
        }
    }

    private void sendActionPacket(int actionID, int slotIndex) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeByte(actionID);
            dos.writeInt(slotIndex);

            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = "TSS_PC";
            packet.data = bos.toByteArray();
            packet.length = bos.size();

            // 🌟 PacketDispatcherを使わない1.5.2の標準的な送り方
            this.mc.getNetHandler().addToSendQueue(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🌟 メソッド名を統一し、型を明示的に指定します
   /* private void sendTabActionPacket(int actionType, int index, net.minecraft.item.ItemStack stack) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
        try {
            dos.writeInt(1); // packetId: 1
            dos.writeInt(actionType);
            dos.writeInt(index);

            // 🌟 ItemStackをNBTとして書き込む処理
            if (stack != null) {
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                stack.writeToNBT(tag);

                // 🌟 一度 byte 配列に圧縮して、その正確な長さを送る
                byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(tag);
                dos.writeShort((short)bytes.length);
                dos.write(bytes);
            } else {
                dos.writeShort((short)-1);
            }

            net.minecraft.network.packet.Packet250CustomPayload packet = new net.minecraft.network.packet.Packet250CustomPayload();
            packet.channel = "TSS_PC";
            packet.data = bos.toByteArray();
            packet.length = bos.size();
            this.mc.getNetHandler().addToSendQueue(packet);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }*/
    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0 && this.isDraggingItem && this.draggingStack != null) {
            Slot targetSlot = this.getSlotAtPositionEx(mouseX, mouseY);
            int left = (this.width - this.xSize) / 2;
            int top = (this.height - this.ySize) / 2;
            int relX = mouseX - left;
            int relY = mouseY - top;

            // --- A. お気に入りスロット(45-89)へドロップ ---
            if (targetSlot != null && targetSlot.slotNumber >= 45 && targetSlot.slotNumber < 90) {
                int favIdx = targetSlot.slotNumber - 45;
                // 🌟 サーバーの handleTabAction(5, ...) を呼び出すためのパケット送信
                this.sendTabAction(5, favIdx, "", this.draggingStack);
                this.mc.sndManager.playSoundFX("random.pop", 0.2F, 1.2F);
            }
            // --- B. タブアイコンへのドロップ ---
            else if (relX >= this.xSize && relX <= this.xSize + 25) {
                // タブの縦位置（描画コードの 10 + i * 20 に合わせる）
                for (int i = 0; i < this.tileEntity.getTabsForPlayer(this.thePlayer).size(); i++) {
                    int tabYStart = 10 + (i * 20);
                    int tabYEnd = tabYStart + 20;

                    if (relY >= tabYStart && relY <= tabYEnd) {
                        // actionType 4: タブアイコン更新 を送信
                        // 個数は不要なので、サーバー側でコピーする際に1個として扱われます
                        this.sendTabAction(4, i, "", this.draggingStack);
                        this.mc.sndManager.playSoundFX("random.orb", 0.2F, 1.0F); // 音を変えると分かりやすい
                        break;
                    }
                }
            }

            // ドラッグ終了のリセット
            this.isDraggingItem = false;
            this.draggingStack = null;
            this.pressedSlotIndex = -1;
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }


    /**
     * 汎用パケット送信メソッド
     * @param packetId 1:タブ, 2:引き出し, 6:預け入れ
     * @param actionType タブ操作の種類など
     * @param index スロット番号
     * @param stack アイテム情報
     * @param amount 引き出し個数（引き出し時のみ使用）
     */
    /**
     * 修正版：サーバーの readByte() 判別方式に完全に準拠した送信メソッド
     */
    private void sendComputerPacket(int packetId, int actionType, int index, net.minecraft.item.ItemStack stack, int amount) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
        try {
            dos.writeByte(packetId);

            if (packetId == 2) {
                // --- 引き出し処理 ---
                if (stack != null) {
                    dos.writeShort((short)stack.itemID);
                    dos.writeByte((byte)stack.stackSize);
                    dos.writeShort((short)stack.getItemDamage());
                    if (stack.stackTagCompound != null) {
                        byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(stack.stackTagCompound);
                        dos.writeShort((short)bytes.length);
                        dos.write(bytes);
                    } else {
                        dos.writeShort((short)-1);
                    }
                } else {
                    dos.writeShort((short)-1);
                    dos.writeByte((byte)0);
                    dos.writeShort((short)0);
                    dos.writeShort((short)-1);
                }
                dos.writeInt(amount);

                // 🌟 追記：サーバー側の readFloat() と対応させる
                ComputerContainer container = (ComputerContainer)this.field_73875_a;
                dos.writeFloat(container.scrollPos);
                dos.writeFloat(this.favoriteScrollOffs);

            } else if (packetId == 1) {
                // --- タブ操作 ---
                dos.writeInt(actionType);
                dos.writeInt(index);
                dos.writeUTF("");

                if (stack != null) {
                    dos.writeShort((short)stack.itemID);
                    dos.writeByte((byte)stack.stackSize);
                    dos.writeShort((short)stack.getItemDamage());
                    if (stack.stackTagCompound != null) {
                        byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(stack.stackTagCompound);
                        dos.writeShort((short)bytes.length);
                        dos.write(bytes);
                    } else {
                        dos.writeShort((short)-1);
                    }
                } else {
                    dos.writeShort((short)-1);
                }

                // 🌟 追記：サーバー側の readFloat() と対応させる
                ComputerContainer container = (ComputerContainer)this.field_73875_a;
                dos.writeFloat(container.scrollPos);
                dos.writeFloat(this.favoriteScrollOffs);


            } else if (packetId == 6) {
                // --- 預け入れ処理 ---
                dos.writeInt(index); // slotIndex
            }

            // 送信処理
            net.minecraft.network.packet.Packet250CustomPayload packet = new net.minecraft.network.packet.Packet250CustomPayload();
            packet.channel = "TSS_PC";
            packet.data = bos.toByteArray();
            packet.length = bos.size();

            if (this.mc.getNetHandler() != null) {
                this.mc.getNetHandler().addToSendQueue(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSearch() {
        ((ComputerContainer) this.field_73875_a).searchText = this.searchBox.getText();
        ((ComputerContainer) this.field_73875_a).updateVisibleSlots();
        // サーバーに検索ワードを伝えるパケット送信処理をここに追加
    }
    private net.minecraft.inventory.Slot getSlotAtPositionEx(int mouseX, int mouseY) {
        for (int i = 0; i < this.field_73875_a.field_75151_b.size(); ++i) {
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot)this.field_73875_a.field_75151_b.get(i);
            // 自作した判定メソッドを呼ぶ
            if (this.isMouseOverSlotEx(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private boolean isMouseOverSlotEx(net.minecraft.inventory.Slot slot, int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        // スロットの座標は GUI の左端・上端からの相対座標なので、それを考慮して判定
        return mouseX >= left + slot.xDisplayPosition - 1 && mouseX <= left + slot.xDisplayPosition + 16 &&
                mouseY >= top + slot.yDisplayPosition - 1 && mouseY <= top + slot.yDisplayPosition + 16;
    }
    private int getActualFavoriteIndex(int slotIndex) {
        // スロット 54 がお気に入りスロットの 0 番目と仮定
        // スクロール位置（行）を考慮して実際のデータ上のインデックスを計算
        int scrollRow = (int) (this.favoriteScrollOffs * Math.max(0, 10 - 9) + 0.5f); // 10は仮の総行数
        int actualIdx = (slotIndex - 54) + (scrollRow * 5);
        return actualIdx;
    }
}