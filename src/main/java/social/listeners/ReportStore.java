package social.listeners;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import social.events.*;
public final class ReportStore implements Observer<NetworkEvent> {
    private final ConcurrentLinkedQueue<NetworkEvent> reports = new ConcurrentLinkedQueue<>();
    public void onEvent(NetworkEvent event) { reports.add(event); }
    public List<NetworkEvent> reports() { return List.copyOf(reports); }
}
