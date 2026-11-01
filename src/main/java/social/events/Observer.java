package social.events;
@FunctionalInterface
public interface Observer<E extends TypedEvent> { void onEvent(E event); }
