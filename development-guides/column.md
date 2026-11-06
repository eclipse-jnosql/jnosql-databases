# Column Driver Development Guide

Reference implementation: **`jnosql-cassandra`**.
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-cassandra/src/main/java/org/eclipse/jnosql/databases/cassandra/` (`communication/` and `mapping/`).

---

## 1. Purpose and scope

A column (wide-column) database stores rows under a table (column family), addressed by a partition key and optional clustering columns.
The communication implementation:

- opens sessions to the cluster;
- translates `SelectQuery`, `DeleteQuery` and `CommunicationEntity` into native statements;
- executes them and converts native rows back to `CommunicationEntity`.

Column stores are strict about which conditions are legal: filtering on non-key columns is usually restricted. The driver must not pretend otherwise.

## 2. Relevant Jakarta NoSQL contracts

Package `org.eclipse.jnosql.communication.semistructured` (module `jnosql-communication-semistructured`, repository `eclipse-jnosql/jnosql`):

| Type | Role |
|---|---|
| `DatabaseConfiguration` | `Function<Settings, DatabaseManagerFactory>`; `ServiceLoader` entry point. |
| `DatabaseManagerFactory` | `Function<String, DatabaseManager>` and `AutoCloseable`. The name is the keyspace for Cassandra. |
| `DatabaseManager` | `insert` (with TTL variants), `update`, `delete(DeleteQuery)`, `select(SelectQuery)`, `count`, `close`. Defaults: `exists`, `singleResult`, `selectCursor`, `query`/`prepare`, `count(SelectQuery)`. |
| `CommunicationEntity`, `Element` | Row and column abstractions. |
| `SelectQuery`, `DeleteQuery`, `UpdateQuery`, `CriteriaCondition` | Query representation. `CriteriaCondition.condition()` is an `org.eclipse.jnosql.communication.Condition` enum. |

The framework owns entity-to-`CommunicationEntity` mapping, templates and repositories (`jnosql-mapping-column`, including `ColumnManagerSupplier`-style CDI wiring). The driver owns only the native interaction.

## 3. Reference implementation architecture

| Responsibility | Class (under `communication/` unless noted) |
|---|---|
| Configuration | `CassandraConfiguration` (filters `jnosql.*` keys and uses `CassandraProperties.of(...)`) |
| Property keys | `CassandraConfigurations` (user, password, host, name, port, query, data center) |
| Connection builder | `CassandraProperties` (`createCluster()` returns a `CqlSessionBuilder`; defaults: port 9042, data center `datacenter1`) |
| Factory | `CassandraColumnManagerFactory` (runs the startup `QUERY` statements, then builds a `CqlSession` per `apply(keyspace)`) |
| Manager API | `CassandraColumnManager` (public interface extending `DatabaseManager`) |
| Manager | `DefaultCassandraColumnManager` (package-private) |
| Query representation | `CassandraQuery` (a decorator over `SelectQuery` that carries the paging state) |
| Query conversion | `QueryUtils` (select, insert, update; identifier quoting; UDT handling), `Relations` (conditions to CQL relations), `DeleteQueryConverter` |
| Query execution | `QueryExecutor` (interface) and `QueryExecutorType` (enum: `PAGING_STATE`, `DEFAULT`) |
| Result conversion | `CassandraConverter.toDocumentEntity(Row)` |
| UDT model | `UDT`, `DefaultUDT`, `IterableUDT`, `UDTBuilder` and related builders |
| Mapping | `mapping/ColumnManagerSupplier`, `mapping/CassandraColumnEntityConverter`, `mapping/CassandraTemplate`, `mapping/DefaultCassandraTemplate`, `mapping/CassandraExtension` (CDI `Extension`), `mapping/CassandraRepository`, `mapping/CQL` |
| Services | `META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration`, `META-INF/services/jakarta.enterprise.inject.spi.Extension` |
| Tests | `CassandraColumnManagerTest`, `CassandraColumnManagerFactoryTest`, `CassandraConfigurationTest`, `QueryUtilsSecurityTest`, `DefaultUDTTest`, `ColumnDatabase` (Testcontainers `CassandraContainer`) |

`CassandraColumnManager` adds `save(...)` overloads with `ConsistencyLevel`, `delete`/`select`/`count` with `ConsistencyLevel`, `cql(...)`, `execute(SimpleStatement)` and `nativeQueryPrepare(...)`. These extras are native-typed, so they live on a database-named interface and not on the standard contract.

## 4. Semantic code analysis

**Separation of the four concerns**

| Concern | Where | Comment |
|---|---|---|
| Query representation | `SelectQuery`, `DeleteQuery`, `CassandraQuery` | `CassandraQuery` is mutable (paging state, exhausted flag). That is a deliberate stateful cursor, but it is not thread-safe by design. |
| Query conversion | `QueryUtils.select`, `Relations.createClause`, `DeleteQueryConverter.delete` | Pure: no session needed for `select` or `delete`. Easy to unit-test (see `QueryUtilsSecurityTest`). `QueryUtils.insert` and `update` take a `CqlSession` because UDT types must be resolved. |
| Query execution | `QueryExecutorType.execute` / `count`, `DefaultCassandraColumnManager` | The manager picks the executor with `QueryExecutor.of(query)`. |
| Result conversion | `CassandraConverter` | Skips null columns, so absent and null look the same to callers. |

**Reusable design decisions**

- `QueryExecutor.of(SelectQuery)` selects the execution mode from the query type. This is existing polymorphism (an interface with an enum implementation). A new driver with a similar split should reuse the idea. Do not add another Strategy layer on top.
- Identifiers are quoted through `CqlIdentifier`, and `QueryUtilsSecurityTest` covers injection payloads. Any driver building text queries should do the same.
- `CassandraProperties` isolates builder construction from `CassandraConfiguration`.

**Observations (do not copy blindly)**

- `QueryUtils` is a large static class mixing select, insert, update and UDT work. It is cohesive around "CQL building", but it is where new responsibilities pile up. Prefer several focused converters.
- `Relations.load` is a `switch` over `Condition`. EQUALS, GT, GTE, LT, LTE, IN, LIKE and AND are translated. Others (OR, NOT, …) throw `UnsupportedOperationException`. This is acceptable typed dispatch with one-line arms.
- `QueryUtils.select` collects sorts with `Collectors.toMap`, so duplicate sort properties throw `IllegalStateException`.
- TTL is narrowed with `(int) duration.getSeconds()`. Validate the range.
- `CassandraColumnManagerFactory.close()` does nothing, so each manager owns its session. Document the ownership if you copy this.
- `CassandraQuery.toString()` returns the text `CouchDBDocumentQuery`, a leftover from copy-paste.

## 5. SOLID considerations

- **SRP.** Keep statement building, execution and row conversion apart, as the reference does. Add new CQL features in a new focused class rather than in `DefaultCassandraColumnManager`.
- **OCP.** A new execution mode becomes a new `QueryExecutor` implementation. New condition support is one new arm in the translator.
- **LSP.** `update(entity)` is an upsert in Cassandra (it delegates to `insert`). Document it. Callers expecting an error for a missing row will be surprised.
- **ISP.** Consistency-level and raw-CQL operations stay on `CassandraColumnManager`, not on `DatabaseManager`.
- **DIP.** The manager depends on `CqlSession`, which the factory injects. For testing, build managers from a Testcontainers session.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Offset vs paging-state execution | Already solved by `QueryExecutor` and `QueryExecutorType`. Reuse it. Do not propose a new strategy. |
| Condition translation (`Relations.load`) | Keep the `switch`. Extract per-condition translators only if an arm needs several steps (for example `LIKE` becoming a SASI or SAI-specific clause) or per-version capability checks. |
| Consistency level | A parameter, not a strategy. |
| UDT building | Builders (`UDTBuilder`) fit the many-optional-parts case. |

Do not replace argument checks (`requireNonNull`, empty-collection checks) with strategies.

## 7. Step-by-step driver implementation (fictional `AcmeDB` column store)

```
jnosql-acmedb/
└── src/main/java/org/eclipse/jnosql/databases/acmedb/communication/
    ├── AcmeDBConfiguration.java            # DatabaseConfiguration
    ├── AcmeDBConfigurations.java           # property keys
    ├── AcmeDBColumnManagerFactory.java     # DatabaseManagerFactory
    ├── AcmeDBColumnManager.java            # DatabaseManager
    ├── AcmeDBStatementBuilder.java         # SelectQuery/DeleteQuery -> native statement (pure)
    ├── AcmeDBRowConverter.java             # native row -> CommunicationEntity (pure)
    └── AcmeDBQueryRunner.java              # executes statements
