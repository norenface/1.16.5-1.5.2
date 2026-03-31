package cpw.mods.fml.common.network;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
public interface IPacketHandler {
    void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player);
}
