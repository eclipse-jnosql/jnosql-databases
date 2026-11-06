# Shared Database Driver Development Guide

Main architecture guide for contributors (human or AI) who add or maintain a database driver in this repository.
Start from [`AGENTS.md`](../AGENTS.md). Testing conventions live in [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc) and are not repeated here.
Contribution conventions live in [`CONTRIBUTING_DATABASE.adoc`](../CONTRIBUTING_DATABASE.adoc).

Model-specific guides: [key-value](key-value.md), [column](column.md), [document](document.md), [graph](graph.md), [time-series](time-series.md), [vector](vector.md).

> Reference implementations show what exists, not what is ideal. Where this guide points out a weakness, it is a lesson, not a refactoring request.

---

## 1. Understanding the architecture

### 1.1 Layers

```
Application (Jakarta NoSQL Template / Jakarta Data repository)
        │
Eclipse JNoSQL mapping       org.eclipse.jnosql.mapping.*        (external: eclipse-jnosql/jnosql)
        │   entity <-> CommunicationEntity, templates, repositories, CDI producers
Eclipse JNoSQL communication org.eclipse.jnosql.communication.*  (external: eclipse-jnosql/jnosql)
        │   DatabaseConfiguration -> DatabaseManagerFactory -> DatabaseManager
Driver (this repository)     org.eclipse.jnosql.databases.<db>.communication
        │
Native client                Jedis, CqlSession, MongoClient, InfluxDBClient, Graph, MilvusClientV2, ...
```

A driver is the bottom adapter: it turns communication-level objects (`CommunicationEntity`, `SelectQuery`, `DeleteQuery`, `CriteriaCondition`) into native calls, and native results back into `CommunicationEntity`.
It does not know about annotated application entities. That is the mapping layer's job.

> **Which package?** Drivers in this repository implement `org.eclipse.jnosql.communication.*`.
> A separate `jakarta.nosql.communication.*` API exists in the specification repository, but no module here imports it.
> Check imports in the reference module before assuming a type.

### 1.2 The two contract families

| Family | Package | Chain |
|---|---|---|
| Key-value | `org.eclipse.jnosql.communication.keyvalue` | `KeyValueConfiguration` → `BucketManagerFactory` → `BucketManager` |
| Semistructured (column, document, time series, vector, graph) | `org.eclipse.jnosql.communication.semistructured` | `DatabaseConfiguration` → `DatabaseManagerFactory` → `DatabaseManager` |

Responsibilities:

- **Configuration** (`DatabaseConfiguration` / `KeyValueConfiguration`): a `Function<Settings, …Factory>`. Reads `Settings`, validates them, creates or receives the native client, returns a factory.
- **Factory** (`DatabaseManagerFactory` / `BucketManagerFactory`): a `Function<String, …Manager>` and `AutoCloseable`. Binds a database/keyspace/bucket name to a manager. Typically owns native-client lifetime.
- **Manager** (`DatabaseManager` / `BucketManager`): executes operations (`insert`, `update`, `delete`, `select`, `count`; or `put`, `get`, `delete`).

`DatabaseManager` also provides default methods (`exists`, `singleResult`, `query`/`prepare` through `QueryParser`, `selectCursor`, `count(SelectQuery)`, `update(UpdateQuery)`, `deleteAndCount`, `defaultIdFieldName()`).
Implement the abstract methods; override defaults only when the native database does better.

Model-specific extensions:

| Model | Manager type | Extra operations |
|---|---|---|
| Vector | `org.eclipse.jnosql.mapping.vector.VectorManager` | `search(VectorSelectQuery)` |
| Graph | `org.eclipse.jnosql.communication.graph.GraphDatabaseManager` | `edge`, `remove`, `deleteEdge`, `findEdgeById` |
| Time series | `DatabaseManager` | Temporal queries (`latest`, `since`, …) are built by the framework as conditions + sort + limit on the id field |

### 1.3 Real source references

