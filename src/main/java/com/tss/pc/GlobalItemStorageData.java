package com.tss.pc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import java.lang.reflect.Method;

public class GlobalItemStorageData extends WorldSavedData {
    private static final String DATA_NAME = "tss_pc_storage";
    private final BulkItemStorage storage = new BulkItemStorage();

    // MapStorageのloadData/setDataはSRG名のためリフレクションで検索する
    private static Method mapStorageLoadMethod;
    private static Method mapStorageSetMethod;

    static {
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
