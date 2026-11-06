# Document Driver Development Guide

Reference implementation: **`jnosql-mongodb`**.
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path: `jnosql-mongodb/src/main/java/org/eclipse/jnosql/databases/mongodb/` (`communication/` and `mapping/`).

---

## 1. Purpose and scope

A document database stores nested, schema-flexible documents inside collections.
The communication implementation:

- creates and owns the native client;
- converts `CommunicationEntity` to native documents and back;
- translates `CriteriaCondition` trees into native filters;
- executes insert, update, delete, select (with projection, sort, skip, limit) and count.

## 2. Relevant Jakarta NoSQL contracts

Package `org.eclipse.jnosql.communication.semistructured` (repository `eclipse-jnosql/jnosql`):

| Type | Role |
|---|---|
| `DatabaseConfiguration` | `Function<Settings, DatabaseManagerFactory>`, found with `ServiceLoader`. |
| `DatabaseManagerFactory` | `Function<String, DatabaseManager>` and `AutoCloseable`. The name is the database. |
| `DatabaseManager` | `insert`, `update`, `delete`, `select`, `count`, `close` (plus default `exists`, `singleResult`, `selectCursor`, `query`). |
| `CommunicationEntity`, `Element` | Document and field. |
| `SelectQuery`, `DeleteQuery`, `UpdateQuery`, `CriteriaCondition` | Query model. |
| `ValueReader`, `ValueWriter`, `ValueUtil` (`org.eclipse.jnosql.communication`) | Value conversion hooks. |

The framework owns entity mapping (`jnosql-mapping-document`, `DocumentManagerSupplier`-style CDI wiring), `Template` and repositories. The driver owns native conversion and execution.

## 3. Reference implementation architecture

| Responsibility | Class (under `communication/` unless noted) |
|---|---|
| Configuration | `MongoDBDocumentConfiguration` (`apply(Settings)`, plus `get(Map)` and `get(MongoClient)` entry points) |
| Property keys | `MongoDBDocumentConfigurations` (host, user, password, URL, authentication source and mechanism, application name) |
| Connection helpers | `HostPortConfiguration`, `MongoAuthentication` |
| Factory | `MongoDBDocumentManagerFactory` (holds the `MongoClient`; `apply(db)` null-checks; `close()` closes the client) |
| Manager | `MongoDBDocumentManager` (public class) |
| Query translation | `DocumentQueryConversor.convert(CriteriaCondition)` returns a `Bson` |
| Entity conversion | `MongoDBUtils` (`getDocument`, `updateDocument`, `of(Map)`; `ID_FIELD = "_id"`) |
| Value conversion | `MongoDBValueWriteDecorator` (a `ValueWriter` wrapping `CompositeValueWriter(new UUIDValueWriter())`), `BinaryValueReader` (registered `ValueReader`) |
| Mapping | `mapping/DocumentManagerSupplier` (CDI), `mapping/MongoDBTemplate` and `mapping/DefaultMongoDBTemplate`, `mapping/ObjectIdConverter` |
| Services | `META-INF/services/org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration` and `META-INF/services/org.eclipse.jnosql.communication.ValueReader` |
| Tests | `DocumentQueryConverterTest`, `MongoDBDocumentManagerTest`, `MongoDBSpecificFeaturesTest`, `MongoDBValueWriteDecoratorTest`, `MongoAuthenticationTest`, `DocumentDatabase` (Testcontainers enum), `integration/*`, `mapping/*`; TCK `tck/MongoDBTemplateSupplier` |

Note the naming mismatch: the class is `DocumentQueryConversor`, its test is `DocumentQueryConverterTest`. New code should use "Converter" consistently.

## 4. Semantic code analysis

**Converters versus manager**

| Belongs in the converter | Belongs in the manager |
|---|---|
| Mapping `Condition` to native filter (`DocumentQueryConversor`) | Choosing the collection (`query.name()`) |
| Field-name validation (`field(name)` rejects `$`-prefixed or empty segments) | Applying projection, sort, skip and limit to the native cursor |
| Regex construction for `LIKE`, `STARTS_WITH`, `ENDS_WITH`, `CONTAINS` (values are `Pattern.quote`d) | Calling `insertOne`, `updateOne`, `deleteMany`, `countDocuments` |
| `CommunicationEntity` to `Document` (`MongoDBUtils.getDocument`) | Deciding upsert or strict update, and TTL support |

**Document identity.** `_id` is the identity. `insert` removes a null `_id` from the entity so the database generates one, then adds the generated `_id` back to the returned entity. `update(entity)` requires `_id` and throws `UnsupportedOperationException` without it, then uses `updateOne` with `$set`.

**Pagination and filtering.** `select` runs `find(filter)`, then projection, sort, skip and limit in that order. `selectCursor` comes from the `DatabaseManager` default methods.

