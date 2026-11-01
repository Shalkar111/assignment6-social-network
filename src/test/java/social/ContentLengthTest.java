package social;
import org.junit.jupiter.api.Test;
import social.model.*;
import social.visitors.ContentLengthVisitor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ContentLengthTest {
    @Test void countsLeafTextWithoutChangingElementClasses() {
        Group group = new Group("g", "Ignored title", List.of(
                new Post("p", "Java", 1), new Comment("c", "Hi", 0),
                new Photo("ph", "Pic", 0), new Link("l", "URL", 0)));
        assertEquals(12, group.accept(new ContentLengthVisitor()));
    }
}
