package social.listeners;
import java.util.concurrent.atomic.AtomicInteger;
import social.events.*;
public final class NotificationListener implements Observer<NetworkEvent> {
    private final AtomicInteger count = new AtomicInteger();
    public void onEvent(NetworkEvent event) { count.incrementAndGet(); }
    public int count() { return count.get(); }
}
