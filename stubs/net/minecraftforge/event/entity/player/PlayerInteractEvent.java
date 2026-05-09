package net.minecraftforge.event.entity.player;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.Event;
public class PlayerInteractEvent extends Event {
    public enum Action { LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR }
    public final EntityPlayer entityPlayer;
    public final Action action;
    public final int x, y, z, face;
    public PlayerInteractEvent(EntityPlayer player, Action action, int x, int y, int z, int face) {
        this.entityPlayer = player;
        this.action = action;
        this.x = x; this.y = y; this.z = z; this.face = face;
    }
}
