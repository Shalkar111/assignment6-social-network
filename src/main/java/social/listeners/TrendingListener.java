package social.listeners;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import social.events.*;
public final class TrendingListener implements Observer<NetworkEvent> {
    private final ConcurrentHashMap<String, AtomicInteger> scores = new ConcurrentHashMap<>();
    public void onEvent(NetworkEvent event) {
        scores.computeIfAbsent(event.contentId(), id -> new AtomicInteger()).addAndGet(event.value());
    }
    public int score(String id) { var score = scores.get(id); return score == null ? 0 : score.get(); }
}
