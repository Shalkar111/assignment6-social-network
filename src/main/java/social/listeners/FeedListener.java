package social.listeners;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import social.events.*;
public final class FeedListener implements Observer<NetworkEvent> {
    private final ConcurrentLinkedQueue<String> posts = new ConcurrentLinkedQueue<>();
    public void onEvent(NetworkEvent event) { posts.add(event.contentId()); }
    public List<String> posts() { return List.copyOf(posts); }
}
