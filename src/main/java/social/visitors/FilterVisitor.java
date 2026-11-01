package social.visitors;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import social.model.*;
public final class FilterVisitor implements Visitor<List<Element>> {
    private final Predicate<Element> predicate;
    public FilterVisitor(Predicate<Element> predicate) { this.predicate = Objects.requireNonNull(predicate); }
    private List<Element> match(Element node) { return predicate.test(node) ? List.of(node) : List.of(); }
    private List<Element> container(Element node, List<Element> children) {
        return Stream.concat(match(node).stream(),
                children.stream().flatMap(child -> child.accept(this).stream())).toList();
    }
    public List<Element> visit(Community node) { return container(node, node.children()); }
    public List<Element> visit(Group node) { return container(node, node.children()); }
    public List<Element> visit(Post node) { return match(node); }
    public List<Element> visit(Comment node) { return match(node); }
    public List<Element> visit(Photo node) { return match(node); }
    public List<Element> visit(Link node) { return match(node); }
}
