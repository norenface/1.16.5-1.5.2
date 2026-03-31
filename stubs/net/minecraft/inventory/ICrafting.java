package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
public interface ICrafting {
    void updateCraftingInventory(Container container, java.util.List items);
    void sendProgressBarUpdate(Container container, int id, int value);
}
