package social.visitors;
import java.util.List;
import social.model.*;
public final class SummaryVisitor implements Visitor<Void> {
    private int groups;
    private int content;
    private int reactions;
    private void traverse(List<Element> children) { children.forEach(child -> child.accept(this)); }
    private Void collect(int count) { content++; reactions += count; return null; }
    public Void visit(Community node) { traverse(node.children()); return null; }
    public Void visit(Group node) { groups++; traverse(node.children()); return null; }
    public Void visit(Post node) { return collect(node.reactions()); }
    public Void visit(Comment node) { return collect(node.reactions()); }
    public Void visit(Photo node) { return collect(node.reactions()); }
    public Void visit(Link node) { return collect(node.reactions()); }
    public int groups() { return groups; }
    public int content() { return content; }
    public int reactions() { return reactions; }
}