| Concern | Redis | Cassandra | MongoDB | InfluxDB | TinkerPop | Milvus |
|---|---|---|---|---|---|---|
| Configuration | `RedisConfiguration` | `CassandraConfiguration` | `MongoDBDocumentConfiguration` | `InfluxDBTimeSeriesConfiguration` | `DatabaseConfigurationAdapter` (+ SPI `GraphConfiguration`) | `MilvusConfiguration` |
| Property keys | `RedisConfigurations` | `CassandraConfigurations` | `MongoDBDocumentConfigurations` | `InfluxDBTimeSeriesConfigurations` | `DatabaseConfigurationAdapter.TINKERPOP_PROVIDER` | `MilvusConfigurations` |
| Factory | `DefaultRedisBucketManagerFactory` | `CassandraColumnManagerFactory` | `MongoDBDocumentManagerFactory` | `InfluxDBTimeSeriesManagerFactory` | inner class of `DatabaseConfigurationAdapter` | `MilvusVectorManagerFactory` |
| Manager | `RedisBucketManager` | `DefaultCassandraColumnManager` | `MongoDBDocumentManager` | `InfluxDBTimeSeriesManager` | `DefaultTinkerpopGraphDatabaseManager` | `MilvusVectorManager` |
| Query translation | n/a | `QueryUtils`, `Relations`, `DeleteQueryConverter` | `DocumentQueryConversor` | `InfluxDBQueryConverter` | `TraversalExecutor` | `MilvusFilterConverter` |
| Entity/value conversion | `RedisBucketManager` (JSON) | `CassandraConverter` | `MongoDBUtils`, `MongoDBValueWriteDecorator` | `InfluxDBEntityConverter` | `CommunicationEntityConverter` | `MilvusRowConverter`, `MilvusVectors` |

All paths follow `jnosql-<db>/src/main/java/org/eclipse/jnosql/databases/<db>/communication/` (Redis, for example:
`jnosql-redis/src/main/java/org/eclipse/jnosql/databases/redis/communication/RedisConfiguration.java`).

### 1.4 Configuration and connection management

- Property keys are an enum named `<Database>Configurations`, with keys prefixed `jnosql.<db>.`.
- `apply(Settings)` does the work. A convenience `get(Map)` or `get(NativeClient)` entry point helps tests. `MongoDBDocumentConfiguration` and `MilvusConfiguration` both offer them.
- Validate required settings eagerly. `InfluxDBTimeSeriesConfiguration` rejects blank URL/token with `IllegalArgumentException`. `MilvusCollectionSettings` is a record validating its values.
- Grouped settings are best modelled as records (`MilvusConnectionSettings`, `MilvusCollectionSettings`).
- Redis picks single node, Sentinel or Cluster from key prefixes, and models each with a resolver (`RedisConfigurationsResolver`, a sealed interface). That is a good fit for a closed set of connection modes.

### 1.5 Query translation versus execution

Translation turns a communication condition tree into a native query form. Execution sends it and reads results.

| Driver | Translation (no I/O) | Execution (I/O) |
|---|---|---|
| MongoDB | `DocumentQueryConversor.convert(CriteriaCondition)` → `Bson` | `MongoDBDocumentManager.select` |
| InfluxDB | `InfluxDBQueryConverter` → `InfluxDBQuery(statement, parameters)` record | `InfluxDBTimeSeriesManager` |
| Milvus | `MilvusFilterConverter.convert(Optional<CriteriaCondition>)` → `Filter(expression, parameters)` | `MilvusVectorManager` |
| TinkerPop | `TraversalExecutor.getPredicate(CriteriaCondition)` → `GraphTraversal` | `DefaultTinkerpopGraphDatabaseManager` |

Translators that return a value object (`InfluxDBQuery`, `Filter`) are easy to unit-test without a database.

### 1.6 Conversion boundaries

- Entity conversion maps `CommunicationEntity`/`Element` to native rows/documents/points and back.
- Value conversion handles individual types. Register custom `ValueWriter`/`ValueReader` instances through `META-INF/services`.
  `MongoDBValueWriteDecorator` composes `CompositeValueWriter` with `UUIDValueWriter`. `BinaryValueReader` is a registered `ValueReader` in the same module.
