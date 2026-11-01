package social;
import java.util.List;
import social.model.*;
public final class DemoData {
    private DemoData() {}
    public static Community community() {
        return new Community("c1", "Campus", List.of(
                new Group("g1", "Students", List.of(new Post("p1", "Java study", 10),
                        new Comment("c2", "Useful notes", 3), new Photo("ph1", "Campus photo", 5),
                        new Link("l1", "spam offer", 2))),
                new Group("g2", "Clubs", List.of(new Post("p2", "Chess meeting", 4),
                        new Video("v1", "Chess video", 6)))));
    }
}
