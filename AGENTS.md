# AGENTS.md — Eclipse JNoSQL Databases

Instructions for AI coding agents and human contributors working on database drivers in this repository.
Read this file first, then follow the [workflow](#agent-workflow).

## Project architecture

### What this repository is

`eclipse-jnosql/jnosql-databases` contains **database drivers**: one Maven module per database
(`jnosql-redis`, `jnosql-cassandra`, `jnosql-mongodb`, `jnosql-influxdb`, `jnosql-tinkerpop`, `jnosql-milvus`, …)
plus shared infrastructure in `jnosql-database-commons`.
Modules are declared in the root [`pom.xml`](pom.xml), whose parent is `jnosql-mapping-parent`.

This repository is **not** a Quarkus extension repository. Do not add Quarkus, build-time or native-image extension code here.
CDI usage is limited to Jakarta CDI producers/extensions inside a driver's `mapping` package.

### Responsibilities and layers

| Layer | Lives in | Responsibility |
|---|---|---|
| Jakarta NoSQL / Jakarta Data specifications | `jakarta.nosql`, `jakarta.data` (external) | Application-facing APIs (`Template`, entity annotations, repositories). |
| Eclipse JNoSQL communication API | `eclipse-jnosql/jnosql`, `org.eclipse.jnosql.communication.*` (external) | Contracts a driver implements: `DatabaseConfiguration`, `DatabaseManagerFactory`, `DatabaseManager`, `KeyValueConfiguration`, `BucketManagerFactory`, `BucketManager`. |
| Eclipse JNoSQL mapping | `eclipse-jnosql/jnosql`, `org.eclipse.jnosql.mapping.*` (external) | Entity metadata, templates, repositories, CDI wiring; per-model managers such as `VectorManager`, `GraphDatabaseManager`. |
| **Database drivers** | `jnosql-<database>/` (this repo) | Translate the communication contracts into native-client calls. |
| Shared driver infrastructure | `jnosql-database-commons/` (this repo) | JSON-B supplier, value writers, configuration reader, integration-test switch. |

Important facts, verified against the sources:

- Drivers implement `org.eclipse.jnosql.communication.*` types, **not** `jakarta.nosql.communication.*`. No module in this repo imports the latter.
- Column, document, time-series, vector and graph drivers share one contract family in `org.eclipse.jnosql.communication.semistructured`:
  `DatabaseConfiguration` (a `Function<Settings, DatabaseManagerFactory>`) → `DatabaseManagerFactory` (a `Function<String, DatabaseManager>`, `AutoCloseable`) → `DatabaseManager` (`AutoCloseable`).
- Key-value uses a parallel family in `org.eclipse.jnosql.communication.keyvalue`: `KeyValueConfiguration` → `BucketManagerFactory` → `BucketManager`.
- Model-specific managers extend `DatabaseManager`: `org.eclipse.jnosql.mapping.vector.VectorManager` (adds `search`) and `org.eclipse.jnosql.communication.graph.GraphDatabaseManager` (adds edge operations).
- Time-series temporal queries (`latest`, `earliest`, `since`, `until`, `last`) are implemented by the framework as conditions, ordering and limit on the id field. The driver only has to execute those.
- Configuration is a `Settings` map read by a `DatabaseConfiguration`. Each driver declares a `<Database>Configurations` enum of property keys (for example `MilvusConfigurations`).
- Drivers register through `META-INF/services` (for example `org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration`). The mapping layer discovers them with `ServiceLoader`, or by the `jnosql.<model>.provider` setting.
- A `mapping` package exists only where the driver exposes database-specific features (Mongo, Cassandra, Redis, TinkerPop). Time-series and vector drivers rely on the generic mapping modules.
- A database may serve several models. Share the native client and configuration; keep model-specific managers separate (see [`development-guides/common.md`](development-guides/common.md)).

## Agent workflow

When implementing a new database driver, follow these steps in order:

1. Read [`CONTRIBUTING_DATABASE.adoc`](CONTRIBUTING_DATABASE.adoc).
2. Read [`TESTING-GUIDELINE.adoc`](TESTING-GUIDELINE.adoc).
3. Read [`development-guides/common.md`](development-guides/common.md).
4. Identify the database model.
5. Read the model-specific guide (table below).
6. Inspect the selected reference implementation.
7. Inspect the relevant Jakarta NoSQL and Eclipse JNoSQL APIs.
8. Identify the database's native capabilities.
9. Design the integration.
10. Implement and test incrementally.
11. Verify compatibility.
12. Commit using Conventional Commits.

If a database supports several models, read **every** applicable guide. Do not force it into one category.

## Documentation routing

| Document | Use it for |
|---|---|
| [`CONTRIBUTING_DATABASE.adoc`](CONTRIBUTING_DATABASE.adoc) | Established contribution conventions. |
| [`TESTING-GUIDELINE.adoc`](TESTING-GUIDELINE.adoc) | Authoritative testing conventions. |
| [`development-guides/common.md`](development-guides/common.md) | Architecture, design principles, workflow, Definition of Done. |
| [`development-guides/key-value.md`](development-guides/key-value.md) | Key-value drivers (reference: `jnosql-redis`). |
| [`development-guides/column.md`](development-guides/column.md) | Column drivers (reference: `jnosql-cassandra`). |
| [`development-guides/document.md`](development-guides/document.md) | Document drivers (reference: `jnosql-mongodb`). |
| [`development-guides/graph.md`](development-guides/graph.md) | Graph drivers (reference: `jnosql-tinkerpop`). |
| [`development-guides/time-series.md`](development-guides/time-series.md) | Time-series drivers (reference: `jnosql-influxdb`). |
| [`development-guides/vector.md`](development-guides/vector.md) | Vector drivers (reference: `jnosql-milvus`). |

Reference implementations show *what exists*, not what is ideal. Legacy code may predate the current guidelines.

## Engineering rules

- Prefer semantic names (`DocumentQueryConversor`, `MilvusFilterConverter`) over implementation-driven ones (`Helper`, `Util`, `Processor`).
- Apply SOLID pragmatically. Do not add structure that nothing needs.
- Give each class a single responsibility.
- Prefer composition over unnecessary inheritance.
- Introduce interfaces only when they express a meaningful contract.
- Use Strategy when behavior varies independently and the variants are genuinely interchangeable.
- Avoid large conditional chains on database type, query type or operation type. A `switch` over a closed enum with one-line arms is acceptable typed dispatch.
- Keep simple guard clauses (`Objects.requireNonNull`, size checks). Do not replace them with patterns.
- Keep query translation separate from query execution.
- Keep entity conversion separate from persistence operations.
- Reuse `jnosql-database-commons`. Do not duplicate shared infrastructure.
- Favor immutable objects (records, final fields) where practical.
- Validate inputs and fail predictably (`NullPointerException` with a message, `IllegalArgumentException`, `UnsupportedOperationException` for unsupported capabilities).
- Do not catch broad exceptions in a way that hides database failures. If you wrap, preserve the cause.
- Preserve existing API compatibility unless explicitly authorized.
- Never invent database capabilities the native client does not provide.
- Do not introduce speculative abstractions.

## Testing requirements

[`TESTING-GUIDELINE.adoc`](TESTING-GUIDELINE.adoc) is authoritative. Do not restate or override it. New tests must follow its conventions for:

- JUnit 5 and AssertJ;
- test naming;
- `@Nested` classes and `@DisplayName`;
- Given / When / Then;
- soft assertions;
- positive and negative scenarios;
- mutation testing (PIT) where configured.

Many existing driver tests predate the guideline and do not use `@Nested`, `@DisplayName` or soft assertions. Do not copy their style.

Additionally, drivers need:

- integration tests against a real database, gated by `-Djnosql.test.integration=true` (see `IntegrationTest` in `jnosql-database-commons`);
- the Jakarta NoSQL TCK where applicable (`mvn test -Ptck -pl <module>`). Currently wired for `jnosql-mongodb`, `jnosql-redis`, `jnosql-valkey`, `jnosql-arangodb` and `jnosql-oracle-nosql`.

## Commit requirements

- Work on a dedicated feature branch, never directly on `main`.
- Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`).
- Keep commits logically focused.
- Documentation-only tasks: commit exactly **one file per commit**.
- Never push or open a pull request automatically.

## Known outdated points in existing docs

Found while writing these guides. They are reported, not fixed here:

- `CONTRIBUTING_DATABASE.adoc` mentions Java 17. The build uses Java 21.
- It uses the package convention `org.jnosql.databases...`. Real modules use `org.eclipse.jnosql.databases.<db>.communication` and `.mapping`.
- It requires a mapping-layer `Supplier` for every driver. `jnosql-influxdb` and `jnosql-milvus` have no `mapping` package.
- `README.adoc` lists only MongoDB under the TCK. Redis, Valkey, ArangoDB and Oracle NoSQL also contain TCK suppliers.
