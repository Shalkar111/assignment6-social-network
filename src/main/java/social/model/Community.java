package social.model;
import java.util.List;
import java.util.Objects;
import social.visitors.Visitor;
public record Community(String id, String text, List<Element> children) implements Element {
    public Community {
        Objects.requireNonNull(id);
        Objects.requireNonNull(text);
        children = List.copyOf(children);
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
