package net.minecraftforge.event;
public class Event {
    private boolean canceled = false;
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean cancel) { this.canceled = cancel; }
}
