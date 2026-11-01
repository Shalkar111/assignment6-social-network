# Social network

Assignment 6, variant 16. Java 17, Observer and Visitor patterns.

## Run and test

Install JDK 17 or newer. On macOS/Linux:

```sh
./run.sh
./run.sh 4
./test.sh
```

The scripts compile with `--release 17`. The default producer pool size is 3; the first argument changes it. The first test run downloads the JUnit Platform standalone runner to the ignored `.deps/` folder. Application code uses only the JDK; JUnit 5 is the only test dependency. Maven is optional: `mvn test`.

Open the folder containing `pom.xml` as a project in IntelliJ IDEA, select JDK 17, and run `social.Main` or the test classes.

## Domain and tree

The model is a read-only snapshot of social network content:

```text
Community c1 Campus
  Group g1 Students
    Post p1           10 reactions
    Comment c2         3 reactions
    Photo ph1          5 reactions
    Link l1            2 reactions, spam offer
  Group g2 Clubs
    Post p2            4 reactions
    Video v1           6 reactions
```

There are three levels, two container types and five leaf types. Records store data and implement `accept`; they contain no reporting or moderation rules. Child lists are defensively copied. Events describe activity against IDs in this snapshot; they do not mutate snapshot reaction counts. Therefore live trending and snapshot engagement are separate measurements.

## Observer

`PostSource` and `ReactionSource` extend our generic `EventSubject<E>`. Both use our own `Subject<E>` and `Observer<E>` contracts. `NetworkEvent` is an immutable record; `EventType` selects the subscribers.

| Listener | Input | Responsibility |
| --- | --- | --- |
| FeedListener | POST_CREATED | Retain post IDs |
| NotificationListener | POST_CREATED, REACTION_ADDED, ACTIVITY_SUMMARY | Count notifications |
| TrendingListener | REACTION_ADDED | Accumulate live reaction scores |
| ModerationListener | POST_CREATED | Publish CONTENT_FLAGGED for spam |
| ActivityAggregator | POST_CREATED, REACTION_ADDED | Publish ACTIVITY_SUMMARY |
| InspectionService | CONTENT_FLAGGED | Visit the affected group |
| ReportStore | REPORT_READY | Retain immutable reports |

Push is used because the event carries an immutable payload. A listener does not have to query changing producer state. Delivery is synchronous in the producer thread, which makes completion and error propagation explicit.

The chain `source -> ActivityAggregator -> NotificationListener` demonstrates a listener that is also a subject. Summary values are unique atomic running totals, but concurrent callbacks can observe summaries out of order. Consumers must not assume global event ordering.

## Visitor

`Visitor<R>` has an overload for each concrete element. `Element.accept(visitor)` dispatches to the concrete element implementation; the call `visitor.visit(this)` then selects the matching overload and visitor implementation.

| Visitor | Result |
| --- | --- |
| EngagementVisitor | Integer sum of snapshot reactions |
| SummaryVisitor | Stateful accumulation of groups, content and reactions |
| FilterVisitor | Immutable list of matching elements |
| JsonVisitor | Nested JSON with escaping |
| ContentLengthVisitor | Sum of leaf text lengths in UTF-16 code units |

Visitors own traversal. Container `accept` methods only perform dispatch. This allows a visitor to choose its own order, stop early or skip children without putting traversal policy in data classes. Stateful `SummaryVisitor` is created per traversal and is not shared between threads. Stateless visitors can also be instantiated per task.

## Integration

`POST_CREATED -> ModerationListener -> CONTENT_FLAGGED -> InspectionService -> REPORT_READY -> ReportStore`.

Inspection looks up the group directly by `groupId`, then runs `FilterVisitor` and `JsonVisitor` on that group only. It never traverses the community or unrelated groups. In the demo, only g1 appears in the report; g2 and its content are absent. Unknown group IDs fail explicitly.

## Concurrency and Clean Code

`ProducerRunner` creates an `ExecutorService` with a configurable fixed pool and submits three finite demo producers. Futures are joined and the executor is shut down and drained before return, including when one producer fails or the caller is interrupted. Synchronous listener chains finish before a producer future finishes. Handlers are required to terminate; there is no forced cancellation of business callbacks.

`ConcurrentHashMap<EventType, CopyOnWriteArrayList<Observer<E>>>` stores subscriptions. No subject-wide lock is used and callbacks run outside registry locks. Duplicate subscriptions are ignored. Each publication iterates a snapshot: removal may still allow the already-started publication to invoke a listener once, and subscription inside a handler takes effect on later publications. Empty event-type lists remain registered, avoiding races caused by replacing lists.

Atomic integers and concurrent queues protect listener state. The content tree and group index are immutable. A failing listener does not prevent other listeners in the same snapshot from receiving the event; the subject collects runtime failures and then throws. There is no automatic retry or durable storage.

`Main` is the composition root. Service dependencies are supplied through constructors. Business services use no service locator, singleton or lazy dependency initialization.

## Tests and hand calculations

There are 29 JUnit tests. They cover event routing, duplicate subscriptions, self-unsubscription, subscription during callbacks, concurrent removal, failure handling, observer chaining, all visitor results, JSON escaping, immutable containers and subtree integration.

The stress test uses four producers with 1,000 events each, split between two sources. Exact assertions: 4,000 source notifications, 4,000 aggregated events and 4,000 summary notifications. Another test changes subscriptions concurrently with three publishers and checks 3,000 permanent-listener deliveries.

Manual visitor values: g1 = 10 + 3 + 5 + 2 = 20; g2 = 4 + 6 = 10; total engagement = 30. Summary = 2 groups and 6 content elements. The filter selects only l1. A separate text-length fixture gives 4 + 2 + 3 + 3 = 12.

## Trade-off commits

Two separate commits demonstrate the Visitor extension cost:

1. `a35ef74` Add content length visitor without changing element classes.
   Changed: `src/main/java/social/visitors/ContentLengthVisitor.java` and `src/test/java/social/ContentLengthTest.java`. No element or Visitor interface was edited. Adding an operation is local.
2. `a200950` Add Video element and update all visitors.
   Changed: `src/main/java/social/model/Video.java`, `src/main/java/social/DemoData.java`, `src/main/java/social/visitors/Visitor.java`, `EngagementVisitor.java`, `SummaryVisitor.java`, `FilterVisitor.java`, `JsonVisitor.java`, `ContentLengthVisitor.java` (all in the visitors folder), `src/test/java/social/NetworkTest.java` and `src/test/java/social/VideoTest.java`. Adding an element requires updating every visitor. Visitor is less suitable if element types change frequently.

Use `git show --stat a35ef74` and `git show --stat a200950` in the restored repository. A clean source ZIP and a separate Git bundle are provided. Restore history using:

```sh
git clone SocialNetwork-history.bundle social-network
cd social-network
git log --oneline
```

All commits use their actual creation date. The assignment's two-week history requirement requires continued work over that period and is not satisfied by several same-day commits.

## Documentation

- `docs/classes.png`: UML class diagram.
- `docs/classes.puml`: editable PlantUML source.
- `docs/REPORT.md`: report.
- `docs/DEFENSE_RU.md`: explanations and practice for defense.
- `docs/TEST_RESULTS.txt`: concise verification results.
