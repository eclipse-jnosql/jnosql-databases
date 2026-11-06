# Graph Driver Development Guide

Reference implementation: **`jnosql-neo4j`** (Neo4j through the official `neo4j-java-driver`, Cypher over Bolt).
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-neo4j/src/main/java/org/eclipse/jnosql/databases/neo4j/` with `communication/` and `mapping/` packages.

---

## 1. Purpose and scope

A graph database stores vertices (nodes) and edges (relationships). Both carry properties, and edges carry a label. The communication implementation:

- connects to the database and owns the native driver and sessions;
- translates `SelectQuery`, `DeleteQuery` and `UpdateQuery` into the native query language (Cypher) and executes them;
- converts native nodes and relationships into `CommunicationEntity` and `CommunicationEdge`;
- implements the edge operations of `GraphDatabaseManager`;
- optionally exposes native capabilities (raw Cypher, bounded traversal) through a database-specific interface.

Jakarta NoSQL and the JNoSQL framework own entity mapping, `GraphTemplate`, repositories and CDI wiring for the standard API. The driver owns everything that mentions Neo4j.

## 2. Relevant Jakarta NoSQL contracts

| Type | Package | Role |
|---|---|---|
| `DatabaseConfiguration`, `DatabaseManagerFactory`, `DatabaseManager` | `org.eclipse.jnosql.communication.semistructured` | Standard chain. |
| `GraphDatabaseManager` | `org.eclipse.jnosql.communication.graph` (module `jnosql-mapping-graph`) | `DatabaseManager` plus `edge(source, label, target, properties)`, `remove(source, label, target)`, `deleteEdge(K id)` and `findEdgeById(K id)`. |
| `CommunicationEdge` | same | `id()`, `source()`, `target()`, `label()`, `properties()`. |
| `GraphTemplate` | `org.eclipse.jnosql.mapping.graph` | Mapping-level API, extended by `Neo4JTemplate`. |

The framework sources live in the sibling `../jnosql` repository. Read them before implementing.

## 3. Reference implementation architecture

| Responsibility | Class (under `communication/` unless noted) |
|---|---|
| Configuration | `Neo4JConfiguration` (final, `apply(Settings)`; default `bolt://localhost:7687`) |
| Property keys | `Neo4JConfigurations` (`URI`, `USERNAME`, `PASSWORD`, `DATABASE`; keys `jnosql.neo4j.*`) |
| Resolved settings | `Neo4Property` (package-private record: `uri`, `user`, `password`) |
| Factory | `Neo4JDatabaseManagerFactory` (holds the native `Driver`; `apply(database)` creates the database if missing and opens a `Session`; `close()` closes the driver) |
| Database-specific contract | `Neo4JDatabaseManager extends GraphDatabaseManager`: `cypher(...)`, `traverse(startNodeId, label, depth)`, `edge(source, label, target)`, `remove(...)` |
| Manager | `DefaultNeo4JDatabaseManager` (implements the contract above; owns one `Session`) |
| Query translation | `Neo4JQueryBuilder` (package-private `enum` singleton `INSTANCE`; `buildQuery` for select/delete/update, `buildCountQuery`, `identifier`) |
| `LIKE` translation | `LikeToCypherRegex` |
| Edge value | `Neo4jCommunicationEdge` (package-private record implementing `CommunicationEdge`) |
| Exceptions | `Neo4JCommunicationException`, `EdgeCommunicationException` (both extend `CommunicationException`) |
| Mapping | `mapping/Neo4JTemplate`, `DefaultNeo4JTemplate`, `Neo4JRepository`, `Neo4JRepositoryProxy`, `Neo4JRepositoryBean`, `Neo4JExtension`, `Cypher` (annotation), `ParamConverterUtils` |
| CDI producer | `mapping/GraphManagerSupplier` (`@Produces @ApplicationScoped`; database from `MappingConfigurations.GRAPH_DATABASE`, default `neo4j`; `@Disposes` closes the manager) |
| Service registration | `src/main/resources/META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration` and `jakarta.enterprise.inject.spi.Extension` |
| Tests | `src/test/java/org/eclipse/jnosql/databases/neo4j/`: `communication/Neo4JDatabaseManagerTest`, `Neo4JQueryBuilderTest`, `LikeToCypherRegexTest`, `DatabaseContainer`; `mapping/*Test`; `integration/*IntegrationTest` |

Module dependencies (`jnosql-neo4j/pom.xml`): `neo4j-java-driver`, `jnosql-database-commons`, `jnosql-mapping-graph`, and `testcontainers-neo4j` for tests.

## 4. Semantic code analysis

**What reads well**

