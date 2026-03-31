package com.tss.pc;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class FavoriteConfig {
    // config/tss_pc/favorites.dat に保存
    private static final File SAVE_FILE = new File("config/tss_pc/favorites.dat");

    public static void saveAll(Map<String, List<ComputerBlockEntity.FavoriteTab>> allPlayerTabs) {
        try {
            if (!SAVE_FILE.getParentFile().exists()) SAVE_FILE.getParentFile().mkdirs();

            NBTTagCompound root = new NBTTagCompound();
            for (Map.Entry<String, List<ComputerBlockEntity.FavoriteTab>> entry : allPlayerTabs.entrySet()) {
                NBTTagList tabList = new NBTTagList();
                for (ComputerBlockEntity.FavoriteTab tab : entry.getValue()) {
                    NBTTagCompound tabTag = new NBTTagCompound();
                    tabTag.setString("name", tab.name);
                    if (tab.icon != null) {
                        NBTTagCompound iconTag = new NBTTagCompound();
                        tab.icon.writeToNBT(iconTag);
                        tabTag.setCompoundTag("icon", iconTag);
                    }
                    NBTTagList slots = new NBTTagList();
                    for (int i = 0; i < tab.slots.size(); i++) {
                        if (tab.slots.get(i) != null) {
                            NBTTagCompound s = new NBTTagCompound();
                            s.setInteger("index", i);
                            tab.slots.get(i).writeToNBT(s);
                            slots.appendTag(s);
                        }
                    }
                    tabTag.setTag("slots", slots);
                    tabList.appendTag(tabTag);
                }
                root.setTag(entry.getKey(), tabList);
            }
            CompressedStreamTools.writeCompressed(root, new FileOutputStream(SAVE_FILE));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void loadAll(Map<String, List<ComputerBlockEntity.FavoriteTab>> allPlayerTabs) {
        if (!SAVE_FILE.exists()) return;
        try {
            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(SAVE_FILE));
            allPlayerTabs.clear();
            for (Object key : root.getTags()) {
                String playerName = ((net.minecraft.nbt.NBTBase)key).getName();
                NBTTagList tabList = root.getTagList(playerName);
                java.util.ArrayList<ComputerBlockEntity.FavoriteTab> list = new java.util.ArrayList<ComputerBlockEntity.FavoriteTab>();
                for (int i = 0; i < tabList.tagCount(); i++) {
                    NBTTagCompound tabTag = (NBTTagCompound) tabList.tagAt(i);
                    ComputerBlockEntity.FavoriteTab tab = new ComputerBlockEntity.FavoriteTab(tabTag.getString("name"));
                    if (tabTag.hasKey("icon")) tab.icon = net.minecraft.item.ItemStack.loadItemStackFromNBT(tabTag.getCompoundTag("icon"));

                    NBTTagList slots = tabTag.getTagList("slots");
                    for (int j = 0; j < slots.tagCount(); j++) {
                        NBTTagCompound s = (NBTTagCompound) slots.tagAt(j);
                        int idx = s.getInteger("index");
                        // 行追加などでサイズが変わる可能性があるので調整
                        while (idx >= tab.slots.size()) tab.slots.add(null);
                        tab.slots.set(idx, net.minecraft.item.ItemStack.loadItemStackFromNBT(s));
                    }
                    list.add(tab);
                }
                allPlayerTabs.put(playerName, list);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}