- Native types must not escape to the standard API. See [section 9](#9-common-architectural-mistakes).

### 1.7 Shared infrastructure (`jnosql-database-commons`)

Under `jnosql-database-commons/src/main/java/org/eclipse/jnosql/`:

| Class | Use |
|---|---|
| `communication/driver/CompositeValueWriter`, `UUIDValueWriter` | Compose value writers (see `MongoDBValueWriteDecorator`, `MilvusValueWriteDecorator`). |
| `communication/driver/JsonbSupplier` (+ default and service-loader implementations) | Shared JSON-B instance (used by Redis). |
| `communication/driver/ConfigurationReader` | Load properties from the classpath (test setup). |
| `communication/driver/IntegrationTest` | Constants for the integration switch `jnosql.test.integration`. |
| `communication/driver/StringMatch` | Wildcard patterns for LIKE-style matching. |
| `communication/driver/ValueJSON` | A `Value` backed by a JSON string. |
| `communication/driver/attachment/*` | Binary attachments (`EntityAttachment`, byte-array and path variants). |
| `mapping/driver/ParamUtil` | Extracts `@Param`-annotated parameters for custom query annotations. |

Search here before writing a helper.

### 1.8 Service registration and CDI

- Register communication entry points with `ServiceLoader` files under `src/main/resources/META-INF/services/`:
  - `org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration`
  - `org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration`
  - `org.eclipse.jnosql.communication.ValueReader` / `ValueWriter` for custom value handling
  - `jakarta.enterprise.inject.spi.Extension` for CDI extensions (`CassandraExtension`, `TinkerpopExtension`)
- Provide `META-INF/beans.xml` when the module has CDI beans.
- A `mapping` package is only needed for database-specific CDI beans/templates (`MongoDBTemplate`, `CassandraTemplate`, `BucketManagerFactorySupplier`).
  `jnosql-influxdb` and `jnosql-milvus` have none. The generic mapping modules supply `Template`/repository support, selected with `jnosql.<model>.provider` or by the single `DatabaseConfiguration` found.

### UUID Compatibility and Conversion

Every new driver must decide, explicitly, how `java.util.UUID` is stored, queried and read back. Do not assume a `UUID` can be handed to the native client, and do not assume a native UUID type removes the need for framework conversion.

**Existing infrastructure (reuse it)**

| Type | Module | Behavior |
|---|---|---|
| `org.eclipse.jnosql.communication.reader.UUIDValueReader` | `jnosql-communication-core` (framework repo `../jnosql`) | Registered `ValueReader`. `read(UUID.class, value)` returns a `UUID` as is, parses a `CharSequence` with `UUID.fromString`, throws `CommunicationException` (cause preserved) for malformed text, and returns `null` for any other input type. |
| `jnosql-database-commons/src/main/java/org/eclipse/jnosql/communication/driver/UUIDValueWriter.java` | `jnosql-database-commons` | `ValueWriter<UUID, String>`. Supports only `UUID.class`. Writes `uuid.toString()`, and `null` for `null`. |
| `CompositeValueWriter` | `jnosql-database-commons` | Composes writers. See `MongoDBValueWriteDecorator`, `MilvusValueWriteDecorator`. |

Because `UUIDValueWriter` and `UUIDValueReader` already form a symmetric `UUID -> String -> UUID` pair, a driver that stores UUIDs as text should reuse them. Do not write another UUID-to-string converter.

**Decide per database**

| Database capability | What to do |
|---|---|
| No native UUID type | Write as `String` through `UUIDValueWriter` (as `jnosql-mongodb`, `jnosql-milvus`, `jnosql-arangodb`, `jnosql-qdrant` and `jnosql-weaviate` do through their `*ValueWriteDecorator`). The reader side is already covered by `UUIDValueReader`. |
| Native UUID type | A native mapping is allowed (`QuestDBEntityConverter` writes and reads a native UUID; `FieldValueConverter` in `jnosql-oracle-nosql` maps it to a string field). Convert the native value back to `java.util.UUID` or to a UUID `String` in the `CommunicationEntity`. Never return a driver-specific UUID or binary type, because `UUIDValueReader` returns `null` for it. |
| Restricted id types (for example Qdrant points accept unsigned integers or UUIDs) | Isolate the mapping in one class (`QdrantPointIds`), keep the original id recoverable, and document what happens to other id values. |

**Where UUIDs appear.** Handle all five consistently, because bugs usually appear in only one:

1. *Entity identifier*: the stored id must read back equal to the original `UUID`.
2. *Entity attribute*: the same conversion as for ids.
3. *Persistence and retrieval*: insert, update, select and delete by id.
4. *Query parameters*: `eq`, `in`, `between` and similar conditions must convert UUID values with the same writer used for storage. A filter converter that skips this will never match stored data. `MilvusFilterConverter` normalizes `UUID` to `String` for this reason.
5. *Query results*: convert the native representation back before returning the `CommunicationEntity`, so that `element.get(UUID.class)` works.

**Rules**

- *Symmetry*: `read(write(uuid))` must equal `uuid`, and a stored value written by an earlier version must remain readable.
- *Invalid values*: malformed text must fail with `CommunicationException` and keep the cause (the `UUIDValueReader` behavior). Do not swallow it and do not return `null`. Tolerant fallbacks (such as the deterministic UUID in `QdrantPointIds`) are a documented, tested exception.
- *Null*: `null` stays `null`. Do not turn it into an empty string or a zero UUID.
- *Registration*: register custom `ValueReader` or `ValueWriter` implementations in `META-INF/services/org.eclipse.jnosql.communication.ValueReader` and `ValueWriter`, or compose them in a decorator used by your converters. Verify that the registration is actually exercised. A writer that exists but is not on the conversion path has no effect.
- *Contracts*: do not change `CommunicationEntity`, `Element` or `Value` semantics. They already convert through the registered readers.
- *Model limits*: if a model cannot carry UUIDs in some position (for example, a key-value store that only stores opaque values, or a time-series measurement with a restricted timestamp field), document the limitation in the model guide and in the driver Javadoc. Do not write a test for unsupported behavior.

**Tests** (style: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc); verified examples: `UUIDValueWriterTest`, `MongoDBValueWriteDecoratorTest`, `MilvusValueWriteDecoratorTest`, `MongoDBDocumentManagerTest.shouldInsertUUID`)

