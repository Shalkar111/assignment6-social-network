package social;
import java.util.*;
import social.events.*;
import social.listeners.*;
import social.model.*;
import social.visitors.*;

public final class Main {
    public static void main(String[] args) throws Exception {
        int threads = args.length == 0 ? 3 : Integer.parseInt(args[0]);
        Community community = DemoData.community();
        Map<String, Group> groups = new HashMap<>();
        for (Element element : community.children()) {
            Group group = (Group) element;
            groups.put(group.id(), group);
        }
        PostSource posts = new PostSource();
        ReactionSource reactions = new ReactionSource();
        EventSubject<NetworkEvent> alerts = new EventSubject<>();
        EventSubject<NetworkEvent> reports = new EventSubject<>();
        FeedListener feed = new FeedListener();
        NotificationListener notifications = new NotificationListener();
        TrendingListener trending = new TrendingListener();
        ActivityAggregator aggregator = new ActivityAggregator();
        ReportStore store = new ReportStore();
        ModerationListener moderation = new ModerationListener(alerts);
        InspectionService inspection = new InspectionService(groups, reports);
        posts.subscribe(EventType.POST_CREATED, feed);
        posts.subscribe(EventType.POST_CREATED, moderation);
        posts.subscribe(EventType.POST_CREATED, notifications);
        posts.subscribe(EventType.POST_CREATED, aggregator);
        reactions.subscribe(EventType.REACTION_ADDED, trending);
        reactions.subscribe(EventType.REACTION_ADDED, notifications);
        reactions.subscribe(EventType.REACTION_ADDED, aggregator);
        aggregator.subscribe(EventType.ACTIVITY_SUMMARY, notifications);
        alerts.subscribe(EventType.CONTENT_FLAGGED, inspection);
        reports.subscribe(EventType.REPORT_READY, store);
        new ProducerRunner(threads).run(List.of(
                () -> posts.post("g1", "p1", 0, "Java study"),
                () -> posts.post("g1", "l1", 0, "spam offer"),
                () -> reactions.react("g2", "p2", 7, "Like")));
        System.out.println("Producer threads: " + threads);
        System.out.println("Feed posts: " + feed.posts().size());
        System.out.println("Activity events: " + aggregator.total());
        System.out.println("Notifications including summaries: " + notifications.count());
        System.out.println("Trending p2: " + trending.score("p2"));
        System.out.println("Tree engagement: " + community.accept(new EngagementVisitor()));
        SummaryVisitor summary = new SummaryVisitor();
        community.accept(summary);
        System.out.println("Tree groups: " + summary.groups() + ", content: " + summary.content());
        System.out.println("Moderation reports: " + store.reports().size());
        for (NetworkEvent report : store.reports()) {
            System.out.println("Report group: " + report.groupId() + ", flagged elements: " + report.value());
            System.out.println(report.text());
        }
    }
}
