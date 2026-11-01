package social.listeners;
import java.util.Locale;
import java.util.Objects;
import social.events.*;
public final class ModerationListener implements Observer<NetworkEvent> {
    private final Subject<NetworkEvent> alerts;
    public ModerationListener(Subject<NetworkEvent> alerts) { this.alerts = Objects.requireNonNull(alerts); }
    public void onEvent(NetworkEvent event) {
        if (event.text().toLowerCase(Locale.ROOT).contains("spam")) {
            alerts.publish(new NetworkEvent(EventType.CONTENT_FLAGGED, event.groupId(), event.contentId(), 1, "Spam review"));
        }
    }
}
