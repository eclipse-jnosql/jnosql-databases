# Time-Series Driver Development Guide

Reference implementation: **`jnosql-influxdb`** (InfluxDB 3 through the `influxdb3-java` client).
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-influxdb/src/main/java/org/eclipse/jnosql/databases/influxdb/communication/`. The module has no `mapping` package.

---

## 1. Purpose and scope

A time-series database stores measurements ordered by time. The communication implementation:

- connects to the database;
- writes a `CommunicationEntity` as a timestamped point;
- translates conditions, sort and limit into the database's query language;
- converts rows back into `CommunicationEntity`.

The driver is **append-oriented**. In the reference, `update`, `delete` and per-point TTL are unsupported and throw `UnsupportedOperationException`. Do not invent mutation semantics the database does not provide.

## 2. Relevant Jakarta NoSQL contracts

| Type | Package | Role |
|---|---|---|
| `DatabaseConfiguration`, `DatabaseManagerFactory`, `DatabaseManager` | `org.eclipse.jnosql.communication.semistructured` | The driver contract (same chain as document and column). |
| `CommunicationEntity`, `Element`, `SelectQuery`, `CriteriaCondition`, `Sort` | same / `org.eclipse.jnosql.communication` | Data and query model. |
| `TimeSeriesTemplate`, `TimeSeriesQuery` | `org.eclipse.jnosql.mapping.timeseries` (module `jnosql-mapping-timeseries`) | Framework-level API. |

**Where temporal operations live.** `TimeSeriesQuery` offers `latest(int)`, `earliest(int)` (`Chronological`) and `since(Temporal)`, `until(Temporal)`, `last(Duration)` (`Interval`). These are implemented by the framework (`DefaultTimeSeriesQuery`), **not** by the driver. They become ordinary `SelectQuery` data: conditions on the `@Id` field plus sort and limit. The driver only has to execute those correctly. Verified contracts:

| Operation | Becomes | Notes |
|---|---|---|
| `since(t)` | `_id >= t` | Inclusive lower bound. |
| `until(t)` | `_id <= t` | Inclusive upper bound. |
| `last(d)` | `[now - d, now]` | Uses a clock. Needs an `Instant`, `OffsetDateTime` or `ZonedDateTime` id. |
| `latest(n)` / `earliest(n)` | sort by id descending / ascending, with limit `n` | `n` must be positive. Cannot combine with a prior limit. |

The framework throws `IllegalArgumentException` when `since` is after `until`, and `IllegalStateException` for a duplicated `since`/`until`/`last` or for `last` combined with `since`/`until`. The supported id types in the mapping layer are `Instant`, `LocalDateTime`, `LocalDate`, `OffsetDateTime` and `ZonedDateTime`.

## 3. Reference implementation architecture

| Responsibility | Class |
|---|---|
| Configuration | `InfluxDBTimeSeriesConfiguration` (`get(Map)`, `apply(Settings)`; requires non-blank URL and token, otherwise `IllegalArgumentException`) |
| Property keys | `InfluxDBTimeSeriesConfigurations` (`jnosql.influxdb.url`, `jnosql.influxdb.token`) |
| Factory | `InfluxDBTimeSeriesManagerFactory` (synchronised; tracks every client it creates; `apply` after `close` throws `IllegalStateException`; `close()` aggregates failures with suppressed causes and zeroes the token `char[]`) |
| Manager | `InfluxDBTimeSeriesManager` (package-private; receives an `InfluxDBClient` and the database name) |
| Entity conversion | `InfluxDBEntityConverter` (`toPoint`, `toEntity`, `toInstant`) |
| Query translation | `InfluxDBQueryConverter` (`convert`, `count`; returns the record `InfluxDBQuery(statement, parameters)`) |
| Service registration | `src/main/resources/META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration` |
| Tests | `InfluxDBQueryConverterTest`, `InfluxDBEntityConverterTest`, `InfluxDBTimeSeriesConfigurationTest`, `InfluxDBTimeSeriesManagerTest`, `InfluxDBTimeSeriesManagerIntegrationTest` (`@EnabledIfSystemProperty`), `InfluxDBDatabase` (Testcontainers `GenericContainer`, image `influxdb:3.11.0-core`), `integration/*` |

Under `src/test/java/org/eclipse/jnosql/databases/influxdb/`.

## 4. Semantic code analysis

**Separation.** Translation (`InfluxDBQueryConverter`) and conversion (`InfluxDBEntityConverter`) are pure, package-private classes with private constructors. They never touch the client. The manager is thin: it converts, calls `client.writePoint(s)` or `client.queryRows(sql, parameters)`, and converts results. This is the recommended shape.

**Temporal entity identity.** The `_id` element is the timestamp. `defaultIdFieldName()` returns `Optional.of("_id")`. `toPoint` requires `_id` and throws `IllegalArgumentException` otherwise. In queries, the `_id` column maps to `time`, and `_id` values are normalized through `toInstant(...).toString()`.

**Timestamp precision.** `toEntity` reads `time` as an `Instant`, or as an integer nanosecond count (including `BigInteger`), converting with `divideAndRemainder` and a range check. A value outside the supported range throws `IllegalArgumentException`. Negative remainders are normalized so pre-1970 values are correct. New drivers must state the native precision (nanoseconds here) and test it.

**Timezone semantics.** `toInstant` accepts `Instant`, `LocalDateTime` (**interpreted as UTC**), `OffsetDateTime` and `ZonedDateTime`. Results always come back as `Instant`. Any local-time handling is therefore a documented convention, not a database feature. Never use the JVM default zone silently.

**Gap to note.** The mapping layer allows `LocalDate` ids, but `toInstant` rejects `LocalDate` with `IllegalArgumentException`. Document and test unsupported id types rather than letting them surface as late failures.

**Query translation.** `condition` is a `switch` over the closed `Condition` enum (EQUALS, GREATER_THAN, GREATER_EQUALS_THAN, LESSER_THAN, LESSER_EQUALS_THAN, IN, BETWEEN, NOT, AND, OR). Values are parameters (`$p0`, `$p1`, …), identifiers are quoted by doubling quotes, and invalid shapes fail early: `IN` needs at least one value, `BETWEEN` exactly two, `AND`/`OR` at least one child, null comparison values are rejected, and `OFFSET` without `LIMIT` throws `UnsupportedOperationException`. `count` uses the alias `jnosql_count` and ignores sort and limit.

**Resource lifecycle.** The factory owns clients. The manager's `close()` is deliberately empty (documented in Javadoc). Close the factory to release resources.

**Observations (do not copy blindly)**

- The projection always includes `time`.
- Entity fields are written as fields only. There is no tag model. A driver for a database with tags needs a deliberate mapping rule.
- The manager's `count(SelectQuery)` throws `IllegalArgumentException` for a non-numeric result.

## 5. SOLID considerations

- **SRP.** Keep identity/timestamp rules in the entity converter, SQL (or native query) generation in the query converter, I/O in the manager, and client ownership in the factory.
- **OCP.** New value types become new arms in `addField`. New conditions become new arms in `condition`. Both are short and typed.
- **LSP.** Unsupported mutations throw `UnsupportedOperationException` consistently (`update`, `update(UpdateQuery)`, `delete`, TTL overloads). Callers can treat the driver as an append-only `DatabaseManager`.
- **ISP.** Do not add native query methods to the standard contract. Put them on a database-named interface if needed.
- **DIP.** The manager receives the `InfluxDBClient`. Tests can inject a mock (`InfluxDBTimeSeriesManagerTest`) or a Testcontainers client.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Identifier temporal types | A `switch` with pattern matching, as in `toInstant`. Sufficient while the set is closed. |
| Condition translation | Same typed dispatch. Do not replace `IN`/`BETWEEN` size checks with strategy classes. |
| Precision (ns, µs, ms) | A single value (an enum or a configuration setting) consulted by the converter. Introduce a precision strategy only if one driver instance must serve several precisions at once. |
| Native query dialect (SQL vs a native language) | If a database offers more than one, a `QueryTranslator` interface with one implementation per dialect is justified. A single dialect does not need it. |

Temporal convenience queries (`since`, `until`, `last`, `latest`, `earliest`) are not variation points in the driver. They are already reduced to conditions, sort and limit.

## 7. Step-by-step driver implementation (fictional `AcmeTS`)

```
jnosql-acmets/
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmets/communication/
    │   │   ├── AcmeTSTimeSeriesConfiguration.java
    │   │   ├── AcmeTSTimeSeriesConfigurations.java
    │   │   ├── AcmeTSTimeSeriesManagerFactory.java
    │   │   ├── AcmeTSTimeSeriesManager.java
    │   │   ├── AcmeTSEntityConverter.java      # entity <-> point (pure)
    │   │   └── AcmeTSQueryConverter.java       # SelectQuery -> native query (pure)
    │   └── resources/META-INF/services/
    │       └── org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration
    └── test/java/...
```

1. Depend on `jnosql-mapping-timeseries` and `jnosql-database-commons`.
2. **Entity converter**: decide the timestamp source (`_id`), accepted temporal types, UTC convention for local types, and field type rules.

   ```java
   static Instant toInstant(Object value) {
       Object converted = value instanceof Value jnosqlValue ? ValueUtil.convert(jnosqlValue) : value;
       return switch (converted) {
           case Instant instant -> instant;
           case OffsetDateTime offset -> offset.toInstant();
           case ZonedDateTime zoned -> zoned.toInstant();
           case null -> throw new IllegalArgumentException("AcmeTS identifier cannot be null");
           default -> throw new IllegalArgumentException("Unsupported AcmeTS identifier: " + converted.getClass());
       };
   }
   ```

3. **Query converter**: return a small record (`statement`, `parameters`). Map `_id` to the time column. Parameterise every value. Reject unsupported shapes early.
4. **Manager**: `insert` writes points. Throw `UnsupportedOperationException` for whatever the database lacks. `select` runs the converted query and maps rows. `count` closes its row stream (try-with-resources).
5. **Factory**: owns clients, rejects use after `close`, aggregates close failures.
6. **Configuration**: validate required settings eagerly. Never log tokens.
7. Register the service file. No `mapping` package is needed unless you add a database-named template.

### UUID and API Documentation

- In `jnosql-influxdb` the identifier is the point time. `InfluxDBEntityConverter.toInstant` accepts `Instant`, `LocalDateTime`, `OffsetDateTime` and `ZonedDateTime`, and rejects everything else (including `UUID`) with `IllegalArgumentException`. A UUID cannot identify a point. Document it.
- `addField` accepts strings, numbers, booleans and characters. A `UUID` attribute therefore fails with `IllegalArgumentException`. If you want to support it, convert explicitly (for example `UUIDValueWriter`), read it back through `UUIDValueReader`, and test the round trip. Otherwise state the limitation. Do not write a test for unsupported behavior other than the documented rejection.
- Do not invent a series identity from a UUID. Series identity is measurement, tags and time.

Rules and tests are in [UUID Compatibility and Conversion](common.md#uuid-compatibility-and-conversion). Any new public API must follow [`javadoc.md`](javadoc.md): document public interfaces, add a short example to important methods, and state null, empty-result and exception behavior.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). The existing InfluxDB tests do not use `@Nested`, `@DisplayName` or soft assertions. New tests must.

Unit tests:

- Query converter: every condition, nested AND/OR/NOT, `IN` empty (negative), `BETWEEN` with a wrong size, null comparison value, `_id` mapped to `time`, identifier quoting (embedded double quotes), `OFFSET` without `LIMIT`, `count`.
- Entity converter: each accepted id type, `LocalDateTime` treated as UTC, null id, unsupported id (including `LocalDate`), missing `_id`, no fields, null field, unsupported field type, nanosecond timestamps, pre-1970 timestamps, out-of-range timestamps.
- Configuration: missing and blank URL or token.
- Factory: `apply` after `close`, aggregated close failures, token cleared.

Integration tests (Testcontainers through an `InfluxDBDatabase`-style enum; `-Djnosql.test.integration=true`):

- Write a series, then read it with `since`, `until`, `last`, `latest` and `earliest` through the framework `TimeSeriesTemplate`.
- Time boundaries: a point exactly at `since` and exactly at `until` is included (both are inclusive). A point one nanosecond outside is excluded.
- Precision: sub-millisecond timestamps round-trip.
- Ordering: results with an explicit sort, and `latest`/`earliest` with ties on identical timestamps. Do not assume an order without a sort.
- Empty results: an empty range and a missing measurement return an empty stream, not an error (verify the database's behavior and document it).
- Timezones: the same instant written as `Instant`, `OffsetDateTime` and `ZonedDateTime` is identical.
- Unsupported operations: `update`, `delete` and TTL throw `UnsupportedOperationException`.

TCK: not wired for time-series drivers in this repository. State that in the Definition of Done.

## 9. Common pitfalls

- Reimplementing `latest`/`since`/`last` in the driver. The framework already reduces them to conditions.
- Using the JVM default time zone for `LocalDateTime`.
- Truncating precision silently (millisecond casts).
- Assuming a result order when no sort was requested.
- Treating an inclusive boundary as exclusive, or the reverse.
- Concatenating values or identifiers into the query text instead of parameters and quoting.
- Allowing `OFFSET` without `LIMIT` when the database cannot do it.
- Pretending `update`/`delete` work on an append-only store.
- Accepting an id type in the mapping layer that the converter rejects (`LocalDate`) without documentation.
- Combining conversion, translation and I/O in the manager.
- Logging tokens, or leaving the token in memory after close.

## 10. Definition of Done

- [ ] `DatabaseConfiguration`, factory and manager are implemented and registered.
- [ ] Entity and query converters are pure and unit-tested, with every `Condition` translated or rejected with `UnsupportedOperationException`.
- [ ] The identity, accepted id types, time zone convention and precision are documented and tested.
- [ ] Inclusive boundaries, ordering, limit and empty-result behavior are verified against the real database.
- [ ] Unsupported mutations and TTL fail consistently.
- [ ] Queries are parameterised and identifiers quoted.
- [ ] One owner closes the native client. Use-after-close and close failures are tested. Secrets are not logged.
- [ ] Unit and integration tests follow `TESTING-GUIDELINE.adoc`. A pinned image is used.
- [ ] TCK status is stated. `mvn -pl jnosql-<db> verify` passes. Other modules are unchanged.
- [ ] Settings and features are documented in the root [`README.adoc`](../README.adoc).