**Native operations.** `MongoDBDocumentManager` also exposes `delete(String, Bson)`, `select(String, Bson)`, `count(String, Bson)` and `aggregate(...)`. These deliberately take native `Bson` and are reached through `MongoDBTemplate`, an optional, database-named extension. They do leak `Bson`. Keep such methods off the standard contract.

**Dependencies.** `DocumentQueryConversor` depends on `MongoDBValueWriteDecorator.MONGO_DB_VALUE_WRITER`, which is a static singleton. It keeps the converter simple but makes the writer chain hard to substitute in tests.

**Observations (do not copy blindly)**

- `convert` mixes one-line arms (`GREATER_THAN`) with multi-step arms (`EQUALS` with null handling, `NOT`, `AND`, `OR`, `BETWEEN`). It is already near the point where extraction is justified.
- `BETWEEN` reads `betweenList.get(0)` and `get(1)` without checking the size. A list with fewer than two values fails with an index error rather than a clear `IllegalArgumentException`. The Influx and Milvus converters validate this.
- `insert(entity, ttl)` throws `UnsupportedOperationException`. The `Iterable` + TTL overload throws per element, so an empty iterable passes silently.
- `update(UpdateQuery)` repeats `requireNonNull(query, "entity name is required")`, which looks intended to check the name.
- `MongoDBDocumentManager.close()` is a no-op and the factory owns the `MongoClient`. `DocumentManagerSupplier` closes the manager, so a client created through that path is never closed. Make the owner explicit in a new driver.

## 5. SOLID considerations

- **SRP.** Keep the manager to orchestration. Conversion of conditions, entities and values stays in converters. If you add aggregation, bulk writes or change streams, put them in new collaborators.
- **OCP.** New value types are added by registering a `ValueWriter` or `ValueReader` (service files), not by editing the converter. New conditions are new arms. If arms keep growing, extract them (section 6).
- **LSP.** A `DatabaseManager` that insists on `_id` for `update` narrows the contract. Document it and test it. Do not ignore a TTL argument. Throw.
- **ISP.** Native-typed operations (`Bson`, aggregation) belong on `MongoDBTemplate`, not on the standard interfaces.
- **DIP.** `MongoDBDocumentConfiguration.get(MongoClient)` allows injecting a prepared client. Prefer constructor injection of the client into the factory over global state.

## 6. Strategy and extensibility

| Variation | Recommendation |
|---|---|
| Value conversion (UUID, binary, custom types) | Already compositional: `CompositeValueWriter`, `ValueWriter`, `ValueReader`. Add a writer. |
| Condition translation | The `switch` over the closed `Condition` enum is acceptable typed dispatch while arms are short. When most arms need several steps, move to a map from `Condition` to a small `ConditionTranslator` (a `Function<CriteriaCondition, Bson>`), built in one place. Do this only if the growth is real, for example native operators that differ by server version. |
| Text matching (`LIKE`, `CONTAINS`, …) | The four arms differ only in the regex-building function. A shared helper that takes the function is enough. It is not a case for a class hierarchy. |

Keep as plain guards: `requireNonNull`, the `$`-prefix check in `field`, and a BETWEEN size check. These are validation.

This task does not propose changing the existing implementation. The guidance applies to new drivers.

## 7. Step-by-step driver implementation (fictional `AcmeDB` documents)

```
jnosql-acmedb/
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmedb/communication/
    │   │   ├── AcmeDBDocumentConfiguration.java
    │   │   ├── AcmeDBDocumentConfigurations.java
    │   │   ├── AcmeDBDocumentManagerFactory.java
    │   │   ├── AcmeDBDocumentManager.java
    │   │   ├── AcmeDBFilterConverter.java      # CriteriaCondition -> native filter (pure)
    │   │   ├── AcmeDBDocumentConverter.java    # CommunicationEntity <-> native document (pure)
    │   │   └── AcmeDBValueWriter.java          # custom ValueWriter, optional
    │   └── resources/META-INF/services/
    │       ├── org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration
    │       └── org.eclipse.jnosql.communication.ValueWriter   # only if you add one
    └── test/java/org/eclipse/jnosql/databases/acmedb/communication/
        ├── AcmeDBFilterConverterTest.java
        ├── AcmeDBDocumentConverterTest.java
        ├── AcmeDBDocumentManagerTest.java    # integration
        └── DocumentDatabase.java             # Testcontainers enum
```

1. **Configuration** with record-style grouped settings and eager validation:

   ```java
   public final class AcmeDBDocumentConfiguration implements DatabaseConfiguration {
       @Override
       public AcmeDBDocumentManagerFactory apply(Settings settings) {
           Objects.requireNonNull(settings, "settings is required");
           return new AcmeDBDocumentManagerFactory(AcmeClient.create(AcmeConnection.of(settings)));
       }
       public AcmeDBDocumentManagerFactory get(AcmeClient client) {
           Objects.requireNonNull(client, "client is required");
           return new AcmeDBDocumentManagerFactory(client);
       }
   }
   ```