```

1. **Model the key rules first.** Decide which columns form the partition key and which conditions the database allows. Write them as validation in the statement builder, and test them before touching the database.
2. **Statement builder** (pure):

   ```java
   final class AcmeDBStatementBuilder {
       AcmeStatement select(SelectQuery query, String keyspace) {
           Objects.requireNonNull(query, "query is required");
           // translate columns, condition, sorts and limit; throw
           // UnsupportedOperationException for conditions AcmeDB cannot express
       }
   }
   ```

3. **Condition translation**: a `switch` over `Condition` with a `default` branch that throws.

   ```java
   private AcmeRelation relation(CriteriaCondition criteria) {
       return switch (criteria.condition()) {
           case EQUALS -> AcmeRelation.eq(name(criteria), value(criteria));
           case GREATER_THAN -> AcmeRelation.gt(name(criteria), value(criteria));
           case IN -> AcmeRelation.in(name(criteria), values(criteria));
           default -> throw new UnsupportedOperationException(
                   "AcmeDB does not support the condition " + criteria.condition());
       };
   }
   ```

4. **Row converter** maps native rows to `CommunicationEntity.of(table, elements)` and decides how nulls are represented.
5. **Manager** composes builder, runner and converter. It implements `insert`, `update`, `delete`, `select`, `count`, `close`. Throw for unsupported TTL.
6. **Factory** receives the native session or client, returns a manager per keyspace, and closes what it created. Be explicit about who closes sessions.
7. **Optional native extensions** (raw queries, consistency) go on `AcmeDBColumnManager extends DatabaseManager`.
8. **Mapping** only if you need a CDI `Extension` or a database-specific template (`CassandraExtension`, `CassandraTemplate`). The generic column mapping handles the rest.
9. Register `META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration`.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). The existing Cassandra tests do not use `@Nested`, `@DisplayName` or soft assertions. New tests must.

Unit tests (no database):

- Statement builder: each supported condition (positive), each unsupported condition (negative, expecting `UnsupportedOperationException`), identifier quoting and injection payloads (see `QueryUtilsSecurityTest`), sort with duplicate property, limit zero, skip without limit (boundary).
- Row converter: scalar types, collections, UDTs, null columns, empty rows.
- Configuration: defaults (port, data center), missing and invalid settings.

Integration tests (Testcontainers through a `ColumnDatabase`-style enum, `-Djnosql.test.integration=true`):

- Insert then select by partition key. Select by partition key plus clustering range.
- Upsert behavior of `update`. Delete by key. Count.
- TTL expiry. Paging across more rows than the page size.
- Invalid queries: filtering on a non-key column without support, unknown column or table. Assert that a clear native failure surfaces with its cause.
- Null and identity: a null column value, a missing partition key (must be rejected), duplicate primary key (upsert semantics).

TCK: Cassandra has no TCK supplier in this repository. Add one only if the Jakarta NoSQL TCK supports the model and the database. State the result in the Definition of Done.

## 9. Common pitfalls

- Letting the manager build CQL strings, execute them and convert rows in one method.
- String concatenation of identifiers or values (injection). Quote identifiers and bind values.
- Assuming every `Condition` is supported. OR, NOT and arbitrary filters are not legal in many column stores. Reject them explicitly.
- Silently adding "allow filtering"-style behavior that changes performance characteristics.
- Treating the partition key as a generic `_id` without documenting the mapping.
- Exposing `CqlSession`, `Row` or `SimpleStatement` through standard-contract methods. Keep them in the database-named interface.
- Narrowing TTL with an unchecked cast.
- Mutable query objects shared between threads (`CassandraQuery`).
- Copying stale names (`CouchDBDocumentQuery`) from another driver.
- Duplicate connection logic across configuration and factory.

## 10. Definition of Done

- [ ] `DatabaseConfiguration`, `DatabaseManagerFactory` and `DatabaseManager` are implemented and registered.
- [ ] Statement building, execution and row conversion are separate, tested units.
- [ ] Every `Condition` is either translated or rejected with `UnsupportedOperationException`. Partition-key and identity rules are documented.
- [ ] `update` semantics (upsert or strict) are documented and tested.
- [ ] Identifiers are quoted. Values are bound. Injection tests exist.
- [ ] Null, empty and invalid query cases have tests.
- [ ] Client and session ownership is documented. Lifecycle tests pass.
- [ ] Native-typed extras sit on a database-named interface.
- [ ] Unit and integration tests follow `TESTING-GUIDELINE.adoc`. Integration tests run with a pinned image.
- [ ] TCK status is stated. `mvn -pl jnosql-<db> verify` passes. Other modules are unchanged.
- [ ] Configuration keys and features are documented in the root [`README.adoc`](../README.adoc).
