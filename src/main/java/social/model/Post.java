package social.model;
import java.util.Objects;
import social.visitors.Visitor;
public record Post(String id, String text, int reactions) implements Element {
    public Post {
        Objects.requireNonNull(id);
        Objects.requireNonNull(text);
        if (reactions < 0) throw new IllegalArgumentException("Negative reactions");
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
