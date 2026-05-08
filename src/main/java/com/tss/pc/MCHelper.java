package com.tss.pc;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Forge 1.5.2 では vanilla メソッドが SRG 名のため、リフレクションで本名を検索してキャッシュする。
 */
public final class MCHelper {

    // === NBTTagCompound ===
    private static Method nbtSetTag;         // setTag(String, NBTBase) → void
    private static Method nbtGetTagList;     // getTagList(String) → NBTTagList
    private static Method nbtGetCompound;    // getCompoundTag(String) → NBTTagCompound
    private static Method nbtRemoveTag;      // removeTag(String) → void

    // === NBTBase / NBTTagCompound copy ===
    private static Method nbtCompoundCopy;   // NBTTagCompound no-arg → NBTBase or subtype

    // === NBTTagList ===
    private static Method nbtAppendTag;      // appendTag(NBTBase) → void
    private static Method nbtTagAt;          // tagAt(int) → NBTBase
    private static Method nbtTagCount;       // tagCount() → int

    // === ItemStack ===
    private static Method itemCopy;          // copy() → ItemStack
    private static Method itemWriteToNBT;    // writeToNBT(NBTTagCompound) → any
    private static Method itemLoadFromNBT;   // static loadItemStackFromNBT(NBTTagCompound) → ItemStack
    private static Method itemGetDisplayName;// getDisplayName() → String

