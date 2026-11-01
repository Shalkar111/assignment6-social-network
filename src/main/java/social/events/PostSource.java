package social.events;
public final class PostSource extends EventSubject<NetworkEvent> {
    public void post(String groupId, String contentId, int value, String text) {
        publish(new NetworkEvent(EventType.POST_CREATED, groupId, contentId, value, text));
    }
}
