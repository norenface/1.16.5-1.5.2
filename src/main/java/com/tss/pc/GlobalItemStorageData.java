package com.tss.pc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class GlobalItemStorageData extends WorldSavedData {
    private static final String DATA_NAME = "tss_pc_storage";
    private final BulkItemStorage storage = new BulkItemStorage();

    // 🌟 1.5.2でロード時に必要なコンストラクタ
    public GlobalItemStorageData(String name) {
        super(name);

        // ストレージに変更があったら markDirty を呼ぶように紐付け
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

    // 🌟 1.5.2 用のデータ取得メソッド
    public static GlobalItemStorageData get(World world) {
        if (world == null || world.field_72995_K) return null;

        MapStorage mapStorage = world.field_72988_C;
        GlobalItemStorageData instance = (GlobalItemStorageData) mapStorage.loadData(GlobalItemStorageData.class, DATA_NAME);

        if (instance == null) {
            instance = new GlobalItemStorageData(DATA_NAME);
            mapStorage.setData(DATA_NAME, instance);
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