- Layering is visible in the names: `Neo4JConfiguration` resolves settings, `Neo4JDatabaseManagerFactory` builds managers, `Neo4JQueryBuilder` only produces text and parameters, and `DefaultNeo4JDatabaseManager` executes.
- Query translation does not execute. `Neo4JQueryBuilder.buildQuery(query, parameters)` fills a parameter map and returns Cypher, so `Neo4JQueryBuilderTest` needs no database.
- Values are bound as parameters. Labels, property names and relationship types go through `Neo4JQueryBuilder.identifier(...)`, which rejects anything outside a safe pattern (`IllegalArgumentException`). `Neo4JQueryBuilderTest` has tests for executable identifiers, sort syntax and relationship types.
- `Neo4JDatabaseManager` is a focused extension interface. Native features (`cypher`, `traverse`) do not pollute `GraphDatabaseManager`.
- Native types are kept inside the package. `Neo4Property` and `Neo4jCommunicationEdge` are package-private records, and `Neo4JConfiguration.apply` returns the factory, not a `Driver`.
- Edge-related failures use `EdgeCommunicationException`, which narrows `Neo4JCommunicationException`.

**Vertices and edges.** A vertex is a `CommunicationEntity` whose name is the node label. The native `elementId` is exposed as the `_id` element (`DefaultNeo4JDatabaseManager.ID`). Edges are created from two entities that already have an `_id`. `edge(source, label, target, properties)` ensures both endpoints exist (`ensureEntityExists`) and returns a `Neo4jCommunicationEdge`. `findEdgeById` and `deleteEdge` accept the id as `Object`.

**Observations (do not copy blindly)**

- `DefaultNeo4JDatabaseManager` mixes statement text, transactions, parameter flattening and record-to-entity conversion in one class. `extractEntity` and the insert/update statement assembly are good candidates for collaborators in a new driver.
- `Neo4JDatabaseManagerFactory.databaseExists` catches `Exception`, logs it and returns `false`. A connectivity failure then surfaces later as "Could not create database". Prefer failing at the first fault with the cause.
- `Neo4JDatabaseManagerFactory.createDatabase` concatenates the database name into `CREATE DATABASE`. A new driver should validate the name the way `identifier` does.
- A manager owns one `Session`. Native sessions are not thread-safe, so do not share a manager across threads without checking the native documentation.
- `select` materializes results into a list before returning a `Stream`. That is simple and avoids dangling transactions, at the cost of memory for large results.
- `traverse` does not validate `depth`.
- `insertEntities` dereferences the result record without handling an empty result.
- Exception types vary between `Neo4JCommunicationException`, `EdgeCommunicationException` and plain `CommunicationException`. A new driver should define one hierarchy.

## 5. SOLID considerations

- **SRP.** Keep configuration, factory, query translation, entity/edge conversion and execution in separate types. Neo4j already separates the first three and the query builder. Do the same for conversion rather than adding methods to the manager.
- **OCP.** `Neo4JQueryBuilder.createWhereClause` switches over `Condition`. Adding a condition means editing it. That is acceptable for a closed, small enumeration, and unsupported conditions fail with `CommunicationException`. Native features are added by extending the database-specific interface, not by changing `GraphDatabaseManager`.
- **LSP.** `DefaultNeo4JDatabaseManager` can replace any `GraphDatabaseManager`. Keep this true. Unsupported TTL inserts throw `UnsupportedOperationException`. Document each such deviation, and do not narrow what the base contract accepts.
- **ISP.** Do not extend `GraphDatabaseManager` with provider-only methods. Put them in `Neo4JDatabaseManager`-style interface (`cypher`, `traverse`). Callers that only need the standard contract stay unaffected.
- **DIP.** The manager depends on the native `Session` it is given, and the factory depends on a `Driver` passed to its constructor. This makes substitution possible in tests. The static `of(Neo4Property)` isolates driver construction. Keep creating native clients in one place.

Prefer composition: the manager uses `Neo4JQueryBuilder.INSTANCE` as a collaborator and does not inherit from a base class. Avoid base manager classes shared by providers.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Condition to Cypher operator | `getConditionOperator` is a short `switch` expression with a `default` that throws. Keep it. |
| `AND`/`OR`/`NOT` versus comparison versus `IN` handling | A growing `switch` in `createWhereClause`. If many more condition kinds with distinct logic arrive, introduce a small `CypherCondition` strategy per kind. Not before. |
| Select, delete and update translation | Three distinct `buildQuery` overloads. This is polymorphism by overload and is clear. Do not merge them behind a flag. |
| Node versus relationship conversion | `extractEntity` branches by native type (`NODE`, `RELATIONSHIP`). Extract a `NativeRecordConverter` when a third kind (path, list) is needed. |
| Raw query execution | A capability of the database-specific interface (`cypher`). Keep it out of the standard contract. |
| Provider capabilities | Make them explicit (TTL throws `UnsupportedOperationException`). Do not hide them behind silent no-ops. |