- Unit: the writer or decorator converts a UUID, leaves other types untouched, and `test(UUID.class)` is true only for UUID.
- Unit: round trip `UUID -> stored representation -> UUID` returns an equal value, for the native and the converted form if both exist.
- Unit: malformed text raises `CommunicationException`. `null` stays `null`.
- Integration: UUID as entity id, persisted and found again by that id.
- Integration: UUID as an attribute, where the model supports attributes.
- Integration: UUID as a query parameter (`eq`, and `in` where supported) returns the stored entity.

```java
// Adapted from MongoDBDocumentManagerTest.shouldInsertUUID and the select-by-_id usage
var id = UUID.randomUUID();
var entity = CommunicationEntity.of("person");
entity.add("_id", id);
entity.add("uuid", UUID.randomUUID());

manager.insert(entity);

var query = select().from("person").where("_id").eq(id).build();
var found = manager.select(query).findFirst().orElseThrow();

assertThat(found.find("_id").orElseThrow().get(UUID.class)).isEqualTo(id);
```

`select` and `CommunicationEntity` come from `org.eclipse.jnosql.communication.semistructured`. Whether `_id` is the id field name depends on the database. Check the driver's own convention.

---

## 2. Semantic design

Names are the first API contract. A reader should know what a type does, for which database and model, from its name alone.

| Prefer | Avoid | Why |
|---|---|---|
| `DocumentQueryConversor` / `MilvusFilterConverter` | `QueryHelper`, `DataProcessor` | States what is converted and the direction. |
| `VectorManager`, `ColumnManagerFactory` | `ManagerHelper`, `CommonUtil` | Model and role are visible. |
| `InfluxDBEntityConverter` | `ObjectHandler` | Names the source and target concepts. |
| `MilvusThreshold`, `MilvusMetric` | `Settings2`, `Options` | Domain terms. |

These are illustrations, not mandatory class names.

Guidelines:

