package cpw.mods.fml.common.network;
public class NetworkRegistry {
    private static final NetworkRegistry INSTANCE = new NetworkRegistry();
    public static NetworkRegistry instance() { return INSTANCE; }
    public void registerGuiHandler(Object mod, IGuiHandler handler) {}
}
