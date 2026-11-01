package social.events;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventSubject<E extends TypedEvent> implements Subject<E> {
    private final ConcurrentHashMap<EventType, CopyOnWriteArrayList<Observer<E>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public void subscribe(EventType type, Observer<E> observer) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(observer);
        listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).addIfAbsent(observer);
    }

    @Override
    public void unsubscribe(EventType type, Observer<E> observer) {
        var subscribers = listeners.get(Objects.requireNonNull(type));
        if (subscribers != null) subscribers.remove(Objects.requireNonNull(observer));
    }

    @Override
    public void publish(E event) {
        Objects.requireNonNull(event);
        var subscribers = listeners.get(event.type());
        if (subscribers == null) return;
        RuntimeException failure = null;
        // The iterator is a snapshot: changes affect later publications.
        for (Observer<E> observer : subscribers) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException ex) {
                if (failure == null) failure = new IllegalStateException("Listener delivery failed");
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }
}