    static {
        Class<?> cCompound = NBTTagCompound.class;
        Class<?> cList     = NBTTagList.class;
        Class<?> cBase     = NBTBase.class;
        Class<?> cStack    = ItemStack.class;

        // --- NBTTagCompound.setTag(String, NBTBase) → void ---
        try {
            for (Method m : cCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class && p[1] == cBase && m.getReturnType() == void.class) {
                    nbtSetTag = m; System.out.println("DEBUG: [TSSPC] MCH setTag=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH setTag err: " + e); }

        // --- NBTTagCompound.getTagList(String) → NBTTagList ---
        try {
            for (Method m : cCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && m.getReturnType() == cList) {
                    nbtGetTagList = m; System.out.println("DEBUG: [TSSPC] MCH getTagList=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH getTagList err: " + e); }

        // --- NBTTagCompound.getCompoundTag(String) → NBTTagCompound ---
        try {
            for (Method m : cCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class && m.getReturnType() == cCompound) {
                    nbtGetCompound = m; System.out.println("DEBUG: [TSSPC] MCH getCompound=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH getCompound err: " + e); }

        // --- NBTTagCompound.removeTag(String) → void ---
        // 既知 MCP 名を試し、なければシグネチャで探す
        try {
            String[] names = {"removeTag", "func_74734_a"};
            outer:
            for (String n : names) {
                try {
                    Method m = cCompound.getMethod(n, String.class);
                    if (m.getReturnType() == void.class) { nbtRemoveTag = m; break outer; }
                } catch (NoSuchMethodException ignored) {}
            }
            if (nbtRemoveTag == null) {
                for (Method m : cCompound.getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    // void, 1 String param, not setString/setBoolean etc. (check not in setter list)
                    if (p.length == 1 && p[0] == String.class && m.getReturnType() == void.class
                            && !m.getName().equals("setString") && !m.getName().startsWith("set")) {
                        nbtRemoveTag = m; break;
                    }
                }
            }
            if (nbtRemoveTag != null) System.out.println("DEBUG: [TSSPC] MCH removeTag=" + nbtRemoveTag.getName());
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH removeTag err: " + e); }

        // --- NBTTagCompound.copy() → NBTBase subtype (no-arg) ---
        try {
            for (Method m : cCompound.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 && cBase.isAssignableFrom(m.getReturnType())
                        && !m.getDeclaringClass().equals(Object.class)) {
                    nbtCompoundCopy = m; System.out.println("DEBUG: [TSSPC] MCH nbtCopy=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH nbtCopy err: " + e); }

        // --- NBTTagList.appendTag(NBTBase) → void ---
        try {
            for (Method m : cList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == cBase && m.getReturnType() == void.class) {
                    nbtAppendTag = m; System.out.println("DEBUG: [TSSPC] MCH appendTag=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH appendTag err: " + e); }

        // --- NBTTagList.tagAt(int) → NBTBase ---
        try {
            for (Method m : cList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == int.class && cBase.isAssignableFrom(m.getReturnType())) {
                    nbtTagAt = m; System.out.println("DEBUG: [TSSPC] MCH tagAt=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH tagAt err: " + e); }

        // --- NBTTagList.tagCount() → int ---
        try {
            for (Method m : cList.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 && m.getReturnType() == int.class
                        && !m.getName().equals("hashCode")) {
                    nbtTagCount = m; System.out.println("DEBUG: [TSSPC] MCH tagCount=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH tagCount err: " + e); }

        // --- ItemStack.copy() → ItemStack ---
        try {
            for (Method m : cStack.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 && m.getReturnType() == cStack) {
                    itemCopy = m; System.out.println("DEBUG: [TSSPC] MCH itemCopy=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH itemCopy err: " + e); }

        // --- ItemStack.writeToNBT(NBTTagCompound) → any ---
        try {
            for (Method m : cStack.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == cCompound && !Modifier.isStatic(m.getModifiers())) {
                    itemWriteToNBT = m; System.out.println("DEBUG: [TSSPC] MCH writeToNBT=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH writeToNBT err: " + e); }

        // --- static ItemStack.loadItemStackFromNBT(NBTTagCompound) → ItemStack ---
        try {
            for (Method m : cStack.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == cCompound && m.getReturnType() == cStack
                        && Modifier.isStatic(m.getModifiers())) {
                    itemLoadFromNBT = m; System.out.println("DEBUG: [TSSPC] MCH loadFromNBT=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH loadFromNBT err: " + e); }

        // --- ItemStack.getDisplayName() → String ---
        try {
            for (Method m : cStack.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 0 && m.getReturnType() == String.class
                        && !m.getName().equals("toString")
                        && !m.getDeclaringClass().equals(Object.class)) {
                    itemGetDisplayName = m; System.out.println("DEBUG: [TSSPC] MCH displayName=" + m.getName()); break;
                }
            }
        } catch (Exception e) { System.out.println("DEBUG: [TSSPC] MCH displayName err: " + e); }
    }

    // ==================== public API ====================

    public static void nbtSetTag(NBTTagCompound nbt, String key, NBTBase val) {
        if (nbtSetTag != null) try { nbtSetTag.invoke(nbt, key, val); return; }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtSetTag invoke err: " + e); }
    }

    public static NBTTagList nbtGetTagList(NBTTagCompound nbt, String key) {
        if (nbtGetTagList != null) try { return (NBTTagList) nbtGetTagList.invoke(nbt, key); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtGetTagList invoke err: " + e); }
        return new NBTTagList();
    }

    public static NBTTagCompound nbtGetCompoundTag(NBTTagCompound nbt, String key) {
        if (nbtGetCompound != null) try { return (NBTTagCompound) nbtGetCompound.invoke(nbt, key); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtGetCompound invoke err: " + e); }
        return new NBTTagCompound();
    }

    public static void nbtRemoveTag(NBTTagCompound nbt, String key) {
        if (nbtRemoveTag != null) try { nbtRemoveTag.invoke(nbt, key); return; }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtRemoveTag invoke err: " + e); }
    }

    /** NBTTagCompound をコピーして返す。失敗時は null を返す。 */
    public static NBTTagCompound nbtCopy(NBTTagCompound nbt) {
        if (nbt == null) return null;
        if (nbtCompoundCopy != null) try { return (NBTTagCompound) nbtCompoundCopy.invoke(nbt); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtCopy invoke err: " + e); }
        return null;
    }

    public static void nbtAppendTag(NBTTagList list, NBTBase val) {
        if (nbtAppendTag != null) try { nbtAppendTag.invoke(list, val); return; }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtAppendTag invoke err: " + e); }
    }

    public static NBTBase nbtTagAt(NBTTagList list, int i) {
        if (nbtTagAt != null) try { return (NBTBase) nbtTagAt.invoke(list, i); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtTagAt invoke err: " + e); }
        return null;
    }

    public static int nbtTagCount(NBTTagList list) {
        if (nbtTagCount != null) try { return (Integer) nbtTagCount.invoke(list); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] nbtTagCount invoke err: " + e); }
        return 0;
    }

    public static ItemStack itemCopy(ItemStack stack) {
        if (stack == null) return null;
        if (itemCopy != null) try { return (ItemStack) itemCopy.invoke(stack); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] itemCopy invoke err: " + e); }
        // フォールバック: フィールド直接コピー
        ItemStack s = new ItemStack(stack.itemID, stack.stackSize, stack.itemDamage);
        s.stackTagCompound = stack.stackTagCompound;
        return s;
    }

    public static void itemWriteToNBT(ItemStack stack, NBTTagCompound nbt) {
        if (stack == null || nbt == null) return;
        if (itemWriteToNBT != null) try { itemWriteToNBT.invoke(stack, nbt); return; }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] itemWriteToNBT invoke err: " + e); }
    }

    public static ItemStack itemLoadFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        if (itemLoadFromNBT != null) try { return (ItemStack) itemLoadFromNBT.invoke(null, nbt); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] itemLoadFromNBT invoke err: " + e); }
        return null;
    }

    public static String itemGetDisplayName(ItemStack stack) {
        if (stack == null) return "";
        if (itemGetDisplayName != null) try { return (String) itemGetDisplayName.invoke(stack); }
        catch (Exception e) { System.out.println("DEBUG: [TSSPC] itemGetDisplayName invoke err: " + e); }
        return String.valueOf(stack.itemID);
    }

    private MCHelper() {}
}
