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

    // Slot フィールドをリフレクションで取得
    // slotNumber: Forge が追加したフィールドで MCP 名のまま残っている → 名前で直接取得
    // xDisplayPosition / yDisplayPosition: vanilla MC フィールド → SRG 名になっているため
    //   int フィールドのインデックスで取得 (2番目・3番目)
    private static java.lang.reflect.Field _slotNumber;
    private static java.lang.reflect.Field _slotX;
    private static java.lang.reflect.Field _slotY;
    // Slot メソッド (SRG名のためリフレクション)
    private static java.lang.reflect.Method _slotHasStack; // getHasStack() → boolean
    private static java.lang.reflect.Method _slotGetStack;  // getStack()    → ItemStack
    static {
        // slotNumber を名前で取得 (Forge-added フィールドは SRG 変換されない)
        try {
            _slotNumber = net.minecraft.inventory.Slot.class.getDeclaredField("slotNumber");
            _slotNumber.setAccessible(true);
        } catch (Throwable ig) { _slotNumber = null; }

        // xDisplayPosition / yDisplayPosition は SRG 名 → int フィールドの2番目・3番目
        // (0番目は slotIndex = vanilla SRG フィールド、最後が slotNumber = Forge-added)
        java.util.List<java.lang.reflect.Field> intFields = new java.util.ArrayList<>();
        for (java.lang.reflect.Field f : net.minecraft.inventory.Slot.class.getDeclaredFields()) {
            if (f.getType() == int.class) {
                try { f.setAccessible(true); } catch (Throwable ig) {}
                intFields.add(f);
            }
        }
        if (intFields.size() >= 2) _slotX = intFields.get(1);
        if (intFields.size() >= 3) _slotY = intFields.get(2);

        // Slot メソッドをシグネチャで検索 (SRG名が実行時と一致しない問題を回避)
        // クラス階層をウォークして宣言元を問わず取得
        {
            Class<?> cls = net.minecraft.inventory.Slot.class;
            while (cls != null && cls != Object.class) {
                for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                    try { m.setAccessible(true); } catch (Throwable ig) {}
                    Class<?>[] p = m.getParameterTypes();
                    Class<?> r = m.getReturnType();
                    if (p.length == 0 && r == boolean.class && _slotHasStack == null)
                        _slotHasStack = m;
                    if (p.length == 0 && r == net.minecraft.item.ItemStack.class && _slotGetStack == null)
                        _slotGetStack = m;
                }
                cls = cls.getSuperclass();
            }
            System.out.println("[TSS_PC] Slot methods: hasStack="
                    + (_slotHasStack != null ? _slotHasStack.getName() : "null")
                    + " getStack="
                    + (_slotGetStack != null ? _slotGetStack.getName() : "null"));
        }
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
    // GuiContainer の int フィールド: [xSize, ySize, guiLeft, guiTop]
    // super.initGui() が guiLeft/guiTop を xSize=176 で計算してしまうため
    // 正しい xSize/ySize を書き込んでから super を呼ぶ。さらに guiLeft/guiTop を上書きする。
    private static java.lang.reflect.Field _gcXSizeF, _gcYSizeF, _gcLeftF, _gcTopF;
    private static boolean _gcFieldsInitialized = false;
    // GuiTextField メソッド (全てSRG名のためリフレクション)
    private static java.lang.reflect.Method _gtfSetFocused, _gtfSetCanLoseFocus;
    private static java.lang.reflect.Method _gtfSetText, _gtfGetText, _gtfIsFocused;
    private static java.lang.reflect.Method _gtfDrawTextBox, _gtfMouseClicked, _gtfTextboxKeyTyped;
    // FontRenderer メソッド (SRG名のためリフレクション)
    private static java.lang.reflect.Method _frDrawString, _frDrawStringShadow, _frGetStringWidth;
    // RenderEngine.bindTexture (SRG名のためリフレクション)
    private static java.lang.reflect.Method _reBind;
    // RenderItem (Class.forName で遅延ロード — <clinit>で直接参照するとClassNotFoundになる)
    private static java.lang.reflect.Method _riRenderItem;
    private static java.lang.reflect.Field  _riZLevel;
    // RenderHelper static methods (SRG名のためリフレクション)
    private static java.lang.reflect.Method _rhEnableGUI;
    private static java.lang.reflect.Method _rhDisable;
    // NBTTagCompound メソッド (SRG名のためリフレクション)
    private static java.lang.reflect.Method _nbtHasKey, _nbtGetInteger;
    // GuiScreen.drawRect (static, SRG名のためリフレクション) — GL11直接描画の代わりに使う
    private static java.lang.reflect.Method _gsDrawRect;
    // Tessellator (SRG名のためリフレクション経由) — drawRectが見つからない場合のフォールバック
    private static Object                   _tessInstance   = null;
    private static java.lang.reflect.Method _tessStart      = null; // startDrawing(int)
    private static java.lang.reflect.Method _tessStartQuads = null; // startDrawingQuads() → void
    private static java.lang.reflect.Method _tessAddVert    = null; // addVertex(double,double,double)
    private static java.lang.reflect.Method _tessDraw       = null; // draw() → int

    // ForgeModLoaderのLoggerに直接書き込む — System.out.printlnはFMLがキャプチャしないため
    private static final java.util.logging.Logger FMLLOG =
            java.util.logging.Logger.getLogger("ForgeModLoader");
    // デバッグログ制御（ログ溢れ防止）
    private static int     _dbgDrawCount    = 0;
    private static boolean _dbgRectWarnDone = false; // drawColoredRect警告は初回のみ

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

        // drawRect は GuiScreen の親クラス Gui に定義されている。
        // getDeclaredMethods() はスーパークラスを含まないため、クラス階層をウォークして探す。
        {
            Class<?> cls = net.minecraft.client.gui.GuiScreen.class;
            outer:
            while (cls != null && cls != Object.class) {
                for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                    try { m.setAccessible(true); } catch (Throwable ig) {}
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 5
                            && java.lang.reflect.Modifier.isStatic(m.getModifiers())
                            && p[0] == int.class && p[1] == int.class && p[2] == int.class
                            && p[3] == int.class && p[4] == int.class
                            && m.getReturnType() == void.class) {
                        _gsDrawRect = m;
                        System.out.println("DEBUG: Found drawRect in " + cls.getSimpleName() + ": " + m.getName());
                        break outer;
                    }
                }
                cls = cls.getSuperclass();
            }
        }

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
        // getDeclaredMethods() はスーパークラスを含まないため、クラス階層を全てウォークする
        try {
            java.util.List<java.lang.reflect.Method> gtfBoolVoid = new java.util.ArrayList<>();
            java.util.List<java.lang.reflect.Method> gtfStrVoid  = new java.util.ArrayList<>();
            Class<?> gtfCls = net.minecraft.client.gui.GuiTextField.class;
            while (gtfCls != null && gtfCls != Object.class) {
                for (java.lang.reflect.Method m : gtfCls.getDeclaredMethods()) {
                    try { m.setAccessible(true); } catch (Throwable ig) {}
                    Class<?>[] p = m.getParameterTypes();
                    Class<?> r = m.getReturnType();
                    System.out.println("DEBUG GTF: " + m.getName() + " p=" + p.length + " r=" + r.getSimpleName());
                    if (p.length == 1 && p[0] == boolean.class && r == void.class) {
                        gtfBoolVoid.add(m);
                    } else if (p.length == 1 && p[0] == String.class && r == void.class) {
                        gtfStrVoid.add(m);
                    } else if (p.length == 0 && r == String.class && _gtfGetText == null) {
                        _gtfGetText = m;
                    } else if (p.length == 0 && r == boolean.class && _gtfIsFocused == null) {
                        _gtfIsFocused = m;
                    } else if (p.length == 0 && r == void.class && _gtfDrawTextBox == null) {
                        _gtfDrawTextBox = m;
                    } else if (p.length == 3 && p[0] == int.class && p[1] == int.class && p[2] == int.class
                            && r == void.class && _gtfMouseClicked == null) {
                        _gtfMouseClicked = m;
                    } else if (p.length == 2 && p[0] == char.class && p[1] == int.class
                            && r == boolean.class && _gtfTextboxKeyTyped == null) {
                        _gtfTextboxKeyTyped = m;
                    }
                }
                gtfCls = gtfCls.getSuperclass();
            }
            if (gtfBoolVoid.size() >= 1) _gtfSetFocused      = gtfBoolVoid.get(0);
            if (gtfBoolVoid.size() >= 2) _gtfSetCanLoseFocus = gtfBoolVoid.get(1);
            if (gtfStrVoid.size()  >= 1) _gtfSetText         = gtfStrVoid.get(0);
            System.out.println("DEBUG GTF result: clicked=" + _gtfMouseClicked
                + " draw=" + _gtfDrawTextBox + " focused=" + _gtfIsFocused
                + " getText=" + _gtfGetText + " keyTyped=" + _gtfTextboxKeyTyped);
        } catch (Throwable t) {
            System.out.println("DEBUG GTF reflection error: " + t);
        }

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

        // RenderEngine.bindTexture — .class直接参照はFMLクラスローダーでNPEになるためClass.forNameで回避
        try {
            Class<?> reClass = Class.forName("net.minecraft.client.renderer.texture.RenderEngine");
            for (java.lang.reflect.Method m : reClass.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Throwable ig) {}
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && _reBind == null) { _reBind = m; break; }
            }
        } catch (Throwable ignored) {}

        // RenderItem — 同様にClass.forNameで安全にロード、インスタンス生成とzLevelフィールドも取得
        try {
            Class<?> riClass = Class.forName("net.minecraft.client.renderer.entity.RenderItem");
            itemRenderer = riClass.newInstance();
            for (java.lang.reflect.Method m : riClass.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Throwable ig) {}
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 5 && p[2].getSimpleName().contains("ItemStack")
                        && p[3] == int.class && p[4] == int.class && _riRenderItem == null) {
                    _riRenderItem = m;
                }
            }
            for (java.lang.reflect.Field f : riClass.getDeclaredFields()) {
                try { f.setAccessible(true); } catch (Throwable ig) {}
                if (f.getType() == float.class && _riZLevel == null) { _riZLevel = f; break; }
            }
        } catch (Throwable ignored) {}

        // RenderHelper static methods (enableGUIStandardItemLighting / disableStandardItemLighting)
        try {
            Class<?> rhClass = Class.forName("net.minecraft.client.renderer.RenderHelper");
            for (java.lang.reflect.Method m : rhClass.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Throwable ig) {}
                if (m.getParameterTypes().length == 0 && m.getReturnType() == void.class
                        && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterTypes().length == 0 && m.getReturnType() == void.class) {
                    if (_rhEnableGUI == null) _rhEnableGUI = m;
                    else if (_rhDisable == null) { _rhDisable = m; break; }
                }
            }
        } catch (Throwable ignored) {}

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

        // Tessellator: drawRect が使えない場合の矩形描画フォールバック (SRG名のためリフレクション)
        try {
            Class<?> tessClass = Class.forName("net.minecraft.client.renderer.Tessellator");
            for (java.lang.reflect.Field f : tessClass.getDeclaredFields()) {
                try { f.setAccessible(true); } catch (Throwable ig) {}
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == tessClass) {
                    try { _tessInstance = f.get(null); } catch (Throwable ig) {}
                    if (_tessInstance != null) {
                        System.out.println("[TSS_PC] Tessellator instance found via field: " + f.getName());
                        break;
                    }
                }
            }
            for (java.lang.reflect.Method m : tessClass.getDeclaredMethods()) {
                try { m.setAccessible(true); } catch (Throwable ig) {}
                Class<?>[] p = m.getParameterTypes();
                Class<?> r  = m.getReturnType();
                // startDrawingQuads() — パラメータなし void
                if (p.length == 0 && r == void.class && _tessStartQuads == null)
                    _tessStartQuads = m;
                // startDrawing(int) — GL_QUADS=7 を渡す
                if (p.length == 1 && p[0] == int.class && r == void.class && _tessStart == null)
                    _tessStart = m;
                // addVertex(double, double, double)
                if (p.length == 3 && p[0] == double.class && p[1] == double.class && p[2] == double.class && _tessAddVert == null)
                    _tessAddVert = m;
                // draw() → int
                if (p.length == 0 && r == int.class && _tessDraw == null)
                    _tessDraw = m;
            }
            FMLLOG.info("[TSS_PC] Tessellator: inst=" + (_tessInstance != null)
                    + " startQuads=" + (_tessStartQuads != null)
                    + " start=" + (_tessStart != null)
                    + " addVert=" + (_tessAddVert != null)
                    + " draw=" + (_tessDraw != null));
        } catch (Throwable ig) {
            FMLLOG.warning("[TSS_PC] Tessellator reflection failed: " + ig);
        }

        FMLLOG.info("[TSS_PC] ComputerScreen static init done. drawRect=" + (_gsDrawRect != null)
                + " gsW=" + (_gsW != null) + " gsH=" + (_gsH != null) + " gsFR=" + (_gsFR != null));
    }

    private static int slotNum(net.minecraft.inventory.Slot s) {
        if (_slotNumber != null) try { return _slotNumber.getInt(s); } catch (Throwable e) {}
        try { return s.slotNumber; } catch (Throwable e) {}
        return 0;
    }
    private static boolean slotHasStack(net.minecraft.inventory.Slot s) {
        if (_slotHasStack != null) try { return (Boolean) _slotHasStack.invoke(s); } catch (Throwable ig) {}
        try { return s.func_75216_f(); } catch (Throwable ignored) { return false; }
    }
    private static net.minecraft.item.ItemStack slotGetStack(net.minecraft.inventory.Slot s) {
        if (_slotGetStack != null) try { return (net.minecraft.item.ItemStack) _slotGetStack.invoke(s); } catch (Throwable ig) {}
        try { return s.func_75211_c(); } catch (Throwable ignored) { return null; }
    }
    // スロットのX/Y座標: リフレクションが失敗した場合はインデックスから直接計算
    // s.xDisplayPosition / s.yDisplayPosition はSRG名が違うためNoSuchFieldErrorになる
    private static int slotX(net.minecraft.inventory.Slot s) {
        if (_slotX != null) try { return _slotX.getInt(s); } catch (Throwable e) {}
        return computeSlotX(slotNum(s));
    }
    private static int slotY(net.minecraft.inventory.Slot s) {
        if (_slotY != null) try { return _slotY.getInt(s); } catch (Throwable e) {}
        return computeSlotY(slotNum(s));
    }
    // ComputerContainerのスロット配置に合わせてインデックスからX/Y座標を計算
    // (Slot内部フィールドへのSRGアクセスを回避)
    private static int computeSlotX(int i) {
        if (i < 45)  return (i % 9) * 18;                 // メインストレージ
        if (i < 90)  return 184 + ((i - 45) % 5) * 18;   // お気に入り
        if (i < 117) return ((i - 90) % 9) * 18;          // プレイヤーインベントリ
        return (i - 117) * 18;                             // ホットバー
    }
    private static int computeSlotY(int i) {
        if (i < 45)  return 26  + (i / 9) * 18;
        if (i < 90)  return 26  + ((i - 45) / 5) * 18;
        if (i < 117) return 140 + ((i - 90) / 9) * 18;
        return 198;
    }

    // 1.5.2用のテクスチャ指定
    private static final String texturePath = "/mods/tss_pc/textures/gui/computer.png";
    private EntityPlayer thePlayer;
    private ComputerContainer container; // field_73875_a の SRG 名が違うため自クラスで保持
    private ComputerBlockEntity tileEntity;
    private GuiTextField searchBox;
    private GuiTextField tabNameField;

    // lazyInit済みフラグ（GUI開くたびにリセット）
    private boolean _guiInitDone = false;

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
    // RenderItem は Class.forName で static ブロック内で初期化 (直接型参照でクラスロード失敗を防ぐ)
    private static Object itemRenderer = null;

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

    private static java.lang.reflect.Field gcTryName(String name) {
        try {
            java.lang.reflect.Field f = net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable ig) { return null; }
    }
    private static void ensureGCFields() {
        if (_gcFieldsInitialized) return;
        _gcFieldsInitialized = true;
        // MCP名で試みる (Forge がMCP名を保持する場合に確実)
        _gcXSizeF = gcTryName("xSize");
        _gcYSizeF = gcTryName("ySize");
        _gcLeftF  = gcTryName("guiLeft");
        _gcTopF   = gcTryName("guiTop");
        // 全 int フィールドをデバッグログに出力 (インデックスフォールバックは廃止)
        StringBuilder sb = new StringBuilder("[TSS_PC] GC int fields:");
        for (java.lang.reflect.Field f : net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredFields()) {
            if (f.getType() == int.class) { try { f.setAccessible(true); } catch (Throwable ig) {} sb.append(" ").append(f.getName()); }
        }
        FMLLOG.warning(sb.toString());
        FMLLOG.warning("[TSS_PC] GC by name: xSize=" + (_gcXSizeF != null ? "OK" : "MISS")
                + " ySize=" + (_gcYSizeF != null ? "OK" : "MISS")
                + " guiLeft=" + (_gcLeftF != null ? "OK" : "MISS")
                + " guiTop=" + (_gcTopF != null ? "OK" : "MISS"));
    }
    /**
     * GuiContainer の xSize/ySize フィールドを super.initGui() の前に正しい値に設定する。
     * super が guiLeft = (w - xSize) / 2 を計算するとき、xSize=300 になっているので
     * guiLeft = (w-300)/2 が自動的に正しく設定される。
     */
    private void setGCSizesBeforeSuper() {
        ensureGCFields();
        // MCP名で見つからなければ初期値(176,166)でスキャン
        if (_gcXSizeF == null || _gcYSizeF == null) {
            for (java.lang.reflect.Field f : net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                try {
                    f.setAccessible(true);
                    int v = f.getInt(this);
                    if (_gcXSizeF == null && v == 176) { _gcXSizeF = f; }
                    else if (_gcYSizeF == null && v == 166) { _gcYSizeF = f; }
                } catch (Throwable ig) {}
            }
        }
        try { if (_gcXSizeF != null) _gcXSizeF.setInt(this, this.xSize); } catch (Throwable ig) {}
        try { if (_gcYSizeF != null) _gcYSizeF.setInt(this, this.ySize); } catch (Throwable ig) {}
        FMLLOG.warning("[TSS_PC] setGCSizes: xSize→" + this.xSize + " ySize→" + this.ySize
                + " xF=" + (_gcXSizeF != null ? _gcXSizeF.getName() : "MISS")
                + " yF=" + (_gcYSizeF != null ? _gcYSizeF.getName() : "MISS"));
    }

    /**
     * super.initGui() 実行後に guiLeft/guiTop を強制修正する。
     * super は xSize=176 で計算するため guiLeft=(w-176)/2 になる。
     * これを (w-300)/2 に修正する。setGCSizesBeforeSuper が成功した場合は
     * 既に (w-300)/2 になっているため、両方の値を検索してどちらでも対応する。
     */
    private void fixAndCacheGuiLeftTop() {
        syncGuiFields();
        int wrongLeft   = (this.width  - 176) / 2;   // super が設定する誤値
        int wrongTop    = (this.height - 166) / 2;
        int correctLeft = (this.width  - this.xSize) / 2;   // (w-300)/2 が正しい値
        int correctTop  = (this.height - this.ySize) / 2;
        if (_gcLeftF == null || _gcTopF == null) {
            for (java.lang.reflect.Field f : net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                try {
                    f.setAccessible(true);
                    int v = f.getInt(this);
                    // 誤値でも正値でもどちらでも guiLeft/guiTop フィールドとして認識する
                    if (_gcLeftF == null && (v == wrongLeft || v == correctLeft)) { _gcLeftF = f; }
                    else if (_gcTopF == null && (v == wrongTop || v == correctTop)) { _gcTopF = f; }
                } catch (Throwable ig) {}
            }
        }
        try { if (_gcLeftF != null) _gcLeftF.setInt(this, correctLeft); } catch (Throwable ig) {}
        try { if (_gcTopF  != null) _gcTopF.setInt(this, correctTop);  } catch (Throwable ig) {}
        FMLLOG.warning("[TSS_PC] fixGuiLeftTop: w=" + this.width + " h=" + this.height
                + " wrong=" + wrongLeft + "/" + wrongTop
                + " correct=" + correctLeft + "/" + correctTop
                + " lF=" + (_gcLeftF != null ? _gcLeftF.getName() : "MISS")
                + " tF=" + (_gcTopF  != null ? _gcTopF.getName()  : "MISS"));
    }

    private void lazyInit() {
        syncGuiFields();
        try {
            int left = (this.width - this.xSize) / 2;
            int top  = (this.height - this.ySize) / 2;
            this.searchBox   = new GuiTextField(this.fontRenderer, left + 66,  top + 10, 100, 12);
            gtfSetFocused(this.searchBox, false);
            gtfSetCanLoseFocus(this.searchBox, true);
            this.tabNameField = new GuiTextField(this.fontRenderer, left + 192, top + 10,  80, 12);
            List<ComputerBlockEntity.FavoriteTab> tabs = this.tileEntity.getTabsForPlayer(this.thePlayer);
            if (this.selectedTabIndex < tabs.size()) {
                gtfSetText(this.tabNameField, tabs.get(this.selectedTabIndex).name);
            }
            _guiInitDone = true;
            FMLLOG.warning("[TSS_PC] lazyInit done: w=" + this.width + " h=" + this.height
                    + " fontRenderer=" + (this.fontRenderer != null));
        } catch (Throwable ig) {
            FMLLOG.warning("[TSS_PC] lazyInit failed: " + ig.getClass().getSimpleName() + ": " + ig.getMessage());
        }
    }

    @Override
    public void func_73866_w() {
        _dbgDrawCount = 0;
        _dbgRectWarnDone = false;
        _guiInitDone = false;
        this.searchBox   = null;
        this.tabNameField = null;
        // super.initGui() が xSize=176 で guiLeft=(w-176)/2 を計算するため、
        // 事前に GuiContainer.xSize=300 に書き換えて (w-300)/2 が計算されるようにする
        setGCSizesBeforeSuper();
        try { super.func_73866_w(); } catch (Throwable ig) {
            FMLLOG.warning("[TSS_PC] super.func_73866_w() threw: " + ig.getClass().getSimpleName() + ": " + ig.getMessage());
        }
        fixAndCacheGuiLeftTop();
        lazyInit();
    }

    @Override
    protected void func_74185_a(float partialTicks, int mouseX, int mouseY) {
        if (this.searchBox == null) {
            lazyInit();
            if (this.searchBox == null) return; // lazyInitも失敗した場合のみスキップ
        }
        syncGuiFields(); // 毎フレーム確実に同期
        if (_dbgDrawCount < 3) {
            _dbgDrawCount++;
            FMLLOG.info("[TSS_PC] drawBackground[" + _dbgDrawCount + "]: w=" + this.width + " h=" + this.height
                    + " drawRect=" + (_gsDrawRect != null)
                    + " tessInst=" + (_tessInstance != null)
                    + " tessQuads=" + (_tessStartQuads != null)
                    + " tessStart=" + (_tessStart != null));
        }
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.tabX = left + this.xSize;
        ComputerContainer container = this.container;

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // メイン背景
        drawColoredRect(left, top, left + this.xSize, top + this.ySize, 0xFF1E1E1E);

        // 仕切り線: メインストレージ / お気に入り の間
        drawColoredRect(left + 186, top, left + 187, top + this.ySize, 0xFF3E3E42);
        // Inventory ラベル上の区切り線
        drawColoredRect(left, top + 132, left + 186, top + 133, 0xFF3E3E42);

        // スロット枠線ループ — スロットX/Y はリフレクションに依存せず直接計算
        int totalSlots = this.container.getSlots().size();
        for (int s = 0; s < totalSlots; s++) {
            int sx = computeSlotX(s);
            int sy = computeSlotY(s);
            int rx = left + sx - 1;
            int ry = top + sy - 1;
            drawColoredRect(rx, ry, rx + 18, ry + 18, 0xFF3E3E42);
            drawColoredRect(rx + 1, ry + 1, rx + 17, ry + 17, 0xFF252526);
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
        Object reForTabs = getRenderEngine();

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
                rhEnableGUI();
                riRenderItem(itemRenderer, this.fontRenderer, reForTabs, currentTab.icon, iconX, iconY);
                rhDisable();
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

        // super.drawScreen() は毎フレーム guiLeft = (width - GuiContainer.xSize) / 2 を再計算する。
        // GuiContainer.xSize を 300 に設定してから super を呼ぶことで
        // guiLeft = (w-300)/2 が正しく計算される。guiLeft の直接書き換えは super に上書きされるため不要。
        ensureGCFields();
        try { if (_gcXSizeF != null) _gcXSizeF.setInt(this, this.xSize); } catch (Throwable ig) {}
        try { if (_gcYSizeF != null) _gcYSizeF.setInt(this, this.ySize); } catch (Throwable ig) {}

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
            rhEnableGUI();
            riSetZLevel(200.0F);
            riRenderItem(itemRenderer, this.fontRenderer, getRenderEngine(), this.draggingStack, mouseX - 8, mouseY - 8);
            riSetZLevel(0.0F);
            rhDisable();
            GL11.glPopMatrix();
        }
    }

    @Override
    protected void func_74184_a(int mouseX, int mouseY) {
        // このメソッドは GuiContainer が GL translate(guiLeft, guiTop) した後に呼ばれる
        // → 座標はスロット座標系 (xDisplayPosition 基準) と同じ相対座標
        frDrawStr(this.fontRenderer, "TSS_PC",   8,   5, 0xAAAAAA);
        frDrawStr(this.fontRenderer, "Search:",  8,  14, 0x888888);
        frDrawStr(this.fontRenderer, "Inventory", 8, 133, 0xAAAAAA);
        // お気に入りエリア下部のボタン (xSize=300, お気に入りエリアは x=192-282)
        frDrawStr(this.fontRenderer, "[+Row]", 192, 193, 0xAAAAAA);
        frDrawStr(this.fontRenderer, "[-Row]", 228, 193, 0xAAAAAA);
        // タブパネル内のボタン (guiLeft 相対 xSize+数px)
        frDrawStr(this.fontRenderer, "[+]", this.xSize + 4, 194, 0x00FF00);
        frDrawStr(this.fontRenderer, "[-]", this.xSize + 4, 206, 0xFF0000);

        for (int i = 0; i < 90; i++) {
            Slot slot = (Slot) this.container.getSlots().get(i);
            if (slot != null && slotHasStack(slot)) {
                ItemStack stackInSlot = slotGetStack(slot);

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
                    float x = (computeSlotX(i) + 16 - 1) / scale - textWidth;
                    float y = (computeSlotY(i) + 16 - (7 * scale)) / scale;

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
                if (MCHelper.invGetItemStack(this.thePlayer.field_71071_by) == null && slotHasStack(slot)) {
                    if (button == 0) {
                        this.isDraggingItem = true;
                        this.draggingStack = MCHelper.itemCopy(slotGetStack(slot));
                        this.pressedSlotIndex = id;
                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slotGetStack(slot), amount);
                        playSoundFX("random.click", 0.6F, 1.2F);
                        return;
                    }
                }
            } else if (id >= 45 && id < 90) {
                if (MCHelper.invGetItemStack(this.thePlayer.field_71071_by) == null && slotHasStack(slot)) {
                    if (button == 0 || button == 1) {
                        boolean isShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        int amount = isShift ? 64 : 1;
                        this.sendComputerPacket(2, 0, id, slotGetStack(slot), amount);
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
                net.minecraft.item.ItemStack heldItem = MCHelper.invGetItemStack(this.thePlayer.field_71071_by);
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
                if (MCHelper.invGetItemStack(this.thePlayer.field_71071_by) != null) {
                    sendActionPacket(6, -999);
                }
            }
        }

        if (slotId < 0) {
            super.handleMouseClick(slot, slotId, mouseButton, mode);
            return;
        }

        if (slotId >= 0 && slotId < 45) {
            net.minecraft.item.ItemStack heldStack = MCHelper.invGetItemStack(this.thePlayer.field_71071_by);
            if (heldStack != null) {
                this.sendComputerPacket(6, 0, slotId, heldStack, 0);
                return;
            }
            if (slot != null && slotHasStack(slot)) {
                int amount = (mode == 1) ? 64 : 1;
                this.sendComputerPacket(2, 0, slotId, slotGetStack(slot), amount);
                return;
            }
            return;
        }

        if (slotId >= 45 && slotId < 90) {
            this.sendComputerPacket(1, 5, slotId - 45, MCHelper.invGetItemStack(this.thePlayer.field_71071_by), 0);
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
        try { if (_gsW  != null) this.width  = _gsW.getInt(this); } catch (Throwable ignored) {}
        try { if (_gsH  != null) this.height = _gsH.getInt(this); } catch (Throwable ignored) {}
        // width/heightが0ならScaledResolutionかDisplayから取得する
        if (this.width <= 0 || this.height <= 0) {
            try {
                // ScaledResolution を使う (MCP名でgetScaledWidth/Heightを反射検索)
                Class<?> srClass = Class.forName("net.minecraft.client.gui.ScaledResolution");
                Object sr = null;
                for (java.lang.reflect.Constructor<?> c : srClass.getConstructors()) {
                    Class<?>[] cp = c.getParameterTypes();
                    if (cp.length == 3) { sr = c.newInstance(mcInst, org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight()); break; }
                    if (cp.length == 1) { sr = c.newInstance(mcInst); break; }
                }
                if (sr != null) {
                    for (java.lang.reflect.Method m : srClass.getMethods()) {
                        Class<?>[] p = m.getParameterTypes();
                        Class<?> r  = m.getReturnType();
                        if (p.length == 0 && r == int.class) {
                            int v = (Integer) m.invoke(sr);
                            if (this.width  <= 0 && v > 0 && v < 10000) { this.width  = v; }
                            else if (this.height <= 0 && v > 0 && v < 10000) { this.height = v; }
                        }
                    }
                }
            } catch (Throwable ig) {}
            // 最終フォールバック: LWJGL Display ピクセルサイズ (スケール非考慮)
            if (this.width  <= 0) try { this.width  = org.lwjgl.opengl.Display.getWidth();  } catch (Throwable ig) {}
            if (this.height <= 0) try { this.height = org.lwjgl.opengl.Display.getHeight(); } catch (Throwable ig) {}
        }
        try { if (_gsFR != null) this.fontRenderer = (net.minecraft.client.gui.FontRenderer) _gsFR.get(this); } catch (Throwable ignored) {}
    }

    private Object getRenderEngine() {
        if (_mcRE != null && this.mc != null) try {
            return _mcRE.get(this.mc);
        } catch (Throwable ignored) {}
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
        if (_nbtHasKey != null) try { return (Boolean) _nbtHasKey.invoke(tag, key); } catch (Throwable ig) {}
        try { return tag.hasKey(key); } catch (Throwable ignored) { return false; }
    }
    private static int nbtGetInt(net.minecraft.nbt.NBTTagCompound tag, String key) {
        if (tag == null) return 0;
        if (_nbtGetInteger != null) try { return (Integer) _nbtGetInteger.invoke(tag, key); } catch (Throwable ig) {}
        try { return tag.getInteger(key); } catch (Throwable ignored) { return 0; }
    }

    // -------------------------------------------------------------------------
    // GuiTextField プロキシ
    // フォールバックは catch(Throwable) で保護 — NoSuchMethodError は Error なので
    // catch(Exception) では捕捉できずクラッシュするため必ず Throwable を使う
    // -------------------------------------------------------------------------

    private static void gtfSetFocused(GuiTextField f, boolean v) {
        if (_gtfSetFocused != null) try { _gtfSetFocused.invoke(f, v); return; } catch (Throwable ig) {}
        try { f.setFocused(v); } catch (Throwable ignored) {}
    }
    private static void gtfSetCanLoseFocus(GuiTextField f, boolean v) {
        if (_gtfSetCanLoseFocus != null) try { _gtfSetCanLoseFocus.invoke(f, v); return; } catch (Throwable ig) {}
        try { f.setCanLoseFocus(v); } catch (Throwable ignored) {}
    }
    private static void gtfSetText(GuiTextField f, String s) {
        if (_gtfSetText != null) try { _gtfSetText.invoke(f, s); return; } catch (Throwable ig) {}
        try { f.setText(s); } catch (Throwable ignored) {}
    }
    private static String gtfGetText(GuiTextField f) {
        if (_gtfGetText != null) try { return (String) _gtfGetText.invoke(f); } catch (Throwable ig) {}
        try { return f.getText(); } catch (Throwable ignored) { return ""; }
    }
    private static boolean gtfIsFocused(GuiTextField f) {
        if (_gtfIsFocused != null) try { return (Boolean) _gtfIsFocused.invoke(f); } catch (Throwable ig) {}
        try { return f.isFocused(); } catch (Throwable ignored) { return false; }
    }
    private static void gtfDrawTextBox(GuiTextField f) {
        if (_gtfDrawTextBox != null) try { _gtfDrawTextBox.invoke(f); return; } catch (Throwable ig) {}
        try { f.drawTextBox(); } catch (Throwable ignored) {}
    }
    private static void gtfMouseClicked(GuiTextField f, int x, int y, int btn) {
        if (_gtfMouseClicked != null) try { _gtfMouseClicked.invoke(f, x, y, btn); return; } catch (Throwable ig) {}
        try { f.mouseClicked(x, y, btn); } catch (Throwable ignored) {}
    }
    private static boolean gtfTextboxKeyTyped(GuiTextField f, char c, int key) {
        if (_gtfTextboxKeyTyped != null) try { return (Boolean) _gtfTextboxKeyTyped.invoke(f, c, key); } catch (Throwable ig) {}
        try { return f.textboxKeyTyped(c, key); } catch (Throwable ignored) { return false; }
    }

    // -------------------------------------------------------------------------
    // FontRenderer プロキシ (catch Throwable で Error も捕捉)
    // -------------------------------------------------------------------------

    private static int frDrawStr(net.minecraft.client.gui.FontRenderer fr, String s, int x, int y, int color) {
        if (fr == null) return 0;
        if (_frDrawString != null) try {
            Object r = _frDrawString.invoke(fr, s, x, y, color);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Throwable ig) {}
        try { return fr.drawString(s, x, y, color); } catch (Throwable ignored) { return 0; }
    }
    private static int frDrawStrShadow(net.minecraft.client.gui.FontRenderer fr, String s, int x, int y, int color) {
        if (fr == null) return 0;
        if (_frDrawStringShadow != null) try {
            Object r = _frDrawStringShadow.invoke(fr, s, x, y, color);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Throwable ig) {}
        try { return fr.drawStringWithShadow(s, x, y, color); } catch (Throwable ignored) { return 0; }
    }
    private static int frGetStrWidth(net.minecraft.client.gui.FontRenderer fr, String s) {
        if (fr == null) return 0;
        if (_frGetStringWidth != null) try {
            Object r = _frGetStringWidth.invoke(fr, s);
            return r instanceof Integer ? (Integer) r : 0;
        } catch (Throwable ig) {}
        try { return fr.getStringWidth(s); } catch (Throwable ignored) { return 0; }
    }

    // -------------------------------------------------------------------------
    // RenderEngine / RenderItem プロキシ
    // -------------------------------------------------------------------------

    private static void reBind(Object re, String path) {
        if (re == null) return;
        if (_reBind != null) try { _reBind.invoke(re, path); return; } catch (Throwable ig) {}
    }

    private static void riRenderItem(Object ri, Object fr, Object re, net.minecraft.item.ItemStack is, int x, int y) {
        if (ri == null || is == null) return;
        if (_riRenderItem != null) try { _riRenderItem.invoke(ri, fr, re, is, x, y); return; } catch (Throwable ig) {}
    }

    private static void riSetZLevel(float z) {
        if (_riZLevel != null && itemRenderer != null)
            try { _riZLevel.setFloat(itemRenderer, z); } catch (Throwable ig) {}
    }

    private static void rhEnableGUI() {
        if (_rhEnableGUI != null) try { _rhEnableGUI.invoke(null); return; } catch (Throwable ig) {}
        try { net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting(); } catch (Throwable ig) {}
    }

    private static void rhDisable() {
        if (_rhDisable != null) try { _rhDisable.invoke(null); return; } catch (Throwable ig) {}
        try { net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting(); } catch (Throwable ig) {}
    }

    // -------------------------------------------------------------------------
    // 矩形描画 — MinecraftのdrawRect(Tessellatorベース)をリフレクション経由で呼ぶ
    // GL11直接描画(glBegin/glEnd)はMinecraftのレンダリングコンテキストで
    // 正しく動作しないためフォールバック専用とする
    // -------------------------------------------------------------------------

    private static void drawColoredRect(int x1, int y1, int x2, int y2, int color) {
        // 第一優先: Gui.drawRect (Tessellatorベース、SRG名をリフレクションで取得)
        if (_gsDrawRect != null) {
            try { _gsDrawRect.invoke(null, x1, y1, x2, y2, color); return; } catch (Throwable ig) {
                if (!_dbgRectWarnDone) {
                    _dbgRectWarnDone = true;
                    FMLLOG.warning("[TSS_PC] Gui.drawRect invoke failed: " + ig.getClass().getSimpleName() + ": " + ig.getMessage());
                }
            }
        }
        // 第二優先: Tessellator経由 (SRG名のためリフレクションで取得)
        float a = (float)((color >> 24) & 0xFF) / 255.0F;
        float r = (float)((color >> 16) & 0xFF) / 255.0F;
        float g = (float)((color >>  8) & 0xFF) / 255.0F;
        float b = (float)((color      ) & 0xFF) / 255.0F;
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r, g, b, a);
            if (_tessInstance != null && _tessAddVert != null && _tessDraw != null) {
                // startDrawingQuads() を優先、なければ startDrawing(7)
                if (_tessStartQuads != null) {
                    _tessStartQuads.invoke(_tessInstance);
                } else if (_tessStart != null) {
                    _tessStart.invoke(_tessInstance, 7); // GL_QUADS = 7
                } else {
                    if (!_dbgRectWarnDone) { _dbgRectWarnDone = true; FMLLOG.warning("[TSS_PC] Tessellator: no start method found, falling back to GL11"); }
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(x1, y2); GL11.glVertex2f(x2, y2);
                    GL11.glVertex2f(x2, y1); GL11.glVertex2f(x1, y1);
                    GL11.glEnd();
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    return;
                }
                _tessAddVert.invoke(_tessInstance, (double)x2, (double)y2, 0.0D);
                _tessAddVert.invoke(_tessInstance, (double)x1, (double)y2, 0.0D);
                _tessAddVert.invoke(_tessInstance, (double)x1, (double)y1, 0.0D);
                _tessAddVert.invoke(_tessInstance, (double)x2, (double)y1, 0.0D);
                _tessDraw.invoke(_tessInstance);
            } else {
                if (!_dbgRectWarnDone) { _dbgRectWarnDone = true; FMLLOG.warning("[TSS_PC] Tessellator instance or methods null, using GL11 fallback"); }
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(x1, y2); GL11.glVertex2f(x2, y2);
                GL11.glVertex2f(x2, y1); GL11.glVertex2f(x1, y1);
                GL11.glEnd();
            }
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Throwable ig) {
            if (!_dbgRectWarnDone) { _dbgRectWarnDone = true;
                FMLLOG.warning("[TSS_PC] drawColoredRect Tessellator failed: " + ig.getClass().getSimpleName() + ": " + ig.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // サウンド / スロット位置ユーティリティ
    // -------------------------------------------------------------------------

    private static java.lang.reflect.Method _smPlayFX;
    private static boolean _smPlayFXInit = false;
    private void playSoundFX(String name, float vol, float pitch) {
        net.minecraft.client.audio.SoundManager sm = getSndManager();
        if (sm == null) return;
        if (!_smPlayFXInit) {
            _smPlayFXInit = true;
            for (java.lang.reflect.Method m : sm.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == String.class && p[1] == float.class && p[2] == float.class) {
                    try { m.setAccessible(true); } catch (Throwable ig) {}
                    _smPlayFX = m; break;
                }
            }
        }
        if (_smPlayFX != null) try { _smPlayFX.invoke(sm, name, vol, pitch); } catch (Throwable ig) {}
    }

    private net.minecraft.inventory.Slot getSlotAtPositionEx(int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top  = (this.height - this.ySize) / 2;
        for (int i = 0; i < this.container.getSlots().size(); ++i) {
            int sx = computeSlotX(i);
            int sy = computeSlotY(i);
            if (mouseX >= left + sx && mouseX < left + sx + 16 &&
                mouseY >= top  + sy && mouseY < top  + sy + 16) {
                return (net.minecraft.inventory.Slot) this.container.getSlots().get(i);
            }
        }
        return null;
    }

    private boolean isMouseOverSlotEx(net.minecraft.inventory.Slot slot, int mouseX, int mouseY) {
        // 後方互換のため残す（getSlotAtPositionEx から直接は呼ばれなくなったが他から呼ばれる可能性）
        int left = (this.width - this.xSize) / 2;
        int top  = (this.height - this.ySize) / 2;
        int idx  = slotNum(slot);
        int sx   = computeSlotX(idx);
        int sy   = computeSlotY(idx);
        return mouseX >= left + sx && mouseX < left + sx + 16 &&
               mouseY >= top  + sy && mouseY < top  + sy + 16;
    }

    private int getActualFavoriteIndex(int slotIndex) {
        int scrollRow = (int) (this.favoriteScrollOffs * Math.max(0, 10 - 9) + 0.5f);
        int actualIdx = (slotIndex - 54) + (scrollRow * 5);
        return actualIdx;
    }
}
