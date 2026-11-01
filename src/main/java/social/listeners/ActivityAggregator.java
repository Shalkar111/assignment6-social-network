package social.listeners;
import java.util.concurrent.atomic.AtomicInteger;
import social.events.*;
public final class ActivityAggregator extends EventSubject<NetworkEvent> implements Observer<NetworkEvent> {
    private final AtomicInteger total = new AtomicInteger();
    public void onEvent(NetworkEvent event) {
        int current = total.incrementAndGet();
        publish(new NetworkEvent(EventType.ACTIVITY_SUMMARY, event.groupId(), event.contentId(), current, "Activity total"));
    }
    public int total() { return total.get(); }
}
