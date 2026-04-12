package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import java.util.*;
public abstract class Container {
    public List field_75151_b = new ArrayList(); // inventorySlots
    protected Slot func_75125_e(Slot slot) { slot.slotNumber = field_75151_b.size(); field_75151_b.add(slot); return slot; } // addSlotToContainer
    public abstract boolean func_75145_c(EntityPlayer player); // canInteractWith
    public ItemStack func_75140_a(int slotId, int button, int modifier, EntityPlayer player) { return null; } // slotClick
    public ItemStack func_75150_a(EntityPlayer player, int slotIndex) { return null; } // transferStackInSlot
    public void func_75142_b() {}  // detectAndSendChanges
    public Slot func_75139_a(int slotId) { return (Slot) field_75151_b.get(slotId); } // getSlot
    public void func_75134_a(EntityPlayer player) {} // onContainerClosed
    public void func_75128_b(ICrafting crafter) {}  // addCraftingToCrafters
}
