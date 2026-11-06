# Graph Driver Development Guide

Reference implementation: **`jnosql-tinkerpop`**.
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-tinkerpop/src/main/java/org/eclipse/jnosql/databases/tinkerpop/` (`communication/` and `mapping/`).

---

## 1. Purpose and scope

A graph database stores vertices and edges, both carrying properties and labels, and is queried by traversal.
In Eclipse JNoSQL most graph databases are reached through **Apache TinkerPop**, so the driver's job is usually *not* to implement a graph engine or a query language. It is to:

- obtain a TinkerPop `Graph` from provider settings;
- adapt it to the JNoSQL communication contract (vertices as entities, plus edges);
- convert between `CommunicationEntity` and TinkerPop `Vertex`/`Edge`;
- run conditions as Gremlin traversals.

Prefer reusing TinkerPop's `GraphTraversal`, `P`, `TextP`, `__` and `T` over inventing a database-specific traversal system. Only write a non-TinkerPop graph driver if the database has no TinkerPop provider.

## 2. Relevant Jakarta NoSQL contracts

| Type | Package | Role |
|---|---|---|
| `DatabaseConfiguration`, `DatabaseManagerFactory`, `DatabaseManager` | `org.eclipse.jnosql.communication.semistructured` | Same chain as column and document drivers. |
| `GraphDatabaseManager` | `org.eclipse.jnosql.communication.graph` (module `jnosql-mapping-graph`) | Extends `DatabaseManager` with `edge(source, label, target, properties)`, `remove(source, label, target)`, `deleteEdge(id)`, `findEdgeById(id)`. |
| `CommunicationEdge` | same package | Edge data returned by the manager. |
| `GraphTemplate` | `org.eclipse.jnosql.mapping.graph` | Framework-level template that `TinkerpopTemplate` extends. |

The framework owns the entity-level template, `@Entity`/`@Id` mapping, repositories and the `GraphManagerSupplier` CDI wiring. The driver owns native interaction and, for TinkerPop, a richer template with traversal abstractions.

## 3. Reference implementation architecture

| Responsibility | Class |
|---|---|
| Provider SPI | `communication/GraphConfiguration` (`Function<Settings, Graph>`, with `ServiceLoader` helpers `getConfiguration()` and `getConfiguration(Class)`) |
| Configuration adapter | `communication/DatabaseConfigurationAdapter` (implements `DatabaseConfiguration`; provider selected by `jnosql.graph.tinkerpop.provider`, constant `TINKERPOP_PROVIDER`, otherwise the first `GraphConfiguration` found) |
| Factory | inner class `DatabaseConfigurationAdapter.GraphDatabaseManagerFactory` (ignores the database name; `close()` is empty) |
| Manager API | `communication/TinkerpopGraphDatabaseManager` (extends `GraphDatabaseManager` and `Supplier<Graph>`; `ID = T.id.getAccessor()`; `static of(Graph)`) |
| Manager | `communication/DefaultTinkerpopGraphDatabaseManager` |
| Condition to traversal | `communication/TraversalExecutor.getPredicate(CriteriaCondition)` |
| Vertex to entity | `communication/CommunicationEntityConverter` (enum singleton `Function<Vertex, CommunicationEntity>`) |
| Edge value | `communication/TinkerpopCommunicationEdge` |
| LIKE support | `communication/LikeToRegex` |
| Transactions | `communication/GraphTransactionUtil` |
| Template | `mapping/TinkerpopTemplate`, `mapping/DefaultTinkerpopTemplate`, `mapping/AbstractTinkerpopTemplate` |
| Entity conversion | `mapping/GraphEntityConverter` |
| Traversal abstractions | `mapping/VertexTraversal`, `EdgeTraversal`, `ValueMapTraversal` and their `Default*`/`Abstract*` classes, `EdgeEntity` |
| Gremlin text queries | `mapping/GremlinExecutor`, `GremlinParamParser`, `GremlinQueryException` |
| CDI | `mapping/configuration/GraphSupplier` (produces the `Graph` and closes it on dispose), `mapping/spi/TinkerpopExtension`, `mapping/spi/TemplateBean`, `mapping/query/TinkerpopRepositoryBean`, `mapping/Transactional` and `TransactionalInterceptor` |
| Services | `META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration`, `META-INF/services/jakarta.enterprise.inject.spi.Extension` |
| Tests | `communication/DefaultTinkerpopGraphDatabaseManagerTest` (abstract, with `TinkerGraphTest`, `Neo4jTest` and `ArangoDBTest` subclasses), `mapping/TinkerpopTemplateTest`, `mapping/EdgeEntityTest`, `mapping/configuration/GraphSupplierTest`, `cdi/*` producers |

## 4. Semantic code analysis

- **Vertices and edges.** `insert` creates a vertex labelled with the entity name and puts the generated identifier back on the entity under `ID`. Edges are created by `edge(...)`, which finds or creates both vertices first.
- **Identity.** `defaultIdFieldName()` returns `T.id.getAccessor()`. `update` requires `ID`. It throws `IllegalArgumentException` when absent and `EmptyResultException` when no such vertex exists. That is a clear, separate error for each case.
- **Traversal execution.** `select`, `delete` and `count` share `buildGraphTraversalOf`, which starts at `graph.traversal().V().hasLabel(name)` and applies `TraversalExecutor.getPredicate`. `TraversalExecutor` is a static, pure function over a closed enum, with one translation per `Condition`.
- **Provider neutrality.** The communication package does not import any vendor class. `GraphConfiguration` is the single seam. Vendors (TinkerGraph, Neo4j, ArangoDB) appear only as test dependencies and test producers.
- **Ownership.** Whoever builds the `Graph` is the configuration, but `DefaultTinkerpopGraphDatabaseManager.close()` closes it (wrapping failures in `CommunicationException` with the cause) while the factory's `close()` is empty. In the CDI path, `GraphSupplier` also closes the `Graph` on dispose. Two paths can therefore close the same graph. Decide on one owner in a new driver.
- **Naming.** `name()` returns a constant sentence ("The tinkerpop graph database manager"), not a meaningful name. Avoid that.
- **Cohesion.** `communication/` stays small (manager, executor, converter). `mapping/` carries many traversal classes. That fan-out reflects a fluent DSL. It is not a pattern to copy into the communication layer.

**Observations (do not copy blindly)**

- `update(Iterable)` is `Stream.of(entities).forEach(this::update)`. By inspection `Stream.of(iterable)` yields a single element (the `Iterable`), so this recurses into `update(Iterable)`. It was not verified by running, and no test covers it. Use `entities.forEach(this::update)`.
- In `select`, `limit` and `skip` are applied before `order().by(...)`, and `skip` is ignored when `limit > 0`. Ordering and paging semantics should be: filter, order, skip, limit.
- `edge(...)` calls `properties.forEach(...)` without a null check.
- `GraphTransactionUtil` swallows commit exceptions after logging them at INFO.
- `GraphDatabaseManagerFactory` ignores the database name, since a `Graph` is already a single database. Document such a decision.

## 5. SOLID considerations

- **SRP.** Manager: lifecycle and operations. `TraversalExecutor`: condition translation. `CommunicationEntityConverter`: vertex conversion. Transactions: `GraphTransactionUtil`. Keep Gremlin text parsing (`GremlinExecutor`) out of the communication layer.
- **OCP.** New graph providers are new `GraphConfiguration` services. No change to the generic layer is needed.
- **LSP.** A `GraphDatabaseManager` must honor edge semantics: `remove` on a missing vertex fails the same way for source and target. `deleteEdge` and `findEdgeById` behave consistently for missing ids.
- **ISP.** `TinkerpopGraphDatabaseManager` adds only `Supplier<Graph>` (native access) to the standard `GraphDatabaseManager`. Native access is opt-in.
- **DIP.** The manager depends on the TinkerPop `Graph` interface, not on a provider class. This is the main design strength to keep.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Graph provider | Already an SPI (`GraphConfiguration`). Add a service, not a conditional. |
| Condition translation | The `switch` in `TraversalExecutor` is acceptable typed dispatch over the closed `Condition` enum. Extract per-condition translators only when provider-specific predicates diverge (for example text search predicates not supported by every provider). |
| Traversal steps (`repeat`, `until`, `order`) | The fluent interfaces express a DSL. Do not turn them into strategies. |
| Transaction handling | `GraphTransactionUtil` makes a policy choice (auto-commit unless locked). If more policies appear, a small `TransactionPolicy` interface can replace the ThreadLocal flag. Do not add it before then. |

Provider limitations are real: not every TinkerPop provider supports transactions, `TextP` predicates or all id types. Check `graph.features()` where a capability matters and throw `UnsupportedOperationException` with a clear message. Do not assume TinkerGraph behavior holds for every provider.

## 7. Step-by-step driver implementation (fictional `AcmeGraph`)

Assume `AcmeGraph` ships a TinkerPop provider.

```
jnosql-acmegraph/
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmegraph/communication/
    │   │   └── AcmeGraphConfiguration.java       # implements GraphConfiguration
    │   └── resources/META-INF/services/
    │       └── org.eclipse.jnosql.databases.tinkerpop.communication.GraphConfiguration
    └── test/java/...
        ├── AcmeGraphConfigurationTest.java
        └── AcmeGraphManagerTest.java             # extends the shared manager tests, integration
```

1. **Depend on `jnosql-tinkerpop`** and the provider's TinkerPop artifact. Do not copy `DefaultTinkerpopGraphDatabaseManager`.
2. **Implement the SPI:**

   ```java
   public final class AcmeGraphConfiguration implements GraphConfiguration {
       @Override
       public Graph apply(Settings settings) {
           Objects.requireNonNull(settings, "settings is required");
           String url = settings.get(AcmeGraphConfigurations.URL, String.class)
                   .filter(value -> !value.isBlank())
                   .orElseThrow(() -> new IllegalArgumentException("The AcmeGraph URL is required"));
           return AcmeGraph.open(url);
       }
   }
   ```

3. **Register** the SPI file for `GraphConfiguration`. Users select it with `jnosql.graph.tinkerpop.provider` when several providers are present.
4. **Add provider-only behavior** (indexes, schema, capabilities) in the configuration or a small collaborator. The generic layer stays provider-neutral.
5. **Reuse the generic manager.** `DatabaseConfigurationAdapter` wraps your `Graph` automatically.
6. **For a non-TinkerPop database**, implement `DatabaseConfiguration`, `DatabaseManagerFactory` and `GraphDatabaseManager` directly. Add a condition translator (pure), an entity converter, and edge operations. Throw `UnsupportedOperationException` for anything the database cannot do. Then add a CDI `Extension` only if you need a database-named template.
7. **Tests** as in section 8.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). The existing TinkerPop tests do not use `@Nested`, `@DisplayName` or soft assertions. New tests must.

Unit tests:

- Condition translation: each `Condition` handled by `TraversalExecutor` (positive), unsupported conditions (negative), `BETWEEN`/`IN` boundaries, nested AND/OR/NOT.
- Vertex to entity conversion: properties, id, multi-valued and null properties.
- Provider selection: explicit provider setting, SPI default, none found (`CommunicationException`).

Integration tests:

- The reference uses an in-memory TinkerGraph and needs no container. Subclasses of the abstract manager test cover other providers (Neo4j, ArangoDB). For a networked provider, use Testcontainers and `-Djnosql.test.integration=true`.
- Positive: insert vertex, update, delete, select with condition, count, create edge with properties, `findEdgeById`, `remove`, `deleteEdge`.
- Negative: `update` without id, `update` of a missing vertex, edge between entities without ids, `edge(...)` with null label.
- Boundary: empty graph, large `limit`, `skip` beyond size, sorted and paged results, edges with no properties.
- Iterable operations (`insert(Iterable)`, `update(Iterable)`) with empty and multi-element input.
- Lifecycle: `close()` closes the graph once. Failures surface as `CommunicationException`.
- Transactions where the provider supports them: commit visibility and rollback.

TCK: not wired for graph drivers in this repository. State that in the Definition of Done.

## 9. Common pitfalls

- Reimplementing Gremlin steps or a traversal engine already provided by TinkerPop.
- Importing vendor classes in the generic communication layer.
- Treating TinkerGraph behavior (id types, transactions, text predicates) as universal.
- Two owners closing the `Graph` (manager, factory and CDI disposer).
- Swallowing transaction failures.
- Applying `order`, `skip` and `limit` in the wrong order.
- `Stream.of(iterable)` where `iterable.stream()` or `forEach` was intended.
- Unchecked null `properties` or labels on edges.
- Leaking `Vertex`/`Edge` through the standard `GraphDatabaseManager` signatures instead of `CommunicationEntity`/`CommunicationEdge`.
- Constant, meaningless `name()` values.
- A god template that mixes entity conversion, traversal building and transaction policy.

## 10. Definition of Done

- [ ] A `GraphConfiguration` (or a full `DatabaseConfiguration`) is implemented and registered.
- [ ] The generic layer has no vendor imports. Provider-specific code lives in the provider module.
- [ ] Vertex and edge operations behave per the `GraphDatabaseManager` contract, with distinct exceptions for missing id and missing vertex.
- [ ] Condition translation is pure, covers every supported `Condition`, and rejects the rest with `UnsupportedOperationException`.
- [ ] Ordering, skip and limit are applied in the correct order and tested.
- [ ] One owner closes the `Graph`. Transaction failures are not swallowed. Lifecycle is tested.
- [ ] Provider limitations are documented and capability checks exist where behavior differs.
- [ ] Unit and integration tests follow `TESTING-GUIDELINE.adoc`, including empty, null and boundary cases.
- [ ] TCK status is stated. `mvn -pl jnosql-<db> verify` passes. Other modules are unchanged.
- [ ] Settings and features are documented in the root [`README.adoc`](../README.adoc).
