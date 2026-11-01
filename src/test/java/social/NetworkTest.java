package social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import social.events.*;
import social.listeners.*;
import social.model.*;
import social.visitors.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {
    private NetworkEvent event(EventType type) {
        return new NetworkEvent(type, "g1", "p1", 1, "Hello");
    }

    @Test void dispatchesOnlyMatchingType() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        AtomicInteger count = new AtomicInteger();
        subject.subscribe(EventType.POST_CREATED, e -> count.incrementAndGet());
        subject.publish(event(EventType.REACTION_ADDED));
        assertEquals(0, count.get());
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, count.get());
    }

    @Test void unsubscribeStopsFutureDelivery() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener listener = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, listener);
        subject.publish(event(EventType.POST_CREATED));
        subject.unsubscribe(EventType.POST_CREATED, listener);
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, listener.count());
    }

    @Test void duplicateSubscriptionDeliversOnce() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener listener = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, listener);
        subject.subscribe(EventType.POST_CREATED, listener);
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, listener.count());
    }

    @Test void listenerCanUnsubscribeItself() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        AtomicInteger count = new AtomicInteger();
        social.events.Observer<NetworkEvent> listener = new social.events.Observer<>() {
            public void onEvent(NetworkEvent e) {
                count.incrementAndGet();
                subject.unsubscribe(e.type(), this);
            }
        };
        subject.subscribe(EventType.POST_CREATED, listener);
        subject.publish(event(EventType.POST_CREATED));
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, count.get());
    }

    @Test void subscriptionInsideHandlerStartsWithNextEvent() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener newcomer = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, e -> subject.subscribe(e.type(), newcomer));
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(0, newcomer.count());
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, newcomer.count());
    }

    @Test void unsubscribeDuringDeliveryUsesSnapshot() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener later = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, e -> subject.unsubscribe(e.type(), later));
        subject.subscribe(EventType.POST_CREATED, later);
        subject.publish(event(EventType.POST_CREATED));
        subject.publish(event(EventType.POST_CREATED));
        assertEquals(1, later.count());
    }

    @Test void failingListenerDoesNotSkipOtherListeners() {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener later = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, e -> { throw new IllegalArgumentException("failure"); });
        subject.subscribe(EventType.POST_CREATED, later);
        var failure = assertThrows(IllegalStateException.class, () -> subject.publish(event(EventType.POST_CREATED)));
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(1, later.count());
    }

    @Test void eventsRejectNullFields() {
        assertThrows(NullPointerException.class, () -> new NetworkEvent(null, "g", "p", 1, ""));
    }

    @Test void aggregatorRepublishesNewEvent() {
        ActivityAggregator aggregator = new ActivityAggregator();
        ReportStore store = new ReportStore();
        aggregator.subscribe(EventType.ACTIVITY_SUMMARY, store);
        aggregator.onEvent(event(EventType.POST_CREATED));
        aggregator.onEvent(event(EventType.REACTION_ADDED));
        assertEquals(List.of(1, 2), store.reports().stream().map(NetworkEvent::value).toList());
        assertEquals(2, aggregator.total());
    }

    @Test void twoSourcesNotifyFiveDistinctListeners() {
        PostSource posts = new PostSource();
        ReactionSource reactions = new ReactionSource();
        FeedListener feed = new FeedListener();
        NotificationListener notices = new NotificationListener();
        TrendingListener trending = new TrendingListener();
        ActivityAggregator aggregator = new ActivityAggregator();
        ReportStore alerts = new ReportStore();
        EventSubject<NetworkEvent> alertSubject = new EventSubject<>();
        alertSubject.subscribe(EventType.CONTENT_FLAGGED, alerts);
        ModerationListener moderation = new ModerationListener(alertSubject);
        posts.subscribe(EventType.POST_CREATED, feed);
        posts.subscribe(EventType.POST_CREATED, notices);
        posts.subscribe(EventType.POST_CREATED, moderation);
        posts.subscribe(EventType.POST_CREATED, aggregator);
        reactions.subscribe(EventType.REACTION_ADDED, trending);
        reactions.subscribe(EventType.REACTION_ADDED, aggregator);
        posts.post("g1", "p1", 0, "SPAM");
        reactions.react("g1", "p1", 2, "Like");
        assertEquals(List.of("p1"), feed.posts());
        assertEquals(1, notices.count());
        assertEquals(2, trending.score("p1"));
        assertEquals(2, aggregator.total());
        assertEquals(1, alerts.reports().size());
    }

    @Test void numericVisitorMatchesHandCalculation() {
        // Students: 10 + 3 + 5 + 2 = 20; Clubs: 4 + 6 = 10. Total = 30.
        assertEquals(30, DemoData.community().accept(new EngagementVisitor()));
    }

    @Test void statefulVisitorAccumulatesAcrossBothGroups() {
        SummaryVisitor visitor = new SummaryVisitor();
        DemoData.community().accept(visitor);
        assertEquals(2, visitor.groups());
        assertEquals(6, visitor.content());
        assertEquals(30, visitor.reactions());
    }

    @Test void filterReturnsMatchingElements() {
        var matches = DemoData.community().accept(new FilterVisitor(e -> e.text().contains("spam")));
        assertEquals(List.of("l1"), matches.stream().map(Element::id).toList());
    }

    @Test void filterMaySelectContainerAndLeaf() {
        var matches = DemoData.community().accept(new FilterVisitor(e -> e.id().equals("g2") || e.id().equals("p2")));
        assertEquals(List.of("g2", "p2"), matches.stream().map(Element::id).toList());
    }

    @Test void jsonHasCorrectNestedStructure() {
        Community tree = new Community("c", "C", List.of(new Group("g", "G", List.of(new Post("p", "P", 2)))));
        String expected = "{\"type\":\"community\",\"id\":\"c\",\"text\":\"C\",\"children\":["
                + "{\"type\":\"group\",\"id\":\"g\",\"text\":\"G\",\"children\":["
                + "{\"type\":\"post\",\"id\":\"p\",\"text\":\"P\",\"reactions\":2}]}]}";
        assertEquals(expected, tree.accept(new JsonVisitor()));
    }

    @Test void jsonEscapesQuotesBackslashesAndControls() {
        String json = new Post("p", "\"\\\n\r\t\u0001", 0).accept(new JsonVisitor());
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\r"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\u0001"));
        assertFalse(json.contains("\n"));
    }

    @Test void emptyContainersExportAndCalculate() {
        Group group = new Group("g", "Empty", List.of());
        assertEquals(0, group.accept(new EngagementVisitor()));
        assertTrue(group.accept(new JsonVisitor()).endsWith("\"children\":[]}"));
    }

    @Test void childListCannotBeChanged() {
        var children = new java.util.ArrayList<Element>();
        Group group = new Group("g", "G", children);
        children.add(new Post("p", "P", 0));
        assertTrue(group.children().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> group.children().add(new Post("q", "Q", 0)));
    }

    @Test void negativeReactionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Post("p", "P", -1));
    }

    @Test void eventTriggersVisitorOnlyOnRelatedSubtree() {
        Group first = new Group("g1", "One", List.of(new Post("p1", "spam first", 1)));
        Group second = new Group("g2", "Two", List.of(new Post("p2", "spam second", 1)));
        EventSubject<NetworkEvent> reports = new EventSubject<>();
        ReportStore store = new ReportStore();
        reports.subscribe(EventType.REPORT_READY, store);
        InspectionService inspection = new InspectionService(Map.of("g1", first, "g2", second), reports);
        EventSubject<NetworkEvent> alerts = new EventSubject<>();
        alerts.subscribe(EventType.CONTENT_FLAGGED, inspection);
        PostSource source = new PostSource();
        source.subscribe(EventType.POST_CREATED, new ModerationListener(alerts));
        source.post("g1", "p1", 0, "spam first");
        assertEquals(1, store.reports().size());
        NetworkEvent report = store.reports().get(0);
        assertEquals("g1", report.groupId());
        assertEquals(1, report.value());
        assertTrue(report.text().contains("p1"));
        assertFalse(report.text().contains("p2"));
    }

    @Test void unknownSubtreeIsReportedAsError() {
        InspectionService inspection = new InspectionService(Map.of(), new EventSubject<>());
        assertThrows(IllegalArgumentException.class, () -> inspection.onEvent(event(EventType.CONTENT_FLAGGED)));
    }

    @Test @Timeout(15) void fourThreadsTimesThousandEventsDeliverExactCount() throws Exception {
        PostSource posts = new PostSource();
        ReactionSource reactions = new ReactionSource();
        ActivityAggregator aggregator = new ActivityAggregator();
        NotificationListener delivered = new NotificationListener();
        NotificationListener summaries = new NotificationListener();
        posts.subscribe(EventType.POST_CREATED, delivered);
        reactions.subscribe(EventType.REACTION_ADDED, delivered);
        posts.subscribe(EventType.POST_CREATED, aggregator);
        reactions.subscribe(EventType.REACTION_ADDED, aggregator);
        aggregator.subscribe(EventType.ACTIVITY_SUMMARY, summaries);
        Runnable postProducer = () -> { for (int i = 0; i < 1000; i++) posts.post("g1", "p1", 0, "Post"); };
        Runnable reactionProducer = () -> { for (int i = 0; i < 1000; i++) reactions.react("g1", "p1", 1, "Like"); };
        new ProducerRunner(4).run(List.of(postProducer, postProducer, reactionProducer, reactionProducer));
        assertEquals(4000, delivered.count());
        assertEquals(4000, aggregator.total());
        assertEquals(4000, summaries.count());
    }

    @Test @Timeout(15) void concurrentSubscriptionChangesKeepPermanentListener() throws Exception {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        NotificationListener permanent = new NotificationListener();
        NotificationListener temporary = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, permanent);
        Runnable publisher = () -> { for (int i = 0; i < 1000; i++) subject.publish(event(EventType.POST_CREATED)); };
        Runnable changer = () -> {
            for (int i = 0; i < 1000; i++) {
                subject.subscribe(EventType.POST_CREATED, temporary);
                subject.unsubscribe(EventType.POST_CREATED, temporary);
            }
        };
        new ProducerRunner(4).run(List.of(publisher, publisher, publisher, changer));
        assertEquals(3000, permanent.count());
        assertTrue(temporary.count() <= 3000);
    }

    @Test @Timeout(10) void snapshotSurvivesConcurrentUnsubscribe() throws Exception {
        EventSubject<NetworkEvent> subject = new EventSubject<>();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        NotificationListener later = new NotificationListener();
        subject.subscribe(EventType.POST_CREATED, e -> {
            entered.countDown();
            try { if (!proceed.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout"); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
        });
        subject.subscribe(EventType.POST_CREATED, later);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> subject.publish(event(EventType.POST_CREATED)));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            subject.unsubscribe(EventType.POST_CREATED, later);
            proceed.countDown();
            future.get(5, TimeUnit.SECONDS);
            subject.publish(event(EventType.POST_CREATED));
            assertEquals(1, later.count());
        } finally {
            proceed.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test @Timeout(10) void runnerDrainsOtherProducersBeforePropagatingFailure() {
        AtomicInteger completed = new AtomicInteger();
        assertThrows(ExecutionException.class, () -> new ProducerRunner(3).run(List.of(
                () -> { throw new IllegalArgumentException("failure"); },
                () -> { for (int i = 0; i < 1000; i++) completed.incrementAndGet(); },
                () -> { for (int i = 0; i < 1000; i++) completed.incrementAndGet(); })));
        assertEquals(2000, completed.get());
    }

    @Test void invalidThreadCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ProducerRunner(0));
    }
}
