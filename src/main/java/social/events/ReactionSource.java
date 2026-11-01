package social.events;
public final class ReactionSource extends EventSubject<NetworkEvent> {
    public void react(String groupId, String contentId, int value, String text) {
        publish(new NetworkEvent(EventType.REACTION_ADDED, groupId, contentId, value, text));
    }
}
