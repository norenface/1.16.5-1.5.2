package net.minecraft.network;
import net.minecraft.network.packet.Packet;
public interface INetworkManager {
    void addToSendQueue(Packet packet);
    void networkShutdown(String reason, Object... args);
}