1. **Prefix with the database** for public/discoverable types (`MongoDBDocumentManager`). Package-private collaborators may drop it when the package already says it.
2. **Name by role**: `…Configuration`, `…ManagerFactory`, `…Manager`, `…Converter`, `…Schema`, `…Metric`.
3. **Verbs match behavior.** `insert` inserts and `update` updates. If `update` is really an upsert, say so in Javadoc. `DefaultCassandraColumnManager.update(entity)` delegates to `insert` because Cassandra writes are upserts. `MilvusVectorManager.insert` is likewise an upsert.
4. **Return values are consistent.** `select` returns a stream or list, empty when nothing matches. It never returns null. `get` returns `Optional`.
5. **Exceptions match semantics.** See [section 7](#7-error-handling).
6. **Domain objects over primitives.** Records such as `InfluxDBQuery(statement, parameters)` and `MilvusFilterConverter.Filter(expression, parameters)` carry meaning that two loose strings do not.
7. **Names must not lie.** `CassandraQuery.toString()` prints `CouchDBDocumentQuery`, a copy-paste leftover. Review copied code for stale names.

Semantic consistency checklist: for each public operation, the name, the Javadoc, the return value, the thrown exceptions and the tests must agree.

---

## 3. SOLID principles

Apply SOLID to reduce the cost of change. Stop when more structure would cost more than it saves.

### 3.1 Single Responsibility

Give each class one reason to change.

- Good separation seen in the repo: `MilvusFilterConverter` (filters), `MilvusRowConverter` (rows), `MilvusCollectionSchema` (schema), `MilvusMetric` (metric mapping), `MilvusThreshold` (threshold), `MilvusVectorManager` (orchestration).
- `InfluxDBQueryConverter` and `InfluxDBEntityConverter` are separated by concern (query vs. entity) and neither opens a connection.
- Watch for managers that grow. `DefaultCassandraColumnManager` delegates statement building to `QueryUtils` and row conversion to `CassandraConverter`, which keeps it smaller. `MilvusVectorManager` is cohesive but already large. New features should go into collaborators, not into it.
- Trade-off: splitting every private method into a class hurts readability. Split when a part has its own tests, its own reason to change, or a different dependency set.

### 3.2 Open/Closed

Extend behavior without editing stable code.

- ServiceLoader registration lets new drivers be added without changing the framework.
- `QueryExecutor.of(SelectQuery)` and the `QueryExecutorType` enum in Cassandra choose between paging-state and default execution without a conditional in the manager.
- Closed enums such as `Condition` are handled with a `switch` plus a `default` that throws `UnsupportedOperationException`. That is acceptable when arms are one-liners. When arms grow into multi-step logic, extract them (see [section 4](#4-strategy-and-behavioral-design)).
- Trade-off: a plug-in point nobody uses is speculative. Add one when a second variant exists or is concretely planned.

### 3.3 Liskov Substitution

A `DatabaseManager` from your driver must behave like any other `DatabaseManager` to its callers.

- Do not strengthen preconditions silently. `MongoDBDocumentManager.update(entity)` requires an `_id` and `MilvusVectorManager.update` likewise. Document such requirements.
- Unsupported operations (TTL on most drivers) throw `UnsupportedOperationException` rather than silently ignoring the argument. Callers can then handle it consistently.
- Do not return partially valid results. Empty collections for "nothing found", exceptions for "cannot do".
- Be careful with default methods: `singleResult` relies on `select` honoring limits.

### 3.4 Interface Segregation

- Prefer narrow, role-based interfaces. `RedisBucketManagerFactory` extends `BucketManagerFactory` with just two Redis-specific structures (`getSortedSet`, `getCounter`) instead of widening the standard contract.
- `CassandraColumnManager` extends `DatabaseManager` with CQL/consistency operations. Optional features sit on a sub-interface, so code depending on the standard type is unaffected.
- Trade-off: one-method interfaces for every collaborator add noise. Use an interface at a boundary (public API, test seam, multiple implementations), not for every class.

### 3.5 Dependency Inversion

- Depend on the communication contracts and `Settings`, not on concrete configuration sources.
- Pass the native client in (`MilvusConfiguration.get(MilvusClientV2)`, `MongoDBDocumentConfiguration.get(MongoClient)`) so tests and callers can substitute it.
- Provider-neutral graph configuration: `GraphConfiguration` (a `Function<Settings, Graph>`) lets `jnosql-tinkerpop` stay independent of any one graph vendor.
- Trade-off: constructor injection of a native client is enough. A custom "client abstraction" wrapping the native one is usually speculative.

---

## 4. Strategy and behavioral design

### 4.1 When Strategy (or typed dispatch) is appropriate

Use it when **behavior varies independently and variants are interchangeable behind one contract**, and the variant set is growing or provider-dependent.

| Variation point | Example | Guidance |
|---|---|---|
| Query-operation translation | `Condition` → native predicate | A `switch` with one-line arms is fine. If each arm grows to multiple steps or needs its own state, extract a small translator per operation. |
| Conversion algorithms | Per-type value writers | Already modeled by `ValueWriter`/`ValueReader` and `CompositeValueWriter` (composition of strategies). Add a new writer, not a new `if`. |
| Database-specific query execution | Cassandra paging vs default | `QueryExecutor` + `QueryExecutorType`. This already solves the variation. Reuse, don't rebuild. |
| Vector metric / representation | `MilvusMetric`, `MilvusVectors.Kind` (DENSE, SPARSE, MULTI) | Enums with behavior per constant, or exhaustive `switch` over a closed enum or sealed hierarchy. Each constant maps to index type, metric type and native query object. |
| Provider capabilities | Graph providers, optional features | Validate capabilities explicitly. Select behavior from a capability, not from `instanceof` on vendor classes. |

### 4.2 When *not* to use it

- Guard clauses: `Objects.requireNonNull`, BETWEEN needs exactly two values, IN must be non-empty. These are validation, not behavior variation.
- A two-branch `if` that will not grow.
- A `switch` over a closed enum with trivial arms, as in `TraversalExecutor` or `InfluxDBQueryConverter`.

Rule of thumb: a growing chain (`if type == A … else if type == B …`) that is changed every time a variant is added is a Strategy candidate. A fixed chain is not.

### 4.3 Other patterns, only when justified

| Pattern | Responsibility | Example in repo | Trade-off |
|---|---|---|---|
| Factory | Create a manager bound to a name, hide client wiring | `MongoDBDocumentManagerFactory`, `CassandraColumnManagerFactory` | Required by the contract. Keep it free of query logic. |
| Adapter | Present a native API as the standard contract | `DatabaseConfigurationAdapter` (adapts `GraphConfiguration` to `DatabaseConfiguration`) | Keep native types inside the adapter. |
| Decorator | Add behavior around an existing one | `MongoDBValueWriteDecorator` (wraps `CompositeValueWriter`); `CassandraQuery` wraps `SelectQuery` with paging state | Stateful decorators (`CassandraQuery` is mutable) need care with thread-safety. |
| Builder | Complex immutable construction | `UDTBuilder` (Cassandra), `SettingsBuilder` | Use only when there are many optional parts. Records suffice for small value objects. |
| Template Method | Shared skeleton with fixed steps | `AbstractSemiStructuredTemplate` (framework) is extended by `DefaultMongoDBTemplate` | Introduces inheritance. Prefer composition unless the skeleton is stable. |

Do not add a pattern to satisfy a checklist.

---

## 5. Driver implementation workflow

Legend: **R** required, **M** model-specific, **O** optional.

| # | Step | Kind | Notes |
|---|---|---|---|
| 1 | Identify the database model | R | Possibly more than one (see [section 6](#6-multi-model-databases)). |
| 2 | Check the Jakarta NoSQL / JNoSQL interfaces | R | Read the contract types in section 1.2 in the sibling `eclipse-jnosql/jnosql` sources. |
| 3 | Identify native driver dependencies | R | Choose the official client. Add the dependency to the module's `pom.xml`. Respect existing dependency management. |
| 4 | Choose the reference implementation | R | See the model guide. |
| 5 | Define the Maven module | R | New `jnosql-<db>` module, registered in the root `pom.xml`, depending on `jnosql-database-commons` and the relevant mapping module. Do not add a parent module just for classification. |
| 6 | Implement configuration | R | `<Db>Configuration` and `<Db>Configurations`. |
| 7 | Implement client and resource lifecycle | R | Decide one owner for the native client and its `close()`. |
| 8 | Implement manager and factory | R | Standard contract plus unsupported-operation behavior. |
| 9 | Implement query conversion | M | Not for key-value. Required for semistructured models. |
| 10 | Implement entity/value conversion | R | Keep separate from persistence calls. |
| 11 | Add mapping integration | O/M | Only for database-specific templates or repositories (CDI). |
| 12 | Register providers and services | R | `META-INF/services/...` and `beans.xml` where relevant. |
| 13 | Add unit tests | R | Converters, configuration and managers with fakes. |
| 14 | Add integration tests | R | Real database, gated by `jnosql.test.integration`. |
| 15 | Run the Jakarta NoSQL TCK | M | Where applicable (see section 8). |
| 16 | Validate compatibility and build | R | `mvn -pl jnosql-<db> verify`, plus the integration switch and TCK as applicable. |

Implementation tips:

- Implement incrementally: configuration + factory, then insert/select, then delete/count, then update, then the optional features.
- Write the unsupported-operation behavior explicitly (throw), never silently ignore.
- Commit each logical step (Conventional Commits).

---

## 6. Multi-model databases

Some databases serve several models (for example key-value and document, or document and vector). Model them as one integration with several thin model-specific faces.

| Question | Guidance |
|---|---|
| Share configuration? | **Yes** when connection settings are identical. One `Configurations` enum, one settings reader. Add model-specific keys only for model-specific options. |
| Share the native client? | **Yes.** Create it once and inject it into each manager or factory. |
| Reuse converters? | **Yes** for value/type conversion that is model-independent. **No** if the model changes the representation (a vector row is not a document). |
| Separate managers? | **Yes.** One manager per model contract (`DatabaseManager` vs `VectorManager` vs `BucketManager`), each small. |
| Model-specific interfaces? | Only for real extra operations. `VectorManager` and `GraphDatabaseManager` extend `DatabaseManager`. Do not add an interface to mark a category. |
| Avoid duplicating connection logic? | Extract a single connection/client factory used by every configuration class. |
| Maven structure | A single module may expose several `ServiceLoader` registrations. Do not create extra parent modules for classification. |

Pitfall: when two models register `DatabaseConfiguration` in the same module, the mapping layer needs `jnosql.<model>.provider` to choose. Document the property.

---

## 7. Error handling

| Situation | Behavior |
|---|---|
| Null argument | `Objects.requireNonNull(arg, "arg is required")` with a clear message (the repo style). |
| Invalid but non-null input (blank URL, wrong vector dimension, BETWEEN without two values) | `IllegalArgumentException` with the offending value or name. |
| Capability the database does not support (TTL, sort in a vector scan, OR in Cassandra) | `UnsupportedOperationException` naming the capability. |
| Illegal state (use after close, collection not created by JNoSQL) | `IllegalStateException`. |
| Native failure | Let it propagate, or wrap in `CommunicationException` (`DefaultTinkerpopGraphDatabaseManager.close()` does) **with the cause attached**. |
| Missing entity on update | An explicit, documented exception (TinkerPop throws `EmptyResultException`). Do not choose silently. |

Rules:

- Preserve the cause. Never `catch (Exception e) { return null; }`.
- Do not swallow failures. `GraphTransactionUtil` logs commit exceptions at INFO and continues. Treat that as a trade-off to avoid copying, not as a model.
- Clean up in `close()` and try-with-resources. `InfluxDBTimeSeriesManagerFactory.close()` is a good example: it closes every client it created, aggregates failures into one `IllegalStateException` with suppressed causes, and rejects use after close.
- Connectivity failures should surface at first use or at `apply(...)` with the native exception as the cause. Don't retry silently.
- Behave consistently: the same invalid input must give the same exception type across insert, update and delete.
- Never include secrets (tokens, passwords) in messages.

---

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc) for JUnit 5, AssertJ, naming, `@Nested`, `@DisplayName`, Given/When/Then, soft assertions and PIT. This section only lists what a driver must cover.

Many existing driver tests do not follow the guideline (no `@Nested`, `@DisplayName` or soft assertions). New tests must follow the guideline. Do not copy the older style.

| Area | Requirement |
|---|---|
| Unit tests | Configuration (valid, missing and invalid settings), query/filter/entity converters (every supported condition plus every unsupported one), value objects. No database required. |
| Integration tests | Real database, enabled by `-Djnosql.test.integration=true` through `IntegrationTest` constants in `jnosql-database-commons`. Cover insert, select, update, delete and count, plus every model-specific feature. |
| Testcontainers | Start the database once per JVM in an enum (see `VectorDatabase`, `InfluxDBDatabase`, `ColumnDatabase`, `DocumentDatabase`). Use a pinned image tag. Testcontainers version is managed in the root `pom.xml`. |
| TCK | `mvn test -Ptck -pl jnosql-<db>`. Needs a `ee.jakarta.tck.nosql.TemplateSupplier` service and a supplier class under `src/test/.../tck/` (`MongoDBTemplateSupplier`, `RedisDBTemplateSupplier`). Applicable to key-value and document drivers today. The root [`README.adoc`](../README.adoc) has the TCK instructions (`performing-jakarta-nosql-tck`). |
| Error scenarios | Null and invalid input, unsupported operations, connection failure, and use after close. |
| Resource lifecycle | `close()` releases the native client exactly once. Factory-then-manager ownership is respected. A closed factory rejects new managers when the driver defines that. |

Avoid tests coupled to implementation details (private method names, the exact order of internal calls). Assert observable behavior: the query produced, the entity returned, the exception type.

---

## 9. Common architectural mistakes

| Mistake | Why it hurts | Instead |
|---|---|---|
| Large `if/else` or `switch` chains that grow with every operation | Each new variant edits the same method. | Typed dispatch over a closed enum for simple arms. Polymorphism or Strategy for growing, multi-step behavior. |
| Excessively generic interfaces (`Processor<T,R>`) | Hides meaning and invites misuse. | Role-specific names and signatures. |
| God classes | A manager doing conversion, validation, query building and I/O. | Delegate to converters and schema/validation collaborators. |
| Excessive inheritance | Fragile base classes and hidden coupling. | Composition. Inherit only a stable skeleton. |
| Static utility classes concealing responsibilities | `QueryUtils`-style classes that accumulate unrelated work and cannot be substituted. | Static is fine for pure, cohesive functions (a converter). Split when responsibilities differ. |
| Leaking native objects through standard APIs | Couples callers to the vendor and breaks portability. | Keep native types behind extension interfaces (`MongoDBTemplate`, `CassandraColumnManager`) that are optional and named for the database. |
| Combining conversion, validation, query generation and execution | Untestable without a database. | Separate them and test each in isolation. |
| Duplicated connection logic | Divergent timeouts, auth and TLS handling. | One configuration path to one client factory. |
| Inconsistent exception handling | Callers cannot rely on contracts. | Section 7. |
| Tests coupled to implementation details | Refactors break tests. | Test behavior. |
| Overengineering with unnecessary patterns | More code, no value. | Add a pattern only for a present, concrete variation. |
| Unclear client ownership | Double close or leaks. Examples seen: `RedisBucketManager.close()` closes the shared `UnifiedJedis` that the factory also owns. `DocumentManagerSupplier` in MongoDB closes a manager whose `close()` is a no-op. | Pick one owner (usually the factory) and document it. |

---

## 10. Definition of Done

**API contract**
- [ ] All abstract methods of the contract are implemented. Unsupported ones throw `UnsupportedOperationException` with a clear message.
- [ ] No public API of another module was changed without authorization.
- [ ] No native-client types leak through standard interfaces.

**Semantic design**
- [ ] Class and method names state their role and the database. No `Helper`, `Util` or `Processor` catch-alls.
- [ ] Operation names, return values and exceptions agree.

**SOLID responsibilities**
- [ ] Conversion, query translation, execution and lifecycle are in separate collaborators.
- [ ] Strategy or polymorphism is used only for real variation. Simple guards remain guards.
- [ ] No large growing conditional chains. Existing shared infrastructure is reused.

**Configuration**
- [ ] `<Db>Configurations` enum with documented keys. Required settings are validated eagerly.
- [ ] The service registration is present in `META-INF/services` (and `beans.xml` if there is CDI).

**Resource management**
- [ ] One documented owner of the native client. `close()` is safe to call and releases resources.
- [ ] Errors preserve causes. No secrets are logged.

**Test coverage**
- [ ] Unit tests for configuration, converters and managers, following `TESTING-GUIDELINE.adoc`.
- [ ] Positive, negative and boundary cases are covered. PIT is run where configured.

**Integration testing**
- [ ] Real-database tests gated by `jnosql.test.integration`. Testcontainers image pinned.

**TCK**
- [ ] The Jakarta NoSQL TCK passes where applicable (`mvn test -Ptck -pl jnosql-<db>`), or the non-applicability is stated.

**Documentation**
- [ ] Settings, supported and unsupported operations and the database-specific extensions are documented (see the root [`README.adoc`](../README.adoc)).
- [ ] Any deviation from these guides is explained.

**Build verification**
- [ ] `mvn -pl jnosql-<db> verify` is green, and the module builds as part of the root build.

**Compatibility**
- [ ] UUID handling is decided and tested (native or converted, ids, attributes, query parameters, results). See [UUID Compatibility and Conversion](#uuid-compatibility-and-conversion).
- [ ] Builds on the Java version in the parent POM (currently 21).
- [ ] Existing drivers and their tests are unaffected.

---

## Known inconsistencies in existing documentation

- [`CONTRIBUTING_DATABASE.adoc`](../CONTRIBUTING_DATABASE.adoc) names Java 17 (the build uses 21), uses the package convention `org.jnosql.databases...` (actual: `org.eclipse.jnosql.databases.<db>`), and requires a mapping `Supplier` for each driver (`jnosql-influxdb` and `jnosql-milvus` have none).
- [`README.adoc`](../README.adoc) lists only MongoDB for the TCK, while Redis, Valkey, ArangoDB and Oracle NoSQL also have TCK suppliers.
- The PIT instructions in `TESTING-GUIDELINE.adoc` mention the `jakarta.data.repository` package, which looks copied from another project.
