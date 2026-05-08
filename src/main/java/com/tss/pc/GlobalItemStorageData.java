package com.tss.pc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GlobalItemStorageData extends WorldSavedData {
    private static final String DATA_NAME = "tss_pc_storage";
    private final BulkItemStorage storage = new BulkItemStorage();

    // MapStorageのloadData/setDataはSRG名のためリフレクションで検索する
    private static Method mapStorageLoadMethod;
    private static Method mapStorageSetMethod;

    // WorldSavedDataのmarkDirty相当メソッド（SRG名）をリフレクションで検索する
    private static Method worldSavedDataMarkDirtyMethod;
    // フォールバック: dirty フィールドを直接操作する
    private static Field worldSavedDataDirtyField;

    static {
        // WorldSavedData の no-arg void メソッド = markDirty相当を探す
        try {
            for (Method m : WorldSavedData.class.getDeclaredMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    worldSavedDataMarkDirtyMethod = m;
                    System.out.println("DEBUG: [TSSPC] WorldSavedData.markDirty = " + m.getName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: [TSSPC] WorldSavedData.markDirty search error: " + e);
        }

        // フォールバック: boolean 型フィールド (dirty) を直接探す
        if (worldSavedDataMarkDirtyMethod == null) {
            try {
                for (Field f : WorldSavedData.class.getDeclaredFields()) {
                    if (f.getType() == boolean.class) {
                        f.setAccessible(true);
                        worldSavedDataDirtyField = f;
                        System.out.println("DEBUG: [TSSPC] WorldSavedData.dirty field = " + f.getName());
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("DEBUG: [TSSPC] WorldSavedData.dirty field search error: " + e);
            }
        }

        // MapStorage.loadData を探す: (Class, String) -> WorldSavedData
        try {
            for (Method m : MapStorage.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == Class.class && p[1] == String.class) {
                    mapStorageLoadMethod = m;
                    System.out.println("DEBUG: [TSSPC] MapStorage.loadData = " + m.getName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: [TSSPC] MapStorage.loadData search error: " + e);
        }

        // MapStorage.setData を探す: (String, WorldSavedData) -> void
        try {
            for (Method m : MapStorage.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class
                        && WorldSavedData.class.isAssignableFrom(p[1])) {
                    mapStorageSetMethod = m;
                    System.out.println("DEBUG: [TSSPC] MapStorage.setData = " + m.getName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: [TSSPC] MapStorage.setData search error: " + e);
        }
    }

    public GlobalItemStorageData(String name) {
        super(name);
        final GlobalItemStorageData parent = this;
        storage.setOnContentsChanged(new Runnable() {
            @Override
            public void run() {
                parent.markDirty();
            }
        });
    }

    // WorldSavedData の markDirty() は実行時 SRG 名のため明示オーバーライドでリフレクション経由で呼ぶ
    @Override
    public void markDirty() {
        if (worldSavedDataMarkDirtyMethod != null) {
            try {
                worldSavedDataMarkDirtyMethod.invoke(this);
                return;
            } catch (Exception e) {
                System.out.println("DEBUG: [TSSPC] markDirty invoke error: " + e);
            }
        }
        // フォールバック: dirty フィールドを直接 true にセット
        if (worldSavedDataDirtyField != null) {
            try {
                worldSavedDataDirtyField.set(this, true);
            } catch (Exception e) {
                System.out.println("DEBUG: [TSSPC] dirty field set error: " + e);
            }
        }
    }

    public BulkItemStorage getStorage() {
        return storage;
    }

    public static GlobalItemStorageData get(World world) {
        if (world == null || world.field_72995_K) return null;

        MapStorage mapStorage = world.field_72988_C;
        if (mapStorage == null) return null;

        GlobalItemStorageData instance = null;

        // loadData を呼ぶ
        if (mapStorageLoadMethod != null) {
            try {
                instance = (GlobalItemStorageData) mapStorageLoadMethod.invoke(
                        mapStorage, GlobalItemStorageData.class, DATA_NAME);
            } catch (Exception e) {
                System.out.println("DEBUG: [TSSPC] loadData invoke error: " + e);
            }
        } else {
            System.out.println("DEBUG: [TSSPC] loadData method not found!");
        }

        if (instance == null) {
            instance = new GlobalItemStorageData(DATA_NAME);
            // setData を呼ぶ
            if (mapStorageSetMethod != null) {
                try {
                    mapStorageSetMethod.invoke(mapStorage, DATA_NAME, instance);
                } catch (Exception e) {
                    System.out.println("DEBUG: [TSSPC] setData invoke error: " + e);
                }
            } else {
                System.out.println("DEBUG: [TSSPC] setData method not found!");
            }
        }
        return instance;
    }

    @Override
    public void func_70307_a(NBTTagCompound nbt) {
        if (nbt.hasKey("InventoryData")) {
            storage.func_70307_a(nbt.getCompoundTag("InventoryData"));
        }
    }

    @Override
    public void func_70310_b(NBTTagCompound nbt) {
        NBTTagCompound invNbt = new NBTTagCompound();
        storage.func_70310_b(invNbt);
        nbt.setCompoundTag("InventoryData", invNbt);
    }
}
