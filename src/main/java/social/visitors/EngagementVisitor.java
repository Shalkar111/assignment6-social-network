package social.visitors;
import java.util.List;
import social.model.*;
public final class EngagementVisitor implements Visitor<Integer> {
    private int sum(List<Element> children) {
        return children.stream().mapToInt(child -> child.accept(this)).sum();
    }
    public Integer visit(Community node) { return sum(node.children()); }
    public Integer visit(Group node) { return sum(node.children()); }
    public Integer visit(Post node) { return node.reactions(); }
    public Integer visit(Comment node) { return node.reactions(); }
    public Integer visit(Photo node) { return node.reactions(); }
    public Integer visit(Link node) { return node.reactions(); }
    public Integer visit(Video node) { return node.reactions(); }
}
