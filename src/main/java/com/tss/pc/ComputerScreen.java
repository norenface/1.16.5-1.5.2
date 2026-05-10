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
    // GuiContainer/GuiScreen のSRG名フィールドをシャドウ (NoSuchFieldError防止)
    protected int xSize = 300;
    protected int ySize = 222;
    protected int width;
    protected int height;
    protected net.minecraft.client.Minecraft mc;
    protected net.minecraft.client.gui.FontRenderer fontRenderer;

    // Slot フィールド (xDisplayPosition/yDisplayPosition/slotNumber) のSRG名が不明なためリフレクションで取得
    private static java.lang.reflect.Field _slotNumber;
    private static java.lang.reflect.Field _slotX;
    private static java.lang.reflect.Field _slotY;
    static {
        java.util.List<java.lang.reflect.Field> intFields = new java.util.ArrayList<>();
        for (java.lang.reflect.Field f : net.minecraft.inventory.Slot.class.getDeclaredFields()) {
            if (f.getType() == int.class) {
                f.setAccessible(true);
                intFields.add(f);
                System.out.println("DEBUG: Slot int field #" + (intFields.size()-1) + " = " + f.getName());
            }
        }
        // MCP 7.51での宣言順: slotNumber(0), xDisplayPosition(1), yDisplayPosition(2)
        if (intFields.size() >= 1) _slotNumber = intFields.get(0);
        if (intFields.size() >= 2) _slotX      = intFields.get(1);
        if (intFields.size() >= 3) _slotY      = intFields.get(2);
    }

    // GuiScreen / Minecraft のSRG名フィールドをリフレクション取得
    private static java.lang.reflect.Field _gsW, _gsH, _gsFR;
    private static java.lang.reflect.Field _mcRE, _mcSnd;
    // Minecraft シングルトン (getMinecraft() は SRG 名のため直接呼び出せない)
    private static net.minecraft.client.Minecraft _mcSingleton;
    // パケット送信用リフレクション
    private static java.lang.reflect.Method _mcGetNetHandler;
    private static java.lang.reflect.Method _nhAddToSendQueue;
    private static java.lang.reflect.Field  _p250Channel, _p250Data, _p250Length;
    // ItemStack フィールド/メソッド (SRG名のため直接アクセス不可)
    private static java.lang.reflect.Field  _isItemID, _isStackSize, _isTagCompound;
    private static java.lang.reflect.Method _isGetDamage, _isHasTag, _isGetTag;
    // CompressedStreamTools.compress(NBTTagCompound)
    private static java.lang.reflect.Method _csCompress;
    // GuiTextField メソッド (全てSRG名のためリフレクション)
    private static java.lang.reflect.Method _gtfSetFocused, _gtfSetCanLoseFocus;
    private static java.lang.reflect.Method _gtfSetText, _gtfGetText, _gtfIsFocused;
    private static java.lang.reflect.Method _gtfDrawTextBox, _gtfMouseClicked, _gtfTextboxKeyTyped;
    // FontRenderer メソッド (SRG名のためリフレクション)
    private static java.lang.reflect.Method _frDrawString, _frDrawStringShadow, _frGetStringWidth;
    // RenderEngine.bindTexture (SRG名のためリフレクション)
    private static java.lang.reflect.Method _reBind;
    // RenderItem.renderItemAndEffectIntoGUI (SRG名のためリフレクション)
    private static java.lang.reflect.Method _riRenderItem;
    // NBTTagCompound メソッド (SRG名のためリフレクション)
    private static java.lang.reflect.Method _nbtHasKey, _nbtGetInteger;

    static {
        // GuiScreen: intフィールドの1番目=width, 2番目=height, FontRendererフィールド=fontRenderer
        java.util.List<java.lang.reflect.Field> gsInts = new java.util.ArrayList<>();
        for (java.lang.reflect.Field f : net.minecraft.client.gui.GuiScreen.class.getDeclaredFields()) {
            try { f.setAccessible(true); } catch (Exception ignored) {}
            Class<?> t = f.getType();
            if (t == int.class) gsInts.add(f);
            else if (t.getSimpleName().contains("FontRenderer")) { _gsFR = f; }
        }
        if (gsInts.size() >= 1) _gsW = gsInts.get(0);
        if (gsInts.size() >= 2) _gsH = gsInts.get(1);

        // Minecraft: static Minecraft フィールド (シングルトン) + RenderEngine + SoundManager
        for (java.lang.reflect.Field f : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
            try { f.setAccessible(true); } catch (Exception ignored) {}
            Class<?> ft = f.getType();
            String sn = ft.getSimpleName();
            if (sn.contains("RenderEngine")) { _mcRE = f; }
            else if (sn.contains("SoundManager")) { _mcSnd = f; }
            else if (ft == net.minecraft.client.Minecraft.class
                    && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                try { Object v = f.get(null); if (v != null) _mcSingleton = (net.minecraft.client.Minecraft)v; }
                catch (Exception ignored) {}
            }
        }

        // Minecraft.getNetHandler() → NetClientHandler
        for (java.lang.reflect.Method m : net.minecraft.client.Minecraft.class.getMethods()) {
            if (m.getParameterTypes().length == 0
                    && m.getReturnType().getSimpleName().contains("NetClientHandler")) {
                _mcGetNetHandler = m; break;
            }
        }

        // NetClientHandler.addToSendQueue(Packet)
        try {
            Class<?> nhClass = Class.forName("net.minecraft.client.multiplayer.NetClientHandler");
            for (java.lang.reflect.Method m : nhClass.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0].getSimpleName().equals("Packet")) {
                    _nhAddToSendQueue = m; break;
                }
            }
        } catch (Exception ignored) {}

        // Packet250CustomPayload: String=channel, byte[]=data, int=length
        for (java.lang.reflect.Field f : net.minecraft.network.packet.Packet250CustomPayload.class.getDeclaredFields()) {
            try { f.setAccessible(true); } catch (Exception ignored) {}
            Class<?> t = f.getType();
            if (t == String.class && _p250Channel == null) _p250Channel = f;
            else if (t == byte[].class  && _p250Data   == null) _p250Data   = f;
            else if (t == int.class     && _p250Length  == null) _p250Length  = f;
        }

        // ItemStack フィールド: int=itemID, int=stackSize, NBTTagCompound=stackTagCompound
        {
            java.util.List<java.lang.reflect.Field> isInts = new java.util.ArrayList<>();
            for (java.lang.reflect.Field f : net.minecraft.item.ItemStack.class.getDeclaredFields()) {
                try { f.setAccessible(true); } catch (Exception ignored) {}
                Class<?> t = f.getType();
                if (t == int.class) isInts.add(f);
                else if (t.getSimpleName().contains("NBTTagCompound") && _isTagCompound == null)
                    _isTagCompound = f;
            }
            if (isInts.size() >= 1) _isItemID    = isInts.get(0); // itemID
            if (isInts.size() >= 2) _isStackSize  = isInts.get(1); // stackSize
        }

        // ItemStack メソッド: getItemDamage()int, hasTagCompound()bool, getTagCompound()NBTTagCompound
        for (java.lang.reflect.Method m : net.minecraft.item.ItemStack.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            Class<?> r = m.getReturnType();
            if (p.length == 0 && r == int.class     && _isGetDamage == null) _isGetDamage = m;
            else if (p.length == 0 && r == boolean.class  && _isHasTag == null)   _isHasTag    = m;
            else if (p.length == 0 && r.getSimpleName().contains("NBTTagCompound") && _isGetTag == null)
                _isGetTag = m;
        }

        // CompressedStreamTools.compress(NBTTagCompound) → byte[]
        try {
            for (java.lang.reflect.Method m : net.minecraft.nbt.CompressedStreamTools.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0].getSimpleName().contains("NBTTagCompound")
                        && m.getReturnType() == byte[].class) {
                    _csCompress = m; break;
                }
            }
        } catch (Exception ignored) {}

        // GuiTextField メソッド (SRG名のため全てリフレクション)
        try {
            java.util.List<java.lang.reflect.Method> gtfBoolVoid = new java.util.ArrayList<>();
            java.util.List<java.lang.reflect.Method> gtfStrVoid  = new java.util.ArrayList<>();
            for (java.lang.reflect.Method m : net.minecraft.client.gui.GuiTextField.class.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Exception ig) {}
                Class<?>[] p = m.getParameterTypes();
                Class<?> r = m.getReturnType();
                if (p.length == 1 && p[0] == boolean.class && r == void.class) {
                    gtfBoolVoid.add(m);
                } else if (p.length == 1 && p[0] == String.class && r == void.class) {
                    gtfStrVoid.add(m);
                } else if (p.length == 0 && r == String.class) {
                    _gtfGetText = m;
                } else if (p.length == 0 && r == boolean.class) {
                    _gtfIsFocused = m;
                } else if (p.length == 0 && r == void.class && _gtfDrawTextBox == null) {
                    _gtfDrawTextBox = m;
                } else if (p.length == 3 && p[0] == int.class && p[1] == int.class && p[2] == int.class && r == void.class) {
                    _gtfMouseClicked = m;
                } else if (p.length == 2 && p[0] == char.class && p[1] == int.class && r == boolean.class) {
                    _gtfTextboxKeyTyped = m;
                }
            }
            if (gtfBoolVoid.size() >= 1) _gtfSetFocused      = gtfBoolVoid.get(0);
            if (gtfBoolVoid.size() >= 2) _gtfSetCanLoseFocus = gtfBoolVoid.get(1);
            if (gtfStrVoid.size()  >= 1) _gtfSetText         = gtfStrVoid.get(0);
        } catch (Exception ignored) {}

        // FontRenderer メソッド (SRG名のためリフレクション)
        try {
            java.util.List<java.lang.reflect.Method> frStrMethods = new java.util.ArrayList<>();
            for (java.lang.reflect.Method m : net.minecraft.client.gui.FontRenderer.class.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Exception ig) {}
                Class<?>[] p = m.getParameterTypes();
                Class<?> r = m.getReturnType();
                if (p.length == 4 && p[0] == String.class && p[1] == int.class
                        && p[2] == int.class && p[3] == int.class) {
                    frStrMethods.add(m);
                } else if (p.length == 1 && p[0] == String.class && r == int.class
                        && _frGetStringWidth == null) {
                    _frGetStringWidth = m;
                }
            }
            if (frStrMethods.size() >= 1) _frDrawString       = frStrMethods.get(0);
            if (frStrMethods.size() >= 2) _frDrawStringShadow = frStrMethods.get(1);
        } catch (Exception ignored) {}

        // RenderEngine.bindTexture (SRG名のためリフレクション)
        try {
            for (java.lang.reflect.Method m : net.minecraft.client.renderer.texture.RenderEngine.class.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Exception ig) {}
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && _reBind == null) { _reBind = m; }
            }
        } catch (Exception ignored) {}

        // RenderItem.renderItemAndEffectIntoGUI (SRG名のためリフレクション)
        try {
            for (java.lang.reflect.Method m : net.minecraft.client.renderer.entity.RenderItem.class.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Exception ig) {}
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 5 && p[2].getSimpleName().contains("ItemStack")
                        && p[3] == int.class && p[4] == int.class && _riRenderItem == null) {
                    _riRenderItem = m;
                }
            }
        } catch (Exception ignored) {}

        // NBTTagCompound: hasKey(String)→bool, getInteger(String)→int (SRG名のためリフレクション)
        try {
            for (java.lang.reflect.Method m : net.minecraft.nbt.NBTTagCompound.class.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Exception ig) {}
                Class<?>[] p = m.getParameterTypes();
                Class<?> r = m.getReturnType();
                if (p.length == 1 && p[0] == String.class && r == boolean.class && _nbtHasKey == null)
                    _nbtHasKey = m;
                else if (p.length == 1 && p[0] == String.class && r == int.class && _nbtGetInteger == null)
                    _nbtGetInteger = m;
            }
        } catch (Exception ignored) {}
    }

    private static int slotNum(net.minecraft.inventory.Slot s) {
        try { if (_slotNumber != null) return _slotNumber.getInt(s); } catch (Exception e) {}
        return s.slotNumber;
    }
    private static int slotX(net.minecraft.inventory.Slot s) {
        try { if (_slotX != null) return _slotX.getInt(s); } catch (Exception e) {}
        return s.xDisplayPosition;
    }
    private static int slotY(net.minecraft.inventory.Slot s) {
        try { if (_slotY != null) return _slotY.getInt(s); } catch (Exception e) {}
        return s.yDisplayPosition;
    }

    // 1.5.2用のテクスチャ指定
    private static final String texturePath = "/mods/tss_pc/textures/gui/computer.png";
    private EntityPlayer thePlayer;
    private ComputerContainer container; // field_73875_a の SRG 名が違うため自クラスで保持
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

    // 定数
    private static final int MAX_VISIBLE_TABS = 9;

    private int tabX;
    private int selectedTabIndex = 0;

    private final int MAX_TABS = 9;
    private static net.minecraft.client.renderer.entity.RenderItem itemRenderer =
            new net.minecraft.client.renderer.entity.RenderItem();

    // GuiContainer.inventorySlots の SRG 名が違うため、コンテナを super に渡す前に変数に保存する
    public ComputerScreen(InventoryPlayer inventory, EntityPlayer playerObj, ComputerBlockEntity te) {
        this(new ComputerContainer(inventory, playerObj, te), playerObj, te);
    }
    private ComputerScreen(ComputerContainer cont, EntityPlayer playerObj, ComputerBlockEntity te) {
        super(cont);
        this.container = cont;
        this.thePlayer = playerObj;
        this.tileEntity = te;
        this.xSize = 300;
        this.ySize = 222;
    }

    @Override
    public void func_73866_w() {
        syncGuiFields();
        super.func_73866_w();
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        this.searchBox = new GuiTextField(this.fontRenderer, left + 66, top + 10, 100, 12);
        gtfSetFocused(this.searchBox, false);
        gtfSetCanLoseFocus(this.searchBox, true);

        this.tabNameField = new GuiTextField(this.fontRenderer, left + 192, top + 10, 80, 12);
        List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
        if (this.selectedTabIndex < tabs.size()) {
            gtfSetText(this.tabNameField, tabs.get(this.selectedTabIndex).name);
        }
    }

    @Override
    protected void func_74185_a(float partialTicks, int mouseX, int mouseY) {
        if (this.searchBox == null) return;
        net.minecraft.client.renderer.texture.RenderEngine re = getRenderEngine();
        if (re != null) reBind(re, "/font/default.png");
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.tabX = left + this.xSize;
        ComputerContainer container = this.container;

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // メイン背景
        drawColoredRect(left, top, left + this.xSize, top + this.ySize, 0xCC1E1E1E);

        // 外枠
        int borderColor = 0xFF3E3E42;
        drawColoredRect(left, top, left + this.xSize, top + 1, borderColor);
        drawColoredRect(left, top + this.ySize - 1, left + this.xSize, top + this.ySize, borderColor);
        drawColoredRect(left, top, left + 1, top + this.ySize, borderColor);
        drawColoredRect(left + this.xSize - 1, top, left + this.xSize, top + this.ySize, borderColor);

        // スロット枠線ループ
        for (int s = 0; s < this.container.getSlots().size(); s++) {
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) this.container.getSlots().get(s);
            int slotX = left + slotX(slot) - 1;
            int slotY = top + slotY(slot) - 1;
            drawColoredRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF3E3E42);
            drawColoredRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF252526);
        }

        // メインスクロールバー
        if (showMainScroll) {
            int barX = left + 172;
            int barY = top + 26;
            int barHeight = 90;
            int totalRows = container.getMaxMainRows();
            int knobHeight = (totalRows > 5) ? (int)(barHeight * (5.0 / totalRows)) : barHeight;
            knobHeight = Math.max(knobHeight, 15);
            int scrollableArea = barHeight - knobHeight;
            int knobY = (int) (scrollableArea * container.scrollPos);
            drawColoredRect(barX, barY, barX + 12, barY + barHeight, 0xFF121212);
            drawColoredRect(barX, barY + knobY, barX + 12, barY + knobY + knobHeight, 0xFF007ACC);
        }

        // お気に入りスクロールバー
        List<ComputerBlockEntity.FavoriteTab> tabsList = this.tileEntity.getTabsForPlayer(this.thePlayer);
        int currentTotalSlots = 45;
        if (tabsList != null && this.selectedTabIndex < tabsList.size()) {
            currentTotalSlots = tabsList.get(this.selectedTabIndex).slots.size();
        }
        this.showFavScroll = currentTotalSlots > 45;

        if (this.showFavScroll) {
            int barX = left + 285;
            int barY = top + 26;
            int barHeight = 162;
            drawColoredRect(barX, barY, barX + 10, barY + barHeight, 0xFF121212);
            int totalFavRows = (int) Math.ceil(currentTotalSlots / 5.0);
            int knobHeight = (totalFavRows > 9) ? (int)(barHeight * (9.0 / totalFavRows)) : barHeight;
            knobHeight = Math.max(knobHeight, 15);
            int knobY = (int) ((barHeight - knobHeight) * this.favoriteScrollOffs);
            drawColoredRect(barX, barY + knobY, barX + 10, barY + knobY + knobHeight, 0xFF007ACC);
        }

        // タブ背景板
        drawColoredRect(tabX, top + 1, tabX + 25, top + this.ySize - 1, 0xFF2D2D30);
        if (showTabScroll) {
            int barX = tabX + 22;
            int barY = top + 1;
            int barHeight = 180;
            int totalTabs = container.getTotalTabCount();
            int knobHeight = (totalTabs > MAX_VISIBLE_TABS)
                    ? (int)(barHeight * ((double)MAX_VISIBLE_TABS / totalTabs)) : barHeight;
            knobHeight = Math.max(knobHeight, 20);
            int maxOffset = Math.max(1, totalTabs - MAX_VISIBLE_TABS);
            float scrollPct = (float)this.tabScrollOffset / maxOffset;
            int knobY = (int) ((barHeight - knobHeight) * scrollPct);
            drawColoredRect(barX, barY, barX + 2, barY + barHeight, 0xFF121212);
            drawColoredRect(barX, barY + knobY, barX + 2, barY + knobY + knobHeight, 0xFF007ACC);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // タブアイコン描画
        int tabSpacing = 20;
        java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
        net.minecraft.client.renderer.texture.RenderEngine reForTabs = getRenderEngine();

        for (int i = 0; i < MAX_VISIBLE_TABS; i++) {
            int actualIndex = i + this.tabScrollOffset;
            if (actualIndex >= tabs.size()) break;

            int iconX = left + this.xSize + 4;
            int iconY = top + 10 + (i * tabSpacing);

            ComputerBlockEntity.FavoriteTab currentTab = tabs.get(actualIndex);

            if (actualIndex == this.container.selectedTabIndex) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawColoredRect(left + this.xSize, iconY - 2, left + this.xSize + 22, iconY + 18, 0xFF007ACC);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            if (currentTab.icon != null) {
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                riRenderItem(itemRenderer, this.fontRenderer, reForTabs, currentTab.icon, iconX, iconY);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            } else {
                String numStr = String.valueOf(actualIndex + 1);
                frDrawStr(this.fontRenderer, numStr, iconX + (numStr.length() > 1 ? 0 : 4), iconY + 4, 0xAAAAAA);
            }
        }
        gtfDrawTextBox(this.searchBox);
        gtfDrawTextBox(this.tabNameField);
    }

    @Override
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        syncGuiFields();
        boolean isMouseDown = org.lwjgl.input.Mouse.isButtonDown(0);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        ComputerContainer container = this.container;

        if (isMouseDown) {
            if (mouseX >= left + 172 && mouseX <= left + 184 && mouseY >= top + 26 && mouseY <= top + 116) {
                float relY = (float)(mouseY - (top + 26)) / 140.0F;
                container.scrollPos = Math.max(0.0F, Math.min(1.0F, relY));
                container.updateVisibleSlots();
            } else if (mouseX >= left + 285 && mouseX <= left + 295 && mouseY >= top + 26 && mouseY <= top + 188) {
                float relY = (float)(mouseY - (top + 26)) / 162.0F;
                this.favoriteScrollOffs = Math.max(0.0F, Math.min(1.0F, relY));
                container.favScrollPos = this.favoriteScrollOffs;
                container.updateVisibleSlots();
            } else if (mouseX >= tabX + 22 && mouseX <= tabX + 24 && mouseY >= top + 10 && mouseY <= top + 190) {
                int totalTabs = container.getTotalTabCount();
                if (totalTabs > MAX_VISIBLE_TABS) {
                    float relY = (float)(mouseY - (top + 10)) / 180.0F;
                    this.tabScrollOffset = (int)(relY * (totalTabs - MAX_VISIBLE_TABS));
                    this.tabScrollOffset = Math.max(0, Math.min(totalTabs - MAX_VISIBLE_TABS, this.tabScrollOffset));
                }
            }
        }

        super.func_73863_a(mouseX, mouseY, partialTicks);

        if (this.isDraggingItem && this.draggingStack != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 500);
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            itemRenderer.zLevel = 200.0F;
            riRenderItem(itemRenderer, this.fontRenderer, getRenderEngine(), this.draggingStack, mouseX - 8, mouseY - 8);
            itemRenderer.zLevel = 0.0F;
            GL11.glPopMatrix();
        }
    }

    @Override
    protected void func_74184_a(int mouseX, int mouseY) {
        frDrawStr(this.fontRenderer, "TSS_PC", 8, 12, 0xAAAAAA);
        frDrawStr(this.fontRenderer, "Inventory", 8, 126, 0xAAAAAA);
        frDrawStr(this.fontRenderer, "[+Row]", 233, 190, 0xAAAAAA);
        frDrawStr(this.fontRenderer, "[-Row]", 260, 190, 0xAAAAAA);
        int buttonsY = 190;
        frDrawStr(this.fontRenderer, "[+]", this.xSize + 5, buttonsY, 0x00FF00);
        frDrawStr(this.fontRenderer, "[-]", this.xSize + 5, buttonsY + 12, 0xFF0000);

        for (int i = 0; i < 90; i++) {
            Slot slot = (Slot) this.container.getSlots().get(i);
            if (slot != null && slot.func_75216_f()) {
                ItemStack stackInSlot = slot.func_75211_c();

                int realCount = 0;
                boolean hasRealCount = false;
                net.minecraft.nbt.NBTTagCompound tag = isGetTag(stackInSlot);
                if (isHasTag(stackInSlot) && nbtHasKey(tag, "RealCount")) {
                    realCount = nbtGetInt(tag, "RealCount");
                    hasRealCount = true;
                } else {
                    realCount = isStackSize(stackInSlot);
                }

                boolean isFavoriteSlot = (i >= 45 && i < 90);
                if (realCount > 1 || (isFavoriteSlot && hasRealCount)) {
                    String displayStr = formatCount(realCount);
                    float scale = 0.7f;
                    int color = (isFavoriteSlot && realCount == 0) ? 0xFF5555 : 0xFFFFFF;

                    org.lwjgl.opengl.GL11.glPushMatrix();
                    org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                    org.lwjgl.opengl.GL11.glScalef(scale, scale, scale);

                    int textWidth = frGetStrWidth(this.fontRenderer, displayStr);
                    float x = (slotX(slot) + 16 - 1) / scale - textWidth;
                    float y = (slotY(slot) + 16 - (7 * scale)) / scale;

                    frDrawStrShadow(this.fontRenderer, displayStr, (int)x, (int)y, color);

                    org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                    org.lwjgl.opengl.GL11.glPopMatrix();
                }
            }
        }
    }

    private String formatCount(int count) {
        if (count >= 1000000) return String.format("%.1fM", count / 1000000.0f);
        if (count >= 1000) return String.format("%.1fK", count / 1000.0f);
        return String.valueOf(count);
    }

    @Override
    protected void func_73864_a(int mouseX, int mouseY, int button) {
        gtfMouseClicked(this.searchBox, mouseX, mouseY, button);
        gtfMouseClicked(this.tabNameField, mouseX, mouseY, button);

        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int relX = mouseX - left;
        int relY = mouseY - top;

        Slot slot = this.getSlotAtPositionEx(mouseX, mouseY);

        if (slot != null) {
            int id = slotNum(slot);

            if (id >= 0 && id < 45) {
                if (this.thePlayer.field_71071_by.getItemStack() == null && slot.func_75216_f()) {
                    if (button == 0) {
                        this.isDraggingItem = true;
                        this.draggingStack = MCHelper.itemCopy(slot.func_75211_c());
                        this.pressedSlotIndex = id;
                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slot.func_75211_c(), amount);
                        playSoundFX("random.click", 0.6F, 1.2F);
                        return;
                    }
                }
            } else if (id >= 45 && id < 90) {
                if (this.thePlayer.field_71071_by.getItemStack() == null && slot.func_75216_f()) {
                    if (button == 0 || button == 1) {
                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slot.func_75211_c(), amount);
                        playSoundFX("random.click", 0.6F, 1.2F);
                    }
                }
                return;
            }
        }

        if (relY >= 190 && relY <= 200) {
            if (relX >= 233 && relX <= 258) {
                playSoundFX("random.click", 1.0F, 1.2F);
                List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                if (this.selectedTabIndex < tabs.size()) {
                    for(int i = 0; i < 5; i++) tabs.get(this.selectedTabIndex).slots.add(null);
                    FavoriteConfig.saveAll(this.tileEntity.getAllPlayerTabs());
                }
                this.showFavScroll = tabs.get(this.selectedTabIndex).slots.size() > 45;
                return;
            }
            if (relX >= 260 && relX <= 285) {
                playSoundFX("random.click", 1.0F, 0.8F);
                List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                if (tabs != null && this.selectedTabIndex < tabs.size()) {
                    List<ItemStack> slots = tabs.get(this.selectedTabIndex).slots;
                    if (slots.size() > 45) {
                        for (int i = 0; i < 5; i++) {
                            if (!slots.isEmpty()) slots.remove(slots.size() - 1);
                        }
                        FavoriteConfig.saveAll(this.tileEntity.getAllPlayerTabs());
                        int totalRows = (int) Math.ceil(slots.size() / 5.0);
                        if (totalRows <= 9) this.favoriteScrollOffs = 0.0F;
                        ComputerContainer container = this.container;
                        container.favScrollPos = this.favoriteScrollOffs;
                        container.updateVisibleSlots();
                    }
                }
                return;
            }
        }

        java.util.List<ComputerBlockEntity.FavoriteTab> tabsForClick = this.tileEntity.getTabsForPlayer(this.thePlayer);
        for (int i = 0; i < MAX_VISIBLE_TABS; i++) {
            int actualIndex = i + this.tabScrollOffset;
            if (actualIndex >= tabsForClick.size()) break;

            int tabYStart = 10 + (i * 20);
            if (relX >= this.xSize && relX <= this.xSize + 25 && relY >= tabYStart && relY <= tabYStart + 20) {
                net.minecraft.item.ItemStack heldItem = this.thePlayer.field_71071_by.getItemStack();
                if (button == 2) {
                    sendTabAction(4, actualIndex, "", null);
                } else if (heldItem != null) {
                    sendTabAction(4, actualIndex, "", heldItem);
                } else {
                    this.selectedTabIndex = actualIndex;
                    this.container.selectedTabIndex = actualIndex;
                    gtfSetText(this.tabNameField, tabsForClick.get(actualIndex).name);
                    sendTabAction(2, actualIndex, "", null);
                    playSoundFX("random.click", 1.0F, 0.8F);
                }
                return;
            }
        }

        if (relX >= this.xSize + 5 && relX <= this.xSize + 25) {
            if (relY >= 190 && relY <= 200) {
                playSoundFX("random.pop", 0.5F, 1.2F);
                java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                String autoName = "Favorite " + (tabs.size() + 1);
                tabs.add(new ComputerBlockEntity.FavoriteTab(autoName));
                sendTabAction(0, 0, "", null);
                return;
            }
            if (relY >= 202 && relY <= 212) {
                java.util.List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
                if (tabs.size() <= 1) return;
                playSoundFX("random.click", 1.0F, 0.8F);
                sendTabAction(1, this.selectedTabIndex, "", null);
                tabs.remove(this.selectedTabIndex);
                if (this.selectedTabIndex >= tabs.size()) this.selectedTabIndex = tabs.size() - 1;
                this.container.selectedTabIndex = this.selectedTabIndex;
                sendTabAction(2, this.selectedTabIndex, "", null);
                gtfSetText(this.tabNameField, tabs.get(this.selectedTabIndex).name);
                this.container.updateVisibleSlots();
                return;
            }
        }

        super.func_73864_a(mouseX, mouseY, button);
    }

    private void sendTabPacket(int actionType, int index) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(1);
            dos.writeInt(actionType);
            dos.writeInt(index);
            sendToServer(bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendTabAction(int actionType, int index, String name, ItemStack icon) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeByte(1);
            dos.writeInt(actionType);
            dos.writeInt(index);
            dos.writeUTF(name != null ? name : "");

            if (icon == null) {
                dos.writeShort(-1);
            } else {
                dos.writeShort(isItemID(icon));
                dos.writeByte(isStackSize(icon));
                dos.writeShort(isGetDamage(icon));
                net.minecraft.nbt.NBTTagCompound tag = isTagField(icon);
                if (tag != null) {
                    byte[] nbtBytes = compressNBT(tag);
                    dos.writeShort((short)nbtBytes.length);
                    dos.write(nbtBytes);
                } else {
                    dos.writeShort(-1);
                }
            }

            ComputerContainer container = this.container;
            dos.writeFloat(container.scrollPos);
            dos.writeFloat(this.favoriteScrollOffs);
            sendToServer(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void func_73869_a(char typedChar, int keyCode) {
        if (gtfIsFocused(this.tabNameField)) {
            if (keyCode == 28 || keyCode == 156) {
                String newName = gtfGetText(this.tabNameField).trim();
                if (!newName.isEmpty()) {
                    sendTabAction(3, this.selectedTabIndex, newName, null);
                    gtfSetFocused(this.tabNameField, false);
                }
                return;
            }
        }
        if (gtfTextboxKeyTyped(this.searchBox, typedChar, keyCode)) {
            updateSearch();
            return;
        }
        if (gtfTextboxKeyTyped(this.tabNameField, typedChar, keyCode)) {
            return;
        }
        super.func_73869_a(typedChar, keyCode);
    }

    @Override
    protected void handleMouseClick(net.minecraft.inventory.Slot slot, int slotId, int mouseButton, int mode) {
        if (mode == 1 && slotId >= 90) {
            if (slot != null) {
                sendActionPacket(6, slotNum(slot));
            } else {
                if (this.thePlayer.field_71071_by.getItemStack() != null) {
                    sendActionPacket(6, -999);
                }
            }
        }

        if (slotId < 0) {
            super.handleMouseClick(slot, slotId, mouseButton, mode);
            return;
        }

        if (slotId >= 0 && slotId < 45) {
            net.minecraft.item.ItemStack heldStack = this.thePlayer.field_71071_by.getItemStack();
            if (heldStack != null) {
                this.sendComputerPacket(6, 0, slotId, heldStack, 0);
                return;
            }
            if (slot != null && slot.func_75216_f()) {
                int amount = (mode == 1) ? 64 : 1;
                this.sendComputerPacket(2, 0, slotId, slot.func_75211_c(), amount);
                return;
            }
            return;
        }

        if (slotId >= 45 && slotId < 90) {
            this.sendComputerPacket(1, 5, slotId - 45, this.thePlayer.field_71071_by.getItemStack(), 0);
            return;
        }

        super.handleMouseClick(slot, slotId, mouseButton, mode);
    }

    @Override
    public void func_73867_d() {
        super.func_73867_d();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / org.lwjgl.opengl.Display.getWidth();
        int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / org.lwjgl.opengl.Display.getHeight() - 1;
        int left = (this.width - this.xSize) / 2;
        ComputerContainer container = this.container;

        if (mouseX < left + 185) {
            float step = 1.0F / Math.max(1, container.getMaxMainRows() - 5);
            container.scrollPos += (wheel > 0 ? -step : step);
            container.scrollPos = Math.max(0, Math.min(1, container.scrollPos));
            container.updateVisibleSlots();
        } else if (mouseX < left + 300) {
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
        } else {
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
            sendToServer(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void func_73869_b(int mouseX, int mouseY, int which) {
        if (which == 0 && this.isDraggingItem && this.draggingStack != null) {
            Slot targetSlot = this.getSlotAtPositionEx(mouseX, mouseY);
            int left = (this.width - this.xSize) / 2;
            int top = (this.height - this.ySize) / 2;
            int relX = mouseX - left;
            int relY = mouseY - top;

            if (targetSlot != null && slotNum(targetSlot) >= 45 && slotNum(targetSlot) < 90) {
                int favIdx = slotNum(targetSlot) - 45;
                this.sendTabAction(5, favIdx, "", this.draggingStack);
                playSoundFX("random.pop", 0.2F, 1.2F);
            } else if (relX >= this.xSize && relX <= this.xSize + 25) {
                for (int i = 0; i < this.tileEntity.getTabsForPlayer(this.thePlayer).size(); i++) {
                    int tabYStart = 10 + (i * 20);
                    int tabYEnd = tabYStart + 20;
                    if (relY >= tabYStart && relY <= tabYEnd) {
                        this.sendTabAction(4, i, "", this.draggingStack);
                        playSoundFX("random.orb", 0.2F, 1.0F);
                        break;
                    }
                }
            }

            this.isDraggingItem = false;
            this.draggingStack = null;
            this.pressedSlotIndex = -1;
        }
        super.func_73869_b(mouseX, mouseY, which);
    }

    private void sendComputerPacket(int packetId, int actionType, int index, net.minecraft.item.ItemStack stack, int amount) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
        try {
            dos.writeByte(packetId);

            if (packetId == 2) {
                if (stack != null) {
                    dos.writeShort((short)isItemID(stack));
                    dos.writeByte((byte)isStackSize(stack));
                    dos.writeShort((short)isGetDamage(stack));
                    net.minecraft.nbt.NBTTagCompound tag = isTagField(stack);
                    if (tag != null) {
                        byte[] bytes = compressNBT(tag);
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
                ComputerContainer container = this.container;
                dos.writeFloat(container.scrollPos);
                dos.writeFloat(this.favoriteScrollOffs);

            } else if (packetId == 1) {
                dos.writeInt(actionType);
                dos.writeInt(index);
                dos.writeUTF("");
                if (stack != null) {
                    dos.writeShort((short)isItemID(stack));
                    dos.writeByte((byte)isStackSize(stack));
                    dos.writeShort((short)isGetDamage(stack));
                    net.minecraft.nbt.NBTTagCompound tag = isTagField(stack);
                    if (tag != null) {
                        byte[] bytes = compressNBT(tag);
                        dos.writeShort((short)bytes.length);
                        dos.write(bytes);
                    } else {
                        dos.writeShort((short)-1);
                    }
                } else {
                    dos.writeShort((short)-1);
                }
                ComputerContainer container = this.container;
                dos.writeFloat(container.scrollPos);
                dos.writeFloat(this.favoriteScrollOffs);

            } else if (packetId == 6) {
                dos.writeInt(index);
            }

            sendToServer(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSearch() {
        (this.container).searchText = gtfGetText(this.searchBox);
        (this.container).updateVisibleSlots();
    }

    // -------------------------------------------------------------------------
    // Minecraft / GuiScreen SRGフィールドアクセサ
    // -------------------------------------------------------------------------

    private static net.minecraft.client.Minecraft getMCInstance() {
        if (_mcSingleton != null) return _mcSingleton;
        for (java.lang.reflect.Field f : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && f.getType() == net.minecraft.client.Minecraft.class) {
                    Object v = f.get(null);
                    if (v != null) { _mcSingleton = (net.minecraft.client.Minecraft) v; return _mcSingleton; }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void syncGuiFields() {
        net.minecraft.client.Minecraft mcInst = getMCInstance();
        if (mcInst != null) this.mc = mcInst;
        try { if (_gsW  != null) this.width  = _gsW.getInt(this); } catch (Exception ignored) {}
        try { if (_gsH  != null) this.height = _gsH.getInt(this); } catch (Exception ignored) {}
        try { if (_gsFR != null) this.fontRenderer = (net.minecraft.client.gui.FontRenderer) _gsFR.get(this); } catch (Exception ignored) {}
    }

    private net.minecraft.client.renderer.texture.RenderEngine getRenderEngine() {
        if (_mcRE != null && this.mc != null) try {
            return (net.minecraft.client.renderer.texture.RenderEngine) _mcRE.get(this.mc);
        } catch (Exception ignored) {}
        return null;
    }

    private net.minecraft.client.audio.SoundManager getSndManager() {
        if (_mcSnd != null && this.mc != null) try {
            return (net.minecraft.client.audio.SoundManager) _mcSnd.get(this.mc);
        } catch (Exception ignored) {}
        return null;
    }

    private void sendToServer(byte[] data) {
        try {
            net.minecraft.client.Minecraft mcInst = (this.mc != null) ? this.mc : getMCInstance();
            if (mcInst == null || _mcGetNetHandler == null || _nhAddToSendQueue == null) return;
            Object nh = _mcGetNetHandler.invoke(mcInst);
            if (nh == null) return;
            net.minecraft.network.packet.Packet250CustomPayload pkt =
                    new net.minecraft.network.packet.Packet250CustomPayload();
            if (_p250Channel != null) _p250Channel.set(pkt, "TSS_PC"); else pkt.channel = "TSS_PC";
            if (_p250Data    != null) _p250Data.set(pkt, data);         else pkt.data    = data;
            if (_p250Length  != null) _p250Length.setInt(pkt, data.length); else pkt.length = data.length;
            _nhAddToSendQueue.invoke(nh, pkt);
        } catch (Exception e) {
            System.out.println("DEBUG: sendToServer err: " + e);
        }
    }

    // -------------------------------------------------------------------------
    // ItemStack / NBT リフレクションアクセサ
    // -------------------------------------------------------------------------

    private static int isItemID(net.minecraft.item.ItemStack s) {
        if (_isItemID != null) try { return _isItemID.getInt(s); } catch (Exception ignored) {}
        return s.itemID;
    }
    private static int isStackSize(net.minecraft.item.ItemStack s) {
        if (_isStackSize != null) try { return _isStackSize.getInt(s); } catch (Exception ignored) {}
        return s.stackSize;
    }
    private static int isGetDamage(net.minecraft.item.ItemStack s) {
        if (_isGetDamage != null) try { return (Integer)_isGetDamage.invoke(s); } catch (Exception ignored) {}
        return s.getItemDamage();
    }
    private static boolean isHasTag(net.minecraft.item.ItemStack s) {
        if (_isHasTag != null) try { return (Boolean)_isHasTag.invoke(s); } catch (Exception ignored) {}
        return s.hasTagCompound();
    }
    private static net.minecraft.nbt.NBTTagCompound isGetTag(net.minecraft.item.ItemStack s) {
        if (_isGetTag != null) try { return (net.minecraft.nbt.NBTTagCompound)_isGetTag.invoke(s); } catch (Exception ignored) {}
        return s.getTagCompound();
    }
    private static net.minecraft.nbt.NBTTagCompound isTagField(net.minecraft.item.ItemStack s) {
        if (_isTagCompound != null) try { return (net.minecraft.nbt.NBTTagCompound)_isTagCompound.get(s); } catch (Exception ignored) {}
        return s.stackTagCompound;
    }
    private static byte[] compressNBT(net.minecraft.nbt.NBTTagCompound tag) {
        if (_csCompress != null) try { return (byte[])_csCompress.invoke(null, tag); } catch (Exception ignored) {}
        try { return net.minecraft.nbt.CompressedStreamTools.compress(tag); } catch (Exception e) { return new byte[0]; }
    }

    private static boolean nbtHasKey(net.minecraft.nbt.NBTTagCompound tag, String key) {
        if (tag == null) return false;
        if (_nbtHasKey != null) try { return (Boolean) _nbtHasKey.invoke(tag, key); } catch (Exception ig) {}
        try { return tag.hasKey(key); } catch (Exception ignored) { return false; }
    }
    private static int nbtGetInt(net.minecraft.nbt.NBTTagCompound tag, String key) {
        if (tag == null) return 0;
        if (_nbtGetInteger != null) try { return (Integer) _nbtGetInteger.invoke(tag, key); } catch (Exception ig) {}
        try { return tag.getInteger(key); } catch (Exception ignored) { return 0; }
    }

    // -------------------------------------------------------------------------
    // GuiTextField プロキシ (SRG名のため直接呼び出せないメソッドをリフレクション経由で呼ぶ)
    // -------------------------------------------------------------------------

    private static void gtfSetFocused(GuiTextField f, boolean v) {
        if (_gtfSetFocused != null) try { _gtfSetFocused.invoke(f, v); return; } catch (Exception ig) {}
        try { f.setFocused(v); } catch (Exception ignored) {}
    }
    private static void gtfSetCanLoseFocus(GuiTextField f, boolean v) {
        if (_gtfSetCanLoseFocus != null) try { _gtfSetCanLoseFocus.invoke(f, v); return; } catch (Exception ig) {}
        try { f.setCanLoseFocus(v); } catch (Exception ignored) {}
    }
    private static void gtfSetText(GuiTextField f, String s) {
        if (_gtfSetText != null) try { _gtfSetText.invoke(f, s); return; } catch (Exception ig) {}
        try { f.setText(s); } catch (Exception ignored) {}
    }
    private static String gtfGetText(GuiTextField f) {
        if (_gtfGetText != null) try { return (String) _gtfGetText.invoke(f); } catch (Exception ig) {}
        try { return f.getText(); } catch (Exception ignored) { return ""; }
    }
    private static boolean gtfIsFocused(GuiTextField f) {
        if (_gtfIsFocused != null) try { return (Boolean) _gtfIsFocused.invoke(f); } catch (Exception ig) {}
        try { return f.isFocused(); } catch (Exception ignored) { return false; }
    }
    private static void gtfDrawTextBox(GuiTextField f) {
        if (_gtfDrawTextBox != null) try { _gtfDrawTextBox.invoke(f); return; } catch (Exception ig) {}
        try { f.drawTextBox(); } catch (Exception ignored) {}
    }
    private static void gtfMouseClicked(GuiTextField f, int x, int y, int btn) {
        if (_gtfMouseClicked != null) try { _gtfMouseClicked.invoke(f, x, y, btn); return; } catch (Exception ig) {}
        try { f.mouseClicked(x, y, btn); } catch (Exception ignored) {}
    }
    private static boolean gtfTextboxKeyTyped(GuiTextField f, char c, int key) {
        if (_gtfTextboxKeyTyped != null) try { return (Boolean) _gtfTextboxKeyTyped.invoke(f, c, key); } catch (Exception ig) {}
        try { return f.textboxKeyTyped(c, key); } catch (Exception ignored) { return false; }
    }

    // -------------------------------------------------------------------------
    // FontRenderer プロキシ (SRG名のため直接呼び出せないメソッドをリフレクション経由で呼ぶ)
    // -------------------------------------------------------------------------

    private static int frDrawStr(net.minecraft.client.gui.FontRenderer fr, String s, int x, int y, int color) {
        if (fr == null) return 0;
        if (_frDrawString != null) try {
            Object r = _frDrawString.invoke(fr, s, x, y, color);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Exception ig) {}
        try { return fr.drawString(s, x, y, color); } catch (Exception ignored) { return 0; }
    }
    private static int frDrawStrShadow(net.minecraft.client.gui.FontRenderer fr, String s, int x, int y, int color) {
        if (fr == null) return 0;
        if (_frDrawStringShadow != null) try {
            Object r = _frDrawStringShadow.invoke(fr, s, x, y, color);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Exception ig) {}
        try { return fr.drawStringWithShadow(s, x, y, color); } catch (Exception ignored) { return 0; }
    }
    private static int frGetStrWidth(net.minecraft.client.gui.FontRenderer fr, String s) {
        if (fr == null) return 0;
        if (_frGetStringWidth != null) try {
            Object r = _frGetStringWidth.invoke(fr, s);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Exception ig) {}
        try { return fr.getStringWidth(s); } catch (Exception ignored) { return 0; }
    }

    // -------------------------------------------------------------------------
    // RenderEngine / RenderItem プロキシ
    // -------------------------------------------------------------------------

    private static void reBind(net.minecraft.client.renderer.texture.RenderEngine re, String path) {
        if (re == null) return;
        if (_reBind != null) try { _reBind.invoke(re, path); return; } catch (Exception ig) {}
        try { re.bindTexture(path); } catch (Exception ignored) {}
    }

    private static void riRenderItem(
            net.minecraft.client.renderer.entity.RenderItem ri,
            net.minecraft.client.gui.FontRenderer fr,
            net.minecraft.client.renderer.texture.RenderEngine re,
            net.minecraft.item.ItemStack is, int x, int y) {
        if (ri == null || is == null) return;
        if (_riRenderItem != null) try { _riRenderItem.invoke(ri, fr, re, is, x, y); return; } catch (Exception ig) {}
        try { ri.renderItemAndEffectIntoGUI(fr, re, is, x, y); } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // GL11 直接描画による矩形描画 (GuiScreen.drawRect は SRG 名のため使用不可)
    // -------------------------------------------------------------------------

    private static void drawColoredRect(int x1, int y1, int x2, int y2, int color) {
        int alpha = (color >> 24) & 0xFF;
        int red   = (color >> 16) & 0xFF;
        int green = (color >>  8) & 0xFF;
        int blue  = (color      ) & 0xFF;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(red / 255.0f, green / 255.0f, blue / 255.0f, alpha / 255.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // -------------------------------------------------------------------------
    // サウンド / スロット位置ユーティリティ
    // -------------------------------------------------------------------------

    private void playSoundFX(String name, float vol, float pitch) {
        net.minecraft.client.audio.SoundManager sm = getSndManager();
        if (sm != null) sm.playSoundFX(name, vol, pitch);
    }

    private net.minecraft.inventory.Slot getSlotAtPositionEx(int mouseX, int mouseY) {
        for (int i = 0; i < this.container.getSlots().size(); ++i) {
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) this.container.getSlots().get(i);
            if (this.isMouseOverSlotEx(slot, mouseX, mouseY)) return slot;
        }
        return null;
    }

    private boolean isMouseOverSlotEx(net.minecraft.inventory.Slot slot, int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        return mouseX >= left + slotX(slot) - 1 && mouseX <= left + slotX(slot) + 16 &&
                mouseY >= top + slotY(slot) - 1 && mouseY <= top + slotY(slot) + 16;
    }

    private int getActualFavoriteIndex(int slotIndex) {
        int scrollRow = (int) (this.favoriteScrollOffs * Math.max(0, 10 - 9) + 0.5f);
        int actualIdx = (slotIndex - 54) + (scrollRow * 5);
        return actualIdx;
    }
}
