package social.events;
public interface Subject<E extends TypedEvent> {
    void subscribe(EventType type, Observer<E> observer);
    void unsubscribe(EventType type, Observer<E> observer);
    void publish(E event);
}
