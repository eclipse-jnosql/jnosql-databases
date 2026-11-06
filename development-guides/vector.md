# Vector Driver Development Guide

Reference implementation: **`jnosql-milvus`** (Milvus through `MilvusClientV2`).
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-milvus/src/main/java/org/eclipse/jnosql/databases/milvus/communication/`. The module has no `mapping` package. It relies on the generic `jnosql-mapping-vector` module.

---

## 1. Purpose and scope

A vector database stores entities that carry an embedding and finds the nearest ones by similarity. The communication implementation:

- connects to the database and owns the native client;
- manages collection schema and index creation (a driver decision, not a framework one);
- converts entities to rows and vectors to the native representation, and back;
- translates filter conditions to the native filter language;
- executes similarity search and returns entities.

## 2. Relevant Jakarta NoSQL contracts

| Type | Package | Role |
|---|---|---|
| `DatabaseConfiguration`, `DatabaseManagerFactory`, `DatabaseManager` | `org.eclipse.jnosql.communication.semistructured` | Standard chain. |
| `VectorManager` | `org.eclipse.jnosql.mapping.vector` | `DatabaseManager` plus `List<CommunicationEntity> search(VectorSelectQuery query)`. |
| `VectorSelectQuery` | same | A `SelectQuery` with `Vector vector()` and `Optional<Float> threshold()`. |
| `Vector`, `DenseVector`, `SparseVector`, `MultiVector` | same | Vector representations. `Vector` is a marker interface. |
| `VectorTemplate`, `VectorSearch` | same | Framework-level API. |

The framework's `VectorManagerSupplier` (CDI, in `org.eclipse.jnosql.mapping.vector.configuration`) reads `jnosql.vector.database`, picks the provider (`jnosql.vector.provider` or the single `DatabaseConfiguration` found), calls the factory, and **requires the returned manager to be a `VectorManager`**. Otherwise it closes the manager and throws `MappingException`. So a vector factory must return a `VectorManager`.

## 3. Reference implementation architecture

| Responsibility | Class |
|---|---|
| Configuration | `MilvusConfiguration` (`get(Map)`, `get(MilvusClientV2)`, `apply(Settings)`) |
| Property keys | `MilvusConfigurations` (host, port, TLS, URI, token, timeout, metric, vector size, consistency, string max length) |
| Settings records | `MilvusConnectionSettings` (default `localhost:19530`), `MilvusCollectionSettings` (metric default COSINE, vector size, consistency default STRONG, max length at most 65,535; validated with `IllegalArgumentException`) |
| Factory | `MilvusVectorManagerFactory` (`apply(db)` creates the database if missing, tolerating a race; `close()` closes the client; returns `VectorManager`) |
| Manager | `MilvusVectorManager` (final, package-private) |
| Schema | `MilvusCollectionSchema` (record: id type, vector field, dimension, fields, kind; `create`, `of(DescribeCollectionResp)`, `validate`) |
| Vector representation | `MilvusVectors` (`Kind` enum: `DENSE`, `SPARSE`, `MULTI`; `kind`, `dimension`, `dataType`, `indexType`, `metricType`, `effectiveMetric`, `indexField`, `toJson`, `toQuery`, `fromNative`) |
| Metrics | `MilvusMetric` (enum `COSINE`, `EUCLIDEAN`, `INNER_PRODUCT`; `parse` accepts the name or `COSINE`/`L2`/`IP`) |
| Threshold | `MilvusThreshold.apply` |
| Row conversion | `MilvusRowConverter` (`vectorElement`, `ensureId`, `toRow`, `toEntity`) |
| Filter conversion | `MilvusFilterConverter.convert(Optional<CriteriaCondition>)` returning `Optional<Filter>` (`expression`, `parameters`) |
| Value conversion | `MilvusValueWriteDecorator` (wraps `CompositeValueWriter(new UUIDValueWriter())`) |
| Service registration | `src/main/resources/META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration` |
| Tests | `MilvusFilterConverterTest`, `MilvusRowConverterTest`, `MilvusCollectionSchemaTest`, `MilvusMetricTest`, `MilvusThresholdTest`, `MilvusSettingsTest`, `MilvusConfigurationTest`, `MilvusConfigurationsTest`, `MilvusValueWriteDecoratorTest`, `MilvusSelectStreamTest`, `MilvusVectorManagerTest`, `MilvusVectorManagerOperationsTest`, `MilvusVectorManagerFactoryTest`, `VectorDatabase` (Testcontainers `MilvusContainer`), `integration/*` |

Tests are under `jnosql-milvus/src/test/java/org/eclipse/jnosql/databases/milvus/` (`communication/` and `integration/`). Unlike most other drivers, the Milvus tests already use `@Nested` and `@DisplayName`.

## 4. Semantic code analysis

**Separation of responsibilities**

| Concern | Class | Notes |
|---|---|---|
| Vector conversion | `MilvusVectors`, `MilvusRowConverter` | Pure static functions. `MilvusVectors` is a single place that answers "what does `DENSE` / `SPARSE` / `MULTI` mean for Milvus". |
| Filter conversion | `MilvusFilterConverter` | Instance-per-conversion (private constructor, collects parameters), returns a `Filter` record. Field names must match `[A-Za-z_][A-Za-z0-9_]*`. Values are bound as template parameters (`{p0}`). |
| Schema management | `MilvusCollectionSchema` | Creates the schema from the first entity, reads it back from `DescribeCollectionResp`, and validates later writes against it. |
| Search execution | `MilvusVectorManager.search` | Validates, builds the `SearchReq`, executes, converts. |
| Settings | `MilvusConnectionSettings`, `MilvusCollectionSettings` | Immutable records that validate on construction. |

**Capability validation is explicit.** `search` checks, in order: positive limit, supported vector kind (`MilvusVectors.kind` throws `UnsupportedOperationException` for an unknown `Vector` type), that the collection's stored kind equals the query kind, and that dimensions match (not for `SPARSE`). A mismatch is an `IllegalArgumentException` naming the collection. An unknown or unsupported capability is `UnsupportedOperationException`.

**Vector kinds.** Verified in `MilvusVectors`:

| Kind | Native data type | Index type | Metric |
|---|---|---|---|
| `DENSE` | `FloatVector` | `AUTOINDEX` | configured metric |
| `SPARSE` | `SparseFloatVector` | `SPARSE_INVERTED_INDEX` | always inner product (`effectiveMetric`) |
| `MULTI` | array of structs holding `FloatVector` | `HNSW` | `MAX_SIM_*` variant of the configured metric |

Do not generalise this table to other vector databases. A provider may lack sparse or multi-vector support, or use other metrics and index types. Check the native client.

**Similarity metrics.** `MilvusMetric` maps `COSINE`, `EUCLIDEAN` (L2) and `INNER_PRODUCT` (IP). `MilvusThreshold.apply` sets a `radius` search parameter. For `EUCLIDEAN` it requires a non-negative threshold and squares it. Treat threshold semantics per metric as a documented contract.

**Resource lifecycle.** The factory owns the `MilvusClientV2`. The manager's `close()` is empty. `select` builds a custom `RowSpliterator` and ties its cleanup to `Stream.onClose`, so callers must close the stream (try-with-resources).

**Error handling.** Missing collections give empty results for `select`, `delete` and `search` (not exceptions). `update` of a missing row silently returns. Both choices should be a conscious, documented decision. TTL overloads throw `UnsupportedOperationException`. `select` with sorts throws `UnsupportedOperationException`. Collection creation tolerates a race by re-checking `exists` and rethrowing otherwise.

**Observations (do not copy blindly)**

- `MilvusVectorManager` is cohesive but large (upsert, create-collection, iteration, search, load management). New features should go into collaborators.
- `insert` is an upsert. `update` only upserts when the row exists. Document such differences.
- `count(SelectQuery)` falls back to `VectorManager.super.count` when skip or limit is set. That is correct but potentially expensive. Note it.
- With no filter, the expression is `_id >= {all}`. This ties unfiltered queries to the id field type.
- `MilvusRowConverter.toEntity` has a convenience overload that defaults to `DENSE`. Prefer passing the kind explicitly.

## 5. SOLID considerations

- **SRP.** Preserve the split into vectors, rows, filters, schema, metrics and threshold. Add new behavior (say, a new index option) to a collaborator, not to `MilvusVectorManager`.
- **OCP.** Adding a vector kind touches `MilvusVectors` (every `switch` on `Kind` is exhaustive, so the compiler flags the missing arm) and the schema code. Keep that set small and typed.
- **LSP.** A `VectorManager` is also a `DatabaseManager`. CRUD must keep its meaning. Unsupported sort, TTL and missing-schema behavior must be consistent and documented.
- **ISP.** `search` is the only addition on `VectorManager`. Native-only operations (index tuning, partitions) belong on a database-named interface.
- **DIP.** The configuration accepts a ready `MilvusClientV2` (`get(MilvusClientV2)`), so tests substitute the client. The manager depends on settings records, not on raw `Settings`.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Vector representation (dense, sparse, multi) | Today a closed `Kind` enum with exhaustive `switch` expressions in `MilvusVectors`. This is typed dispatch and fits while three kinds exist. If each kind accumulates more behavior (index options, validation, JSON shape), move to a small `VectorKindStrategy` interface with one implementation per kind, held in an `EnumMap`. Do it when a fourth kind or kind-specific options appear, not before. |
| Similarity metric | `MilvusMetric` is an enum with behavior (`toMetricType`, `parse`). Sufficient. |
| Threshold semantics per metric | One `switch`-like branch in `MilvusThreshold`. Extract per-metric rules only if more metrics need transformations. |
| Filter translation | `switch` over `Condition` with short arms. Keep. |
| Provider capabilities | Validate explicitly (see section 4). Do not use `instanceof` on vendor types in the generic path. |

Do not replace simple guards (positive limit, `BETWEEN` size, `IN` non-empty, field-name regex) with strategy classes.

## 7. Step-by-step driver implementation (fictional `AcmeVec`)

```
jnosql-acmevec/
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmevec/communication/
    │   │   ├── AcmeVecConfiguration.java
    │   │   ├── AcmeVecConfigurations.java
    │   │   ├── AcmeVecConnectionSettings.java      # record, validates
    │   │   ├── AcmeVecCollectionSettings.java      # record: metric, dimension
    │   │   ├── AcmeVecVectorManagerFactory.java
    │   │   ├── AcmeVecVectorManager.java           # implements VectorManager
    │   │   ├── AcmeVecMetric.java                  # enum
    │   │   ├── AcmeVecVectors.java                 # vector kind support
    │   │   ├── AcmeVecRowConverter.java
    │   │   ├── AcmeVecFilterConverter.java
    │   │   └── AcmeVecCollectionSchema.java
    │   └── resources/META-INF/services/
    │       └── org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration
    └── test/java/...
```

1. **List the capabilities the native client really has**: vector kinds, metrics, filter operators, index types, dimension limits. Anything absent is `UnsupportedOperationException`.
2. **Settings records** validate eagerly (`IllegalArgumentException`).
3. **Metric enum**:

   ```java
   enum AcmeVecMetric {
       COSINE(AcmeMetric.COSINE), EUCLIDEAN(AcmeMetric.L2);
       private final AcmeMetric nativeMetric;
       AcmeVecMetric(AcmeMetric nativeMetric) { this.nativeMetric = nativeMetric; }
       AcmeMetric toNative() { return nativeMetric; }
   }
   ```

4. **Vector support**: exhaustive `switch` over the supported `Vector` types, with a `default` that throws.

   ```java
   static float[] values(Vector vector) {
       return switch (vector) {
           case DenseVector dense -> dense.values();
           default -> throw new UnsupportedOperationException(
                   "AcmeVec does not support the vector type: " + vector.getClass().getName());
       };
   }
   ```

5. **Filter converter**: pure, returns a record with the expression and parameters. Validate field names. Reject unsupported conditions.
6. **Row converter**: entity to native row and back. Decide the id strategy (generate when missing) and unit-test it.
7. **Schema**: create from the first entity or from configuration. Validate later writes (kind, dimension, id type, field types) and fail with a message naming the collection.
8. **Manager**: `implements VectorManager`. Implement the `DatabaseManager` methods and `search(VectorSelectQuery)`. Validate in this order: limit, vector kind, schema compatibility, dimension. Throw for unsupported TTL and sort.
9. **Factory**: creates the database or namespace if missing, returns a `VectorManager`, and owns the client.
10. Register the service file. Document `jnosql.vector.database` and `jnosql.vector.provider` for users.

### UUID and API Documentation

- `jnosql-milvus` stores UUIDs as text. `MilvusValueWriteDecorator` composes `UUIDValueWriter`, `MilvusRowConverter.toJson` writes a `UUID` as a string, `MilvusRowConverter.ensureId` generates a UUID string when `_id` is missing, and `MilvusFilterConverter` normalizes `UUID` to `String` in filters. Non-integral ids use a `VarChar` primary key in `MilvusCollectionSchema.create`.
- The id comes back as text. `Element.get(UUID.class)` restores the `UUID` through `UUIDValueReader`.
- Existing coverage: `MilvusValueWriteDecoratorTest`, `MilvusRowConverterTest`, `MilvusFilterConverterTest`. Add integration tests for a UUID id and a UUID attribute, retrieved by id and used in a `search` filter.
- A vector database that restricts ids (for example to integers or UUIDs) must keep the mapping in one class and keep the original id recoverable.

Rules and tests are in [UUID Compatibility and Conversion](common.md#uuid-compatibility-and-conversion). Any new public API must follow [`javadoc.md`](javadoc.md): document public interfaces, add a short example to important methods, and state null, empty-result and exception behavior.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). Use the Milvus tests (`@Nested`, `@DisplayName`) as the closest style example, and then check them against the guideline.

Unit tests (no database):

- Filter converter: each supported condition, nested AND/OR/NOT, null equality, `BETWEEN` wrong size, `IN` empty, invalid field names, non-comparable range values, unsupported conditions.
- Vector support: each kind converts to JSON/native and back (round trip), unknown `Vector` type throws `UnsupportedOperationException`, empty sparse vector, dimension zero.
- Row converter: exactly one vector element (none and two are negative), id generation, nested values, null values.
- Schema: creation, `of(description)` for a collection not created by JNoSQL (`IllegalStateException`), validation failures (kind, dimension, id type, field type).
- Metric and threshold: parsing and aliases, unknown name, negative Euclidean threshold, the transformation applied.
- Settings and configuration: defaults and invalid values (non-positive vector size, over-long max length).
- Manager with a mocked client: limit validation, kind mismatch, dimension mismatch, TTL and sort unsupported, missing collection behavior.

Integration tests (Testcontainers through a `VectorDatabase`-style enum; `-Djnosql.test.integration=true`):

- Upsert then `search` for every supported vector kind. Top-k ordering. Limit and skip.
- Threshold behavior per metric.
- Filter plus search. Filter-only `select` and `count`.
- Dimension and kind mismatch against an existing collection.
- Missing collection: empty results.
- Lifecycle: stream closing, factory closes the client once.

TCK: not wired for vector drivers in this repository. State that in the Definition of Done.

## 9. Common pitfalls

- Assuming every provider supports dense, sparse and multi-vectors and every metric.
- Accepting a vector of the wrong kind or dimension and failing late inside the native client.
- Treating similarity scores and thresholds as interchangeable across metrics. Euclidean (distance, smaller is closer) differs from cosine and inner product.
- Mixing schema creation, row conversion, filter translation and search in one method.
- Concatenating filter values into the expression text instead of binding parameters.
- Unvalidated field names in filter expressions.
- Silent schema inference that hides conflicts between entities of the same collection.
- Swallowing exceptions when creating a collection. Re-check state and rethrow the original.
- Unclosed result streams that hold native resources.
- Unlimited or zero-limit searches.
- Exposing native vector or request types through `VectorManager` methods.
- Duplicate connection logic across configuration entry points.

## 10. Definition of Done

- [ ] `DatabaseConfiguration`, factory (returning a `VectorManager`) and manager are implemented and registered.
- [ ] Supported vector kinds and metrics are listed, validated explicitly, and anything else throws `UnsupportedOperationException`.
- [ ] Schema, row, vector and filter conversion are separate, pure and unit-tested.
- [ ] `search` validates limit, kind and dimension, and documents threshold semantics per metric.
- [ ] Filters bind parameters, validate field names, and reject unsupported conditions.
- [ ] Missing-collection, update-missing-row, sort and TTL behaviors are documented and tested.
- [ ] One owner closes the native client. Streams release resources. Lifecycle is tested.
- [ ] Unit and integration tests follow `TESTING-GUIDELINE.adoc`. A pinned image is used.
- [ ] TCK status is stated. `mvn -pl jnosql-<db> verify` passes. Other modules are unchanged.
- [ ] Settings and features are documented in the root [`README.adoc`](../README.adoc).
