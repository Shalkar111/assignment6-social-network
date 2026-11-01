package social.visitors;
import social.model.*;
public interface Visitor<R> {
    R visit(Community community);
    R visit(Group group);
    R visit(Post post);
    R visit(Comment comment);
    R visit(Photo photo);
    R visit(Link link);
    R visit(Video video);
}