Do not replace guards such as `Objects.requireNonNull`, `identifier` validation, or "entity must have `_id`" with strategy classes.

## 7. Step-by-step driver implementation (fictional `AcmeGraph`)

```
jnosql-acmegraph/
├── pom.xml                                   # parent: jnosql-databases-parent
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmegraph/
    │   │   ├── communication/
    │   │   │   ├── AcmeGraphConfiguration.java
    │   │   │   ├── AcmeGraphConfigurations.java
    │   │   │   ├── AcmeGraphDatabaseManagerFactory.java
    │   │   │   ├── AcmeGraphDatabaseManager.java          # extends GraphDatabaseManager
    │   │   │   ├── DefaultAcmeGraphDatabaseManager.java
    │   │   │   ├── AcmeGraphQueryBuilder.java
    │   │   │   ├── AcmeGraphEntityConverter.java
    │   │   │   ├── AcmeGraphCommunicationEdge.java        # record
    │   │   │   └── AcmeGraphCommunicationException.java
    │   │   └── mapping/                                   # only if needed
    │   └── resources/META-INF/services/
    │       └── org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration
    └── test/java/...
```

1. **Identify contracts.** Read `GraphDatabaseManager` and `CommunicationEdge` in `../jnosql`, then `Neo4JDatabaseManager`.
2. **Inspect the reference.** Read the classes in section 3, in the order configuration, factory, query builder, manager.
3. **Create the Maven module.** Copy the dependency shape of `jnosql-neo4j/pom.xml` (native driver, `jnosql-database-commons`, `jnosql-mapping-graph`, Testcontainers for tests) and register it in the root `pom.xml` modules list. Do not add a classification parent module.
4. **Native dependencies.** Pin the native driver version in a property, as `neo4j.driver` does.
5. **Configuration.** An enum implementing `Supplier<String>` for keys, and a final configuration class:

   ```java
   public final class AcmeGraphConfiguration implements DatabaseConfiguration {
       @Override
       public AcmeGraphDatabaseManagerFactory apply(Settings settings) {
           var uri = settings.getOrDefault(AcmeGraphConfigurations.URI, "acme://localhost:1234");
           var user = settings.get(AcmeGraphConfigurations.USERNAME, String.class).orElse(null);
           return AcmeGraphDatabaseManagerFactory.of(new AcmeGraphProperty(uri, user));
       }
   }
   ```

6. **Manager and factory.** The factory holds the native client and implements `close()`. `apply(database)` validates the name and returns a manager.

   ```java
   public interface AcmeGraphDatabaseManager extends GraphDatabaseManager {
       Stream<CommunicationEntity> query(String statement, Map<String, Object> parameters);
   }
   ```

7. **Conversion.** Put node/edge to `CommunicationEntity` / `CommunicationEdge` in a dedicated converter, with a record for the edge (`record AcmeGraphCommunicationEdge(Object id, CommunicationEntity source, CommunicationEntity target, String label, Map<String, Object> properties) implements CommunicationEdge`). Decide how native ids map to `_id`.
8. **Query translation and execution.** A builder that returns text plus parameters (a small record is clearer than an out-parameter map), validates identifiers, binds values, and rejects unsupported conditions. Execution stays in the manager.
9. **Lifecycle.** The factory owns the native client. A manager owns only what it opened. Close both in `close()`. Document thread-safety.
10. **Register.** Add the `DatabaseConfiguration` service file. Add mapping classes (template, repository, CDI producer, extension) only if the module needs to expose database-specific features, as Neo4j does.
11. **Integration tests.** A Testcontainers holder like `DatabaseContainer` with a pinned image, guarded by `@EnabledIfSystemProperty(named = IntegrationTest.NAMED, matches = IntegrationTest.MATCHES)`.
12. **TCK.** No TCK supplier is wired for `jnosql-neo4j` in this repository. State the TCK status explicitly for the new driver.

### UUID and API Documentation

- In `jnosql-neo4j` the entity `_id` is the native `elementId` string set by `DefaultNeo4JDatabaseManager` on insert and used by `edge`, `remove` and `traverse`. It is not an application UUID. Keep an application UUID in a separate property.
- The driver passes entity values to Cypher parameters without a UUID converter, and `jnosql-neo4j` has no UUID test. The `neo4j-java-driver` 6.3.0 accepts a `UUID` parameter (`Values.value(UUID)` returns a `UUIDValue`), but storage depends on the server and Bolt version. The tests use the `neo4j:5.26.3` image. Verify with an integration test before documenting support, and convert with `UUIDValueWriter` if the target server does not store it.
- Vertex and edge properties, query parameters and results must use one representation. Test a UUID property on a vertex and on an edge, and a filter on it.

