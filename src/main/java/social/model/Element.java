package social.model;
import social.visitors.Visitor;
public interface Element {
    String id();
    String text();
    <R> R accept(Visitor<R> visitor);
}
