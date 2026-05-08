package com.tss.pc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

public class BulkItemStorage {
    private final List<ItemStack> inventory = new ArrayList<ItemStack>();
    private Runnable onContentsChanged = null;

    public void setOnContentsChanged(Runnable callback) { this.onContentsChanged = callback; }

    public void onContentsChanged() {
        if (this.onContentsChanged != null) this.onContentsChanged.run();
    }

    // SRG 名問題のある NBT・ItemStack メソッドは MCHelper 経由で呼ぶ

    private boolean isSameExceptDamage(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.itemID != s2.itemID) return false;

        String name1 = MCHelper.itemGetDisplayName(s1);
        String name2 = MCHelper.itemGetDisplayName(s2);
        if (name1 == null || !name1.equals(name2)) return false;

        // NBT 比較: RealCount (表示用の管理タグ) を除いて比較する
        NBTTagCompound tag1 = s1.stackTagCompound != null ? MCHelper.nbtCopy(s1.stackTagCompound) : null;
        NBTTagCompound tag2 = s2.stackTagCompound != null ? MCHelper.nbtCopy(s2.stackTagCompound) : null;

        if (tag1 != null) MCHelper.nbtRemoveTag(tag1, "RealCount");
        if (tag2 != null) MCHelper.nbtRemoveTag(tag2, "RealCount");

        // コピーが取れなかった場合はフィールド直接比較で代用
        if (tag1 == null && tag2 == null) return true;
        if (tag1 == null || tag2 == null) return false;
        return tag1.equals(tag2);
    }

    public int getItemCount(ItemStack target) {
        if (target == null) return 0;
        long total = 0;
        for (ItemStack invStack : this.inventory) {
            if (invStack != null && isSameExceptDamage(invStack, target)) {
                total += invStack.stackSize;
            }
        }
        return (int) total;
    }

    public int addStack(ItemStack stack, int amount) {
        System.out.println("DEBUG: Adding " + amount + " of ID:" + stack.itemID);
        if (stack == null || amount <= 0) return 0;

        boolean merged = false;
        for (ItemStack existing : inventory) {
            if (existing.itemID == stack.itemID
                    && existing.getItemDamage() == stack.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(existing, stack)) {
                existing.stackSize += amount;
                merged = true;
                break;
            }
        }
        if (!merged) {
            ItemStack copy = MCHelper.itemCopy(stack);
            copy.stackSize = amount;
            inventory.add(copy);
        }
        onContentsChanged();
        return amount;
    }

    public boolean removeStack(ItemStack stack, int amount) {
        if (stack == null || amount <= 0) return false;

        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack existing = this.inventory.get(i);
            if (existing != null && existing.itemID == stack.itemID
                    && existing.getItemDamage() == stack.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(existing, stack)) {
                if (existing.stackSize >= amount) {
                    existing.stackSize -= amount;
                    if (existing.stackSize <= 0) this.inventory.remove(i);
                    onContentsChanged();
                    return true;
                }
            }
        }

        // 救済: ID のみで一致
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack existing = this.inventory.get(i);
            if (existing != null && existing.itemID == stack.itemID) {
                if (existing.stackSize >= amount) {
                    existing.stackSize -= amount;
                    if (existing.stackSize <= 0) this.inventory.remove(i);
                    onContentsChanged();
                    return true;
                }
            }
        }
        return false;
    }

    public void withdrawStack(net.minecraft.entity.player.EntityPlayer player, ItemStack target, int amount) {
        if (target == null || player == null) return;
        ItemStack extracted = this.extractActual(target, amount);
        if (extracted != null && extracted.stackSize > 0) {
            if (!player.field_71071_by.func_70441_a(extracted)) {
                player.func_70998_q(extracted, false);
            }
            onContentsChanged();
        }
    }

    public int getCountOf(ItemStack target) {
        if (target == null) return 0;
        int total = 0;
        for (ItemStack s : this.inventory) {
            if (s != null && s.itemID == target.itemID
                    && s.getItemDamage() == target.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(s, target)) {
                total += s.stackSize;
            }
        }
        return total;
    }

    public List<ItemStack> getFilteredItemList(String selectedMod, String search) {
        List<ItemStack> aggregated = new ArrayList<ItemStack>();
        for (ItemStack realStack : inventory) {
            if (!search.isEmpty()) {
                String name = MCHelper.itemGetDisplayName(realStack).toLowerCase();
                if (!name.contains(search.toLowerCase())) continue;
            }

            boolean found = false;
            for (ItemStack aggStack : aggregated) {
                if (isSameExceptDamage(realStack, aggStack)) {
                    aggStack.stackSize += realStack.stackSize;
                    found = true;
                    break;
                }
            }
            if (!found) aggregated.add(MCHelper.itemCopy(realStack));
        }
        Collections.sort(aggregated, new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack s1, ItemStack s2) {
                return Integer.valueOf(s1.itemID).compareTo(s2.itemID);
            }
        });
        return aggregated;
    }

    public ItemStack extractActual(ItemStack displayStack, int amount) {
        System.out.println("DEBUG: Storage size = " + inventory.size());
        for (ItemStack s : inventory) {
            System.out.println("DEBUG: In Storage: " + MCHelper.itemGetDisplayName(s) + " ID:" + s.itemID);
        }

        int remaining = amount;
        ItemStack result = null;
        Iterator<ItemStack> it = inventory.iterator();
        while (it.hasNext() && remaining > 0) {
            ItemStack realStack = it.next();
            if (isSameExceptDamage(realStack, displayStack)) {
                if (result == null) {
                    result = MCHelper.itemCopy(realStack);
                    int toTake = Math.min(remaining, realStack.stackSize);
                    result.stackSize = toTake;
                    realStack.stackSize -= toTake;
                    remaining -= toTake;
                } else {
                    int toTake = Math.min(remaining, realStack.stackSize);
                    realStack.stackSize -= toTake;
                    remaining -= toTake;
                    result.stackSize += toTake;
                }
                if (realStack.stackSize <= 0) it.remove();
            }
        }
        if (result != null) onContentsChanged();
        return result;
    }

    public long getCount(ItemStack displayStack) {
        long count = 0;
        for (ItemStack s : inventory) {
            if (isSameExceptDamage(s, displayStack)) count += s.stackSize;
        }
        return count;
    }

    // 1.5.2 用の NBT 保存 — setTag / appendTag は SRG 名なので MCHelper 経由
    public void func_70310_b(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : inventory) {
            NBTTagCompound sTag = new NBTTagCompound();
            MCHelper.itemWriteToNBT(stack, sTag);
            sTag.setLong("FullCount", (long) stack.stackSize);
            MCHelper.nbtAppendTag(list, sTag);
        }
        MCHelper.nbtSetTag(nbt, "Items", list);
    }

    public void func_70307_a(NBTTagCompound nbt) {
        inventory.clear();
        NBTTagList list = MCHelper.nbtGetTagList(nbt, "Items");
        for (int i = 0; i < MCHelper.nbtTagCount(list); i++) {
            NBTTagCompound sTag = (NBTTagCompound) MCHelper.nbtTagAt(list, i);
            if (sTag == null) continue;
            ItemStack stack = MCHelper.itemLoadFromNBT(sTag);
            if (stack == null) continue;
            if (sTag.hasKey("FullCount")) {
                stack.stackSize = (int) sTag.getLong("FullCount");
            }
            inventory.add(stack);
        }
    }

    public List<ItemStack> getInventoryList() { return this.inventory; }
}