Rules and tests are in [UUID Compatibility and Conversion](common.md#uuid-compatibility-and-conversion). Any new public API must follow [`javadoc.md`](javadoc.md): document public interfaces, add a short example to important methods, and state null, empty-result and exception behavior.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). `Neo4JDatabaseManagerTest` uses AssertJ `SoftAssertions` but is flat (no `@Nested` or `@DisplayName`) and names tests `shouldXxx`. New tests must follow the guideline.

Unit tests (no database), as in `Neo4JQueryBuilderTest` and `LikeToCypherRegexTest`:

- Select with and without condition, limit, skip and sorts. Delete and update. Count.
- `_id` translated to the native id function.
- Values bound as parameters, never concatenated.
- Rejection of invalid identifiers, sort syntax and relationship types. Unsupported conditions. Update with no elements.

Integration tests (`-Djnosql.test.integration=true`, Testcontainers):

| Scenario | Existing example |
|---|---|
| Vertex insert, retrieval, update, count, delete | `shouldInsert`, `shouldSelectById`, `shouldUpdate`, `shouldCount`, `shouldDeleteById` |
| Edge creation, with and without properties, lookup, removal | `shouldCreateEdge`, `shouldCreateEdgeWithProperties`, `shouldFindEdgeById`, `shouldRemoveEdge`, `shouldDeleteEdgeById` |
| Relationships and traversal | `shouldTraverseGraph` |
| Native query | `shouldExecuteCustomQuery` |
| Filtering and paging | `shouldSelectLimit`, `shouldSelectStartAndLimit`, `shouldSelectOrderAsc`, `shouldSelectLike`, `shouldSelectAnd`, `shouldSelectOr` |
| Template and repository over the driver | `integration/GraphTemplateIntegrationTest`, `TemplateIntegrationTest`, `RepositoryIntegrationTest` |

Add what the reference does not cover, where your driver supports it:

- Missing vertex or edge: `findEdgeById` returns `Optional.empty()`, selecting a missing id returns an empty stream.
- Invalid identifiers: null or blank ids, an entity or endpoint without `_id` (`EdgeCommunicationException`).
- Invalid queries: unsupported condition and malformed native statements raise a documented exception with the cause preserved.
- Unsupported operations: TTL inserts throw `UnsupportedOperationException`.
- Connection failure: an unreachable URI fails with a clear exception and releases native resources.
- Resource cleanup: closing the manager closes its session. Closing the factory closes the driver, once.
- Duplicate edges: `edge(source, label, target)` does not create a second relationship, and `edge(source, label, target, properties)` updates the existing one.
- Boundaries: traversal depth of zero or a negative number, empty result sets.

## 9. Common pitfalls

- Building Cypher text and running it in the same method. Keep the builder pure.
- Concatenating user values, labels or database names into native statements without validation.
- Returning native `Node`, `Relationship`, `Session` or `Driver` objects from standardized interfaces.
- Growing `DefaultNeo4JDatabaseManager`-style classes with conversion and statement assembly.
- Duplicating node-to-entity conversion for select, traverse and edge lookup.
- Creating edges to endpoints that do not exist, or silently creating duplicates.
- Mixing up the native element id and an application-level id.
- Catching `Exception` and returning a default, which hides connectivity errors.
- Leaving sessions, transactions or result streams open.
- Treating `traverse` or raw queries as part of the standard contract.
- Adding a base manager class and a deep hierarchy for providers that share only a few lines.

## 10. Definition of Done

- [ ] `GraphDatabaseManager` and `CommunicationEdge` contracts are implemented and documented, with deviations listed.
- [ ] Names are semantic, and each class has one responsibility (configuration, factory, translation, conversion, execution).
- [ ] Provider-specific features sit in a database-specific interface, not in the standard one.
- [ ] Query translation is pure and unit-tested, with bound parameters and validated identifiers.
- [ ] Conversion of vertices and edges is in one place and reused.
- [ ] One owner closes each native resource. Lifecycle is tested.
- [ ] Failures raise a documented exception hierarchy and preserve causes.
- [ ] Integration tests (Testcontainers) cover vertices, edges, traversal or queries, missing elements and invalid input, following `TESTING-GUIDELINE.adoc`.
- [ ] TCK status is stated. Run it if a TCK supplier is added.
- [ ] Service files are registered. `mvn -pl jnosql-<db> verify` passes and other modules are unchanged.
- [ ] Features and settings are documented in the root [`README.adoc`](../README.adoc).
