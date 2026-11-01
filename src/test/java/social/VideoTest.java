package social;
import org.junit.jupiter.api.Test;
import social.model.*;
import social.visitors.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class VideoTest {
    @Test void everyVisitorSupportsNewElement() {
        Video video = new Video("v", "Demo", 6);
        Element element = video;
        assertEquals(6, element.accept(new EngagementVisitor()));
        assertEquals(4, element.accept(new ContentLengthVisitor()));
        assertEquals(List.of(video), element.accept(new FilterVisitor(e -> e instanceof Video)));
        SummaryVisitor summary = new SummaryVisitor();
        element.accept(summary);
        assertEquals(1, summary.content());
        assertEquals(6, summary.reactions());
        assertEquals("{\"type\":\"video\",\"id\":\"v\",\"text\":\"Demo\",\"reactions\":6}", element.accept(new JsonVisitor()));
    }
    @Test void rejectsNegativeReactions() {
        assertThrows(IllegalArgumentException.class, () -> new Video("v", "Demo", -1));
    }
}
