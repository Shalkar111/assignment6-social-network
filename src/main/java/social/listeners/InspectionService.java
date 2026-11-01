package social.listeners;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import social.events.*;
import social.model.*;
import social.visitors.*;
public final class InspectionService implements Observer<NetworkEvent> {
    private final Map<String, Group> groups;
    private final Subject<NetworkEvent> reports;
    public InspectionService(Map<String, Group> groups, Subject<NetworkEvent> reports) {
        this.groups = Map.copyOf(groups);
        this.reports = Objects.requireNonNull(reports);
    }
    public void onEvent(NetworkEvent event) {
        Group group = groups.get(event.groupId());
        if (group == null) throw new IllegalArgumentException("Unknown group: " + event.groupId());
        var matches = group.accept(new FilterVisitor(node ->
                !(node instanceof Group) && node.text().toLowerCase(Locale.ROOT).contains("spam")));
        String json = group.accept(new JsonVisitor());
        reports.publish(new NetworkEvent(EventType.REPORT_READY, group.id(), event.contentId(),
                matches.size(), json));
    }
}
