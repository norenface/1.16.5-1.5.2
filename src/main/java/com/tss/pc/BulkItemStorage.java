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

    // 1.5.2 互換の「同一アイテム判定」（Damage(耐久値)を除外して比較）
// BulkItemStorage.java
// BulkItemStorage.java
// BulkItemStorage.java 内のメソッドを差し替え
    private boolean isSameExceptDamage(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) return false;

        // 1. IDチェック
        if (s1.itemID != s2.itemID) return false;

        // 2. 名前チェック
        String name1 = s1.getDisplayName();
        String name2 = s2.getDisplayName();
        if (name1 == null || !name1.equals(name2)) return false;

        // 3. NBTのクリーニングと比較
        // 元のアイテムを汚さないようにコピーを作成
        NBTTagCompound tag1 = s1.stackTagCompound != null ? (NBTTagCompound)s1.stackTagCompound.copy() : null;
        NBTTagCompound tag2 = s2.stackTagCompound != null ? (NBTTagCompound)s2.stackTagCompound.copy() : null;

        // 🌟 比較の邪魔になる管理用タグを削除
        if (tag1 != null) tag1.removeTag("RealCount");
        if (tag2 != null) tag2.removeTag("RealCount");

        // クリーニング後の「実質的な中身」判定
        boolean hasNBT1 = tag1 != null && !tag1.getTags().isEmpty();
        boolean hasNBT2 = tag2 != null && !tag2.getTags().isEmpty();

        // 両方空なら一致 (これで金鉱石などが通るはず)
        if (!hasNBT1 && !hasNBT2) return true;

        // 有無の食い違いログ
        if (hasNBT1 != hasNBT2) {
            System.out.println("DEBUG [NBT EXISTENCE DIFF]: " + name1);
            System.out.println("  Storage Has Content: " + hasNBT1 + " / Request Has Content: " + hasNBT2);
            if (hasNBT1) System.out.println("  Storage Content: " + tag1.toString());
            if (hasNBT2) System.out.println("  Request Content: " + tag2.toString());
            return false;
        }

        // 内容一致チェック
        boolean nbtEqual = tag1.equals(tag2);
        if (!nbtEqual) {
            System.out.println("DEBUG [NBT CONTENT DIFF]: " + name1);
            System.out.println("  S1 (Storage cleaned): " + tag1.toString());
            System.out.println("  S2 (Request cleaned): " + tag2.toString());
        }

        return nbtEqual;
    }
    /**
     * 指定されたアイテムがストレージ内に合計何個あるか返す
     */
    public int getItemCount(ItemStack target) {
        if (target == null) return 0;
        long total = 0;

        for (ItemStack invStack : this.inventory) {
            if (invStack != null && isSameExceptDamage(invStack, target)) {
                total += invStack.stackSize;
            }
        }
        // intの範囲に収めて返す（表示用なのでこれでOK）
        return (int)total;
    }

    // アイテム投入 (1.16.5 の insertItem)
    public int addStack(ItemStack stack, int amount) {

        System.out.println("DEBUG: Adding " + amount + " of ID:" + stack.itemID);
        if (stack == null || amount <= 0) return 0;

        boolean merged = false;
        for (ItemStack existing : inventory) {
            // matchesの代わり: アイテムID、ダメージ、タグが完全一致か
            if (existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage() && ItemStack.areItemStackTagsEqual(existing, stack)) {
                existing.stackSize += amount;
                merged = true;
                break;
            }
        }
        if (!merged) {
            ItemStack copy = stack.copy();
            copy.stackSize = amount;
            inventory.add(copy);
        }
        onContentsChanged();
        return amount;
    }

    // 指定アイテムの削除 (救済措置ロジックを完全維持)
    public boolean removeStack(ItemStack stack, int amount) {
        if (stack == null || amount <= 0) return false;

        // 第1段階：厳密なチェック
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack existing = this.inventory.get(i);
            if (existing != null && existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage() && ItemStack.areItemStackTagsEqual(existing, stack)) {
                if (existing.stackSize >= amount) {
                    existing.stackSize -= amount;
                    if (existing.stackSize <= 0) this.inventory.remove(i);
                    onContentsChanged();
                    return true;
                }
            }
        }

        // 第2段階：救済措置 (同じItem IDであれば許容)
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

    // 🌟 1.5.2 互換形式の引き出しメソッド
    public void withdrawStack(net.minecraft.entity.player.EntityPlayer player, ItemStack target, int amount) {
        if (target == null || player == null) return;

        // 1. ストレージから実体を引き出す (既存の extractActual を利用)
        ItemStack extracted = this.extractActual(target, amount);

        if (extracted != null && extracted.stackSize > 0) {
            // 2. プレイヤーのインベントリに加える
            // 1.5.2 では player.field_71071_by.addItemStackToInventory を使用
            if (!player.field_71071_by.func_70441_a(extracted)) {
                // 3. インベントリがいっぱいなら足元にドロップ
                // 1.5.2 での確実なドロップメソッド: dropPlayerItemWithRandomChoice
                // (第2引数は「ランダムに散らすか」のフラグ)
                player.func_70998_q(extracted, false);
            }

            // 4. 変更を通知
            onContentsChanged();
        }
    }
    // BulkItemStorage.java に追加
    public int getCountOf(ItemStack target) {
        if (target == null) return 0;

        int total = 0;
        for (ItemStack s : this.inventory) {
            if (s != null) {
                // ID、ダメージ値、NBTタグが一致するかチェック
                boolean isSame = s.itemID == target.itemID &&
                        s.getItemDamage() == target.getItemDamage() &&
                        ItemStack.areItemStackTagsEqual(s, target);

                if (isSame) {
                    total += s.stackSize;
                }
            }
        }
        return total;
    }
    // フィルタリング表示 (Mod名でのフィルタリングを IDベースに変更)
    public List<ItemStack> getFilteredItemList(String selectedMod, String search) {
        List<ItemStack> aggregated = new ArrayList<ItemStack>();
        for (ItemStack realStack : inventory) {
            // ※1.5.2にはNamespaceがないため、Modフィルタリングを維持するには別途管理が必要
            // ここでは簡易的に search のみ実装（selectedMod は all のみ対応）
            if (!search.isEmpty()) {
                String name = realStack.getDisplayName().toLowerCase();
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
            if (!found) aggregated.add(realStack.copy());
        }
        // ソート (ID順)
        Collections.sort(aggregated, new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack s1, ItemStack s2) {
                return Integer.valueOf(s1.itemID).compareTo(s2.itemID);
            }
        });
        return aggregated;
    }

    // 実アイテムの抽出
    public ItemStack extractActual(ItemStack displayStack, int amount) {

        System.out.println("DEBUG: Storage size = " + inventory.size());
        for(ItemStack s : inventory) {
            System.out.println("DEBUG: In Storage: " + s.getDisplayName() + " ID:" + s.itemID);
        }
        int remaining = amount;
        ItemStack result = null;
        Iterator<ItemStack> it = inventory.iterator();
        while (it.hasNext() && remaining > 0) {
            ItemStack realStack = it.next();
            if (isSameExceptDamage(realStack, displayStack)) {
                if (result == null) {
                    result = realStack.copy();
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

    // 🌟 1.5.2 用のデータ保存 (serialize/deserialize)
    public void func_70310_b(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : inventory) {
            NBTTagCompound sTag = new NBTTagCompound();
            stack.writeToNBT(sTag);
            // 1.5.2はstackSizeがbyte(127まで)なので、巨大な数を保存するために別途Longで保存
            sTag.setLong("FullCount", (long)stack.stackSize);
            list.appendTag(sTag);
        }
        nbt.setTag("Items", list);
    }

    public void func_70307_a(NBTTagCompound nbt) {
        inventory.clear();
        NBTTagList list = nbt.getTagList("Items");
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound sTag = (NBTTagCompound) list.tagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(sTag);
            if (sTag.hasKey("FullCount")) {
                stack.stackSize = (int)sTag.getLong("FullCount");
            }
            if (stack != null) inventory.add(stack);
        }
    }

    public List<ItemStack> getInventoryList() { return this.inventory; }
}