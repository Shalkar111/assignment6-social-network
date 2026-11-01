package social.model;
import java.util.List;
import java.util.Objects;
import social.visitors.Visitor;
public record Group(String id, String text, List<Element> children) implements Element {
    public Group {
        Objects.requireNonNull(id);
        Objects.requireNonNull(text);
        children = List.copyOf(children);
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
