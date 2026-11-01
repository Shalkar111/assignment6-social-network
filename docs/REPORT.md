# Assignment 6 Observer and Visitor Patterns

Variant 16 Social network

Saparali Shalkar, SE - 2532

## 1 Purpose and structure

The application models event processing in a social network. PostSource publishes POST_CREATED and ReactionSource publishes REACTION_ADDED. Seven listener classes handle feeds, notifications, trending scores, moderation, activity aggregation, inspections and report storage.

The content snapshot uses a Composite structure with three levels: Community, Group and content. Community and Group are containers. Post, Comment, Photo, Link and Video are leaves. Immutable records and defensive copies make the tree safe to read concurrently.

The application uses Java 17 language/API compatibility. Production code depends only on the JDK; tests use JUnit 5. Main constructs and connects all services in one composition root. Dependencies are passed through constructors.

## 2 Observer design

Subject<E> exposes subscribe, unsubscribe and publish. Observer<E> exposes onEvent. E is bounded by TypedEvent, which provides the event type. NetworkEvent is an immutable record containing type, groupId, contentId, value and text.

Push delivery is appropriate because the event already contains the required data. It avoids a second query to producer state that might have changed. Dispatch is synchronous in the publishing producer thread.

ActivityAggregator is both an observer and a subject. It increments an AtomicInteger for every source event and republishes ACTIVITY_SUMMARY. NotificationListener subscribes to those summaries, forming an observer chain.

The registry is ConcurrentHashMap<EventType, CopyOnWriteArrayList<Observer<E>>>. Registration is idempotent. An iterator sees a snapshot, so handlers may subscribe or unsubscribe during notification. Removal affects future snapshots; an already-started dispatch may still deliver its event. Multiple producers may call the same listener concurrently.

Concurrent queues protect retained posts and reports. Atomic counters protect notification and aggregation totals. TrendingListener uses a concurrent map of atomic scores. Callbacks run outside registry locks; the entire subject is never locked.

## 3 Visitor design

Visitor<R> declares an overload for each concrete element. A call to element.accept(visitor) dynamically selects the element implementation. Inside that method, visitor.visit(this) uses the concrete element type to select the overload and the concrete visitor implementation to perform the operation.

Visitors perform traversal. Container accept methods only dispatch, leaving traversal decisions in the operations. Element records store data; reporting, filtering and export rules are kept outside them.

EngagementVisitor returns the sum of reactions. SummaryVisitor accumulates group, content and reaction totals in instance fields. FilterVisitor returns an immutable list selected by a Predicate<Element>. JsonVisitor builds nested JSON manually and escapes quotes, backslashes, control characters and surrogate code units. ContentLengthVisitor counts leaf text lengths.

A new SummaryVisitor must be used for each independent traversal. InspectionService creates visitors for each event, so mutable visitor state is not shared across producer threads.

## 4 Integration and lifecycle

When a post text contains spam, ModerationListener publishes CONTENT_FLAGGED. InspectionService retrieves the related Group by groupId. It runs FilterVisitor and JsonVisitor on that group, then publishes REPORT_READY. ReportStore retains the result.

The demo flags group g1. The generated JSON contains g1 and its four children, including suspicious link l1. It excludes group g2. An integration test uses two suspicious groups and verifies that only the selected subtree appears.

ProducerRunner uses a fixed ExecutorService with a configurable thread count. Main supplies three finite producer tasks. It waits for submitted futures, calls shutdown and drains the executor before returning. If one producer fails, other submitted producers still finish and the failure is propagated.

Synchronous callbacks finish before their producer completes. This avoids an untracked asynchronous queue during shutdown. Delivery is in-memory; process failure has no persistence guarantee. Callbacks must terminate. Running totals are atomic, but concurrent publications do not provide a global delivery order.

The content tree is a stable read-only snapshot. Live reaction events update trending statistics and do not mutate the snapshot. Snapshot engagement and live event statistics are reported separately.

## 5 Verification

The project has 29 JUnit tests. The test suite passed locally using JDK 24 with javac --release 17 and JUnit Platform 1.10.2 containing JUnit Jupiter 5.10.2.

Tests cover event types, subscriptions, duplicate registration, self-unsubscription, subscription inside a callback, snapshot semantics under concurrent removal, listener failures, observer chains, immutable child lists, all visitors, JSON escaping and related-subtree integration.

The stress test starts four producer tasks, each publishing 1,000 events. Two tasks use PostSource and two use ReactionSource. It checks exactly 4,000 source notifications, 4,000 aggregation updates and 4,000 summary notifications. A second concurrency test checks 3,000 permanent-listener deliveries while another task changes subscriptions.

Hand calculation for g1: 10 + 3 + 5 + 2 = 20 reactions. For g2: 4 + 6 = 10. Total engagement is 30. SummaryVisitor returns 2 groups, 6 content elements and 30 reactions. The spam filter returns only l1. The text-length fixture gives 4 + 2 + 3 + 3 = 12.

The demo output is: 2 feed posts, 3 activity events, 6 notifications including summaries, live trending score 7 for p2, tree engagement 30 and one moderation report with one suspicious element.

## 6 Extension cost

Commit a35ef74 adds ContentLengthVisitor and ContentLengthTest. No element class or Visitor interface changes. This demonstrates that a new operation can be added locally.

Commit a200950 adds Video and VideoTest, updates Visitor and all five existing visitors, updates DemoData and adjusts NetworkTest expected values. Ten files change. A new element is expensive because every visitor must support it.

Visitor is suitable when element types are stable and operations change often. If new content types appear frequently, the cost of editing every visitor may outweigh this benefit.

The README lists exact changed paths and commands to inspect both commits. The supplied Git bundle preserves these commits. Commits have their real creation dates; the two-week development history requirement still requires work over that period.

## 7 Reproduction

Run ./run.sh 3 to compile and start the demo with a three-thread producer pool. Run ./run.sh 4 to select four threads. Run ./test.sh for the test suite; its first run downloads the JUnit runner. Maven users may run mvn test.

The source archive includes the application, tests, pom.xml, scripts, README, this report, defense notes and UML source/image. GitHub publication and LMS submission are separate remaining steps.
