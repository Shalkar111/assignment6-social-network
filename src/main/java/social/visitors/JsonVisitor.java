package social.visitors;
import java.util.List;
import java.util.stream.Collectors;
import social.model.*;
public final class JsonVisitor implements Visitor<String> {
    private String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (ch < 32 || Character.isSurrogate(ch)) result.append(String.format("\\u%04x", (int) ch));
                    else result.append(ch);
                }
            }
        }
        return result.append('"').toString();
    }
    private String header(String type, Element node) {
        return "{\"type\":" + quote(type) + ",\"id\":" + quote(node.id()) + ",\"text\":" + quote(node.text());
    }
    private String container(String type, Element node, List<Element> children) {
        return header(type, node) + ",\"children\":[" +
                children.stream().map(child -> child.accept(this)).collect(Collectors.joining(",")) + "]}";
    }
    private String leaf(String type, Element node, int reactions) {
        return header(type, node) + ",\"reactions\":" + reactions + "}";
    }
    public String visit(Community node) { return container("community", node, node.children()); }
    public String visit(Group node) { return container("group", node, node.children()); }
    public String visit(Post node) { return leaf("post", node, node.reactions()); }
    public String visit(Comment node) { return leaf("comment", node, node.reactions()); }
    public String visit(Photo node) { return leaf("photo", node, node.reactions()); }
    public String visit(Link node) { return leaf("link", node, node.reactions()); }
    public String visit(Video node) { return leaf("video", node, node.reactions()); }
}
