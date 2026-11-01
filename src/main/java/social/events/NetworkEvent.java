package social.events;
import java.util.Objects;
public record NetworkEvent(EventType type, String groupId, String contentId, int value, String text)
        implements TypedEvent {
    public NetworkEvent {
        Objects.requireNonNull(type);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(contentId);
        Objects.requireNonNull(text);
    }
}