2. **Filter converter** (pure, unit-testable):

   ```java
   final class AcmeDBFilterConverter {
       static AcmeFilter convert(CriteriaCondition condition) {
           Element element = condition.element();
           return switch (condition.condition()) {
               case EQUALS -> AcmeFilter.eq(element.name(), element.value().get());
               case BETWEEN -> between(element);
               case AND -> AcmeFilter.and(children(condition).stream()
                       .map(AcmeDBFilterConverter::convert).toList());
               default -> throw new UnsupportedOperationException(
                       "The condition " + condition.condition() + " is not supported by AcmeDB");
           };
       }
       private static AcmeFilter between(Element element) {
           List<Object> values = ValueUtil.convertToList(element.value());
           if (values.size() != 2) {
               throw new IllegalArgumentException("BETWEEN requires exactly two values");
           }
           return AcmeFilter.range(element.name(), values.get(0), values.get(1));
       }
   }
   ```

3. **Document converter**: decide how `_id` maps to the native identifier, how nested `Element` lists become sub-documents, and how nulls are stored. Test round-trips.
4. **Manager** composes both converters and the native collection access. Implement `insert`, `update`, `delete`, `select`, `count`, `close`. Throw `UnsupportedOperationException` for unsupported TTL.
5. **Factory** owns the client and closes it.
6. **Mapping** (optional): a database-named template for native features, plus CDI wiring, following `mapping/MongoDBTemplate` and `mapping/DefaultMongoDBTemplate`.
7. Register services. Add Testcontainers, integration and TCK classes.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). Existing Mongo tests do not use `@Nested`, `@DisplayName` or soft assertions. New tests must.

Unit tests:

- Filter converter: every supported condition, nested AND/OR/NOT, null equality, `BETWEEN` with fewer or more than two values (negative), invalid field names (`$where`, empty, empty segments), regex metacharacters in `LIKE`/`CONTAINS` values, unsupported conditions.
- Document converter: scalars, nested documents, lists of documents, binary, UUID, null and empty values, round-trip.
- Value writers and readers: registered through services, applied in the right order.
- Configuration: connection string versus host/port, authentication, invalid settings.

Integration tests (Testcontainers through a `DocumentDatabase`-style enum; `-Djnosql.test.integration=true`):

- Insert with and without `_id`. `update` with and without `_id`. Delete by condition. Count.
- Select with projection, sort, skip, limit. Skip beyond the result size (empty). Limit zero.
- Empty result sets. Missing collection.
- TTL unsupported. Lifecycle: factory closes the client.

TCK: applicable to document drivers. `tck/MongoDBTemplateSupplier` is registered in `src/test/resources/META-INF/services/ee.jakarta.tck.nosql.TemplateSupplier`. Run `mvn test -Ptck -pl jnosql-mongodb` (adapt the module for your driver).

## 9. Common pitfalls

- Concatenating user input into native query text or operators. Validate field names and quote regex input.
- Putting condition translation inside the manager.
- Leaking `Bson`/`Document` through `DatabaseManager` methods. Native operations belong on a database-named template or interface.
- Missing argument validation in one arm (BETWEEN, IN) while other arms validate.
- Inconsistent identity handling: generating ids in some paths and not others.
- Ignoring `Iterable` + TTL overloads. They must fail consistently, including for empty input.
- A static singleton `ValueWriter` that tests cannot replace.
- Two owners of the native client. A manager closing something the factory owns, or nobody closing it.
- Mixing `LIKE` and wildcard semantics from `StringMatch` without documenting differences.
- A very large `switch` where several arms have grown into multi-step blocks.

## 10. Definition of Done

- [ ] `DatabaseConfiguration`, factory and manager are implemented and registered.
- [ ] Filter conversion is pure, covers every supported `Condition`, and rejects the rest with `UnsupportedOperationException`. Malformed input throws `IllegalArgumentException`.
- [ ] Entity and value conversion are separate from persistence calls and round-trip tested.
- [ ] Identity rules (`_id`) are documented and tested for insert and update.
- [ ] Projection, sort, skip and limit are tested, including boundaries.
- [ ] TTL and other unsupported operations fail consistently.
- [ ] A single owner closes the native client. Lifecycle is tested.
- [ ] Native-typed operations sit on an optional, database-named interface.
- [ ] Unit and integration tests follow `TESTING-GUIDELINE.adoc`. A pinned container image is used.
- [ ] The TCK supplier is added and `mvn test -Ptck -pl jnosql-<db>` passes.
- [ ] `mvn -pl jnosql-<db> verify` passes. Other modules and public APIs are unchanged.
- [ ] Settings and features are documented in the root [`README.adoc`](../README.adoc).
