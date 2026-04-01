package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
public class Slot {
    public final IInventory inventory;
    public int slotNumber;
    public int xDisplayPosition;
    public int yDisplayPosition;
    public Slot(IInventory inventory, int slotIndex, int xPos, int yPos) {
        this.inventory = inventory; this.slotNumber = slotIndex; this.xDisplayPosition = xPos; this.yDisplayPosition = yPos;
    }
    public boolean func_75214_a(ItemStack stack) { return true; }   // isItemValid
    public ItemStack func_75211_c() { return inventory.func_70301_a(slotNumber); }  // getStack
    public boolean func_75216_f() { return func_75211_c() != null; } // getHasStack
    public void func_75212_b(ItemStack stack) { inventory.func_70299_a(slotNumber, stack); func_75220_f(); } // putStack
    public void func_75220_f() { inventory.func_70296_a(); }         // onSlotChanged
    public boolean func_82869_a(EntityPlayer player) { return true; } // canTakeStack
    public ItemStack func_75209_e(int amount) { return inventory.func_70298_a(slotNumber, amount); } // decrStackSize
    public void func_82870_a(EntityPlayer player, ItemStack stack) {} // onPickupFromSlot
    public int getSlotIndex() { return slotNumber; }
    public int getSlotStackLimit() { return 64; }
}
