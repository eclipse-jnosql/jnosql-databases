# Key-Value Driver Development Guide

Reference implementation: **`jnosql-redis`**.
Read [`AGENTS.md`](../AGENTS.md) and [`common.md`](common.md) first. Testing conventions: [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

Base path for Redis code: `jnosql-redis/src/main/java/org/eclipse/jnosql/databases/redis/` (`communication/` and `mapping/`).

---

## 1. Purpose and scope

A key-value database stores an opaque value under a key inside a named namespace (a *bucket*).
The communication implementation:

- connects to the database;
- exposes buckets as `BucketManager` instances;
- serializes values to and from the native representation;
- maps TTL, deletion and lookup onto native commands.

It does **not** provide arbitrary queries. The standardized key-value contract has `put`, `get` and `delete`. Do not assume a key-value store can filter by value or scan by condition.

## 2. Relevant Jakarta NoSQL contracts

Package `org.eclipse.jnosql.communication.keyvalue` (module `jnosql-communication-key-value` in `eclipse-jnosql/jnosql`):

| Type | Role |
|---|---|
| `KeyValueConfiguration` | `Function<Settings, BucketManagerFactory>`. Discovered with `ServiceLoader`. |
| `BucketManagerFactory` | `Function<String, BucketManager>` and `AutoCloseable`. Also `getList`, `getSet`, `getQueue`, `getMap`. |
| `BucketManager` | `name()`, `put` (several overloads, optionally with a `Duration` TTL), `get`, `delete`, `close()`. |
| `KeyValueEntity` | Key and value pair passed to `put`. |

The driver implements these. The framework (mapping module `jnosql-mapping-key-value`) provides `KeyValueTemplate`, entity conversion (`KeyValueEntityConverter`), repositories and the CDI producers (`BucketManagerSupplier`, `BucketManagerFactorySupplier`, `CollectionSupplier`).
Select a provider with `jnosql.keyvalue.provider` when several are on the classpath.

## 3. Reference implementation architecture

| Responsibility | Class |
|---|---|
| Configuration | `communication/RedisConfiguration` (implements `KeyValueConfiguration`; `apply(Settings)`) |
| Property keys | `RedisConfigurations`, `RedisClusterConfigurations`, `RedisSentinelConfigurations` |
| Connection modes | `RedisConfigurationsResolver` (sealed interface with single-node, cluster, Sentinel master and Sentinel slave resolvers) |
| Factory | `RedisBucketManagerFactory` (public interface; adds `getSortedSet`, `getCounter`) and `DefaultRedisBucketManagerFactory` (package-private; holds the `UnifiedJedis`) |
| Manager | `RedisBucketManager` |
| Key namespacing | `RedisUtils.createKeyWithNameSpace` (`namespace:key`; an empty key throws `IrregularKeyValue`) |
| Redis-native structures | `RedisList`, `RedisSet`, `RedisQueue`, `RedisMap` (over `RedisCollection`), `SortedSet`/`DefaultSortedSet`, `Counter`/`DefaultCounter`, `Ranking`/`DefaultRanking` |
| JSON serialization | `JsonbSupplier` from `jnosql-database-commons` |
| Mapping integration | `mapping/BucketManagerFactorySupplier` (CDI `@Produces` and `@Disposes`), `mapping/CollectionSupplier` (produces `Counter` and `SortedSet`) |
| Service registration | `src/main/resources/META-INF/services/org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration` |
| Tests | `RedisBucketManagerTest`, `RedisBucketManagerFactoryTest`, `RedisConfigurationTest`, `RedisUtilsTest`, collection tests; `communication/KeyValueDatabase` (Testcontainers enum); TCK `tck/RedisDBTemplateSupplier` |

Lifecycle: `RedisConfiguration.apply` builds one `UnifiedJedis`, wraps it in the factory, and the factory creates a `RedisBucketManager` per bucket name.

## 4. Semantic code analysis

- **Naming.** `RedisBucketManager`, `RedisConfigurationsResolver` and `Counter`/`SortedSet` say what they are. `RedisUtils` is the weakest name in the module. It holds one cohesive thing (key namespacing) but the name invites unrelated additions.
- **Cohesion.** Each Redis structure has its own small class. `RedisCollection` is a shared base for the `java.util.Collection` adapters. It is one level of inheritance, with real shared behavior (serialize, index, remove).
- **Encapsulation.** `DefaultRedisBucketManagerFactory` is package-private. Callers see only `RedisBucketManagerFactory`.
- **Value objects.** `Ranking` is a small interface for member and points. Connection settings are not grouped into records here, unlike Milvus.
- **Exceptions.** `IrregularKeyValue` for bad keys, `NullPointerException` and `IllegalArgumentException` for argument checks.
- **Dependencies.** The manager depends on `UnifiedJedis` directly. There is no abstraction in between, which is acceptable as Jedis is the only client.

Observations (not to be copied blindly):

- `put(entity, ttl)` sets the value and then sets the expiry in two commands, so it is not atomic. It also narrows the TTL with `(int) ttl.getSeconds()`. A new driver should use the native atomic form when one exists and reject TTLs the native client cannot represent.
- `get` treats an empty string as absent.
- `RedisBucketManager.close()` closes the shared `UnifiedJedis`, which the factory also owns. Pick one owner.
- `applyForSentinel` builds the replica client configuration with `SentinelMasterConfigurationsResolver` although `SentinelSlaveConfigurationsResolver` exists. Verify this before depending on it.

## 5. SOLID considerations

- **SRP.** Keep the manager to bucket-level `put`/`get`/`delete`. Put key naming in one class (as `RedisUtils` does) and serialization in another collaborator rather than in the manager.
- **OCP.** New connection modes extend the `RedisConfigurationsResolver` hierarchy (a sealed interface) instead of branching inside `apply`. Today `apply` still chooses a mode by key prefix. Keep that selection in one place.
- **LSP.** A `BucketManager` must behave as the contract says: `get` returns an `Optional`, `delete` of a missing key does not fail. Do not make a TTL silently ignored. Throw `UnsupportedOperationException` if the database cannot expire keys.
- **ISP.** The standard contract stays small. Redis extras sit on `RedisBucketManagerFactory`. Do the same for database-specific features: a sub-interface named for the database.
- **DIP.** Inject the native client (`UnifiedJedis`) into the factory and manager so tests can supply a fake or a Testcontainers client.

## 6. Strategy and extensibility

Genuine variation points in a key-value driver:

| Variation | Recommendation |
|---|---|
| Connection mode (single, cluster, Sentinel) | Already modeled by resolvers. Keep one resolver per mode. |
| Value serialization (JSON, binary, custom) | A small `serialize`/`deserialize` collaborator injected into the manager. Introduce it only when two formats must coexist. |
| Native structure types (list, set, map, queue, counter) | Separate classes per structure, created by the factory methods. No conditional on structure type. |

Do **not**:

- turn argument validation (`requireNonNull`, empty key) into strategy classes;
- add a query abstraction. The contract has none;
- dispatch on operation type with a `switch`. `put`, `get` and `delete` are separate methods.

## 7. Step-by-step driver implementation (fictional `AcmeDB`)

```
jnosql-acmedb/
├── pom.xml                                   # depends on jnosql-database-commons, jnosql-communication-key-value, acmedb client
└── src/
    ├── main/
    │   ├── java/org/eclipse/jnosql/databases/acmedb/communication/
    │   │   ├── AcmeDBConfiguration.java
    │   │   ├── AcmeDBConfigurations.java
    │   │   ├── AcmeDBBucketManagerFactory.java
    │   │   ├── AcmeDBBucketManager.java
    │   │   └── AcmeDBKeys.java               # namespace + key rules
    │   └── resources/META-INF/services/
    │       └── org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration
    └── test/java/org/eclipse/jnosql/databases/acmedb/communication/
        ├── AcmeDBConfigurationTest.java
        ├── AcmeDBKeysTest.java
        ├── AcmeDBBucketManagerTest.java      # integration, gated
        └── KeyValueDatabase.java             # Testcontainers enum
```

1. **Keys enum**

   ```java
   public enum AcmeDBConfigurations implements Supplier<String> {
       HOST("jnosql.acmedb.host"),
       PORT("jnosql.acmedb.port");
       private final String configuration;
       AcmeDBConfigurations(String configuration) { this.configuration = configuration; }
       @Override public String get() { return configuration; }
   }
   ```

2. **Configuration** builds the native client and hands it to the factory.

   ```java
   public final class AcmeDBConfiguration implements KeyValueConfiguration {
       @Override
       public BucketManagerFactory apply(Settings settings) {
           Objects.requireNonNull(settings, "settings is required");
           String host = settings.getOrDefault(AcmeDBConfigurations.HOST, "localhost").toString();
           return new AcmeDBBucketManagerFactory(AcmeClient.connect(host));
       }
   }
   ```

   Check the `Settings` method names against `org.eclipse.jnosql.communication.Settings` before copying. The snippet is illustrative.

3. **Factory** owns the client and creates managers per bucket.

   ```java
   final class AcmeDBBucketManagerFactory implements BucketManagerFactory {
       private final AcmeClient client;
       AcmeDBBucketManagerFactory(AcmeClient client) { this.client = client; }
       @Override public BucketManager apply(String bucket) {
           Objects.requireNonNull(bucket, "bucket is required");
           return new AcmeDBBucketManager(client, bucket);
       }
       @Override public void close() { client.close(); }
       // getList / getSet / getQueue / getMap: implement what AcmeDB supports,
       // otherwise throw UnsupportedOperationException naming the structure.
   }
   ```

4. **Manager** implements `put`, `get`, `delete`, `name`, and `close()`. The manager must not close a client the factory owns.
5. **Optional features** (counters, sorted sets): add a `AcmeDBBucketManagerFactory` sub-interface like `RedisBucketManagerFactory`, and a `mapping` package only if CDI producers are needed (`BucketManagerFactorySupplier`).
6. **Register** the service file with the fully-qualified name of `AcmeDBConfiguration`.
7. **Tests** as in section 8, then the TCK.

## 8. Testing

Follow [`TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc). New tests must use `@Nested`, `@DisplayName`, Given/When/Then and soft assertions. The existing Redis tests predate this, so do not copy their style.

Unit tests (no database):

- Configuration: defaults, every supported mode, missing and invalid settings.
- Key naming: normal key, empty key (negative), keys with the separator character (boundary).
- Value serialization round-trips for supported types, and unsupported types (negative).

Integration tests (real database, `-Djnosql.test.integration=true`, Testcontainers via a `KeyValueDatabase` enum):

- Positive: `put` then `get`, overwrite, `delete`, batch `put`/`get`, and TTL expiry (use a short TTL).
- Negative: `get` of a missing key returns an empty `Optional`. `delete` of a missing key does not throw. Null key or value is rejected.
- Boundary: empty string value, very large value, zero and negative TTL, a TTL below the native resolution.
- Lifecycle: the factory closes the client once. A manager used after close fails predictably.
- Native structures: one test class per structure, outside the common contract tests.

TCK: applicable to key-value drivers. Redis and Valkey have suppliers (`tck/RedisDBTemplateSupplier`, registered in `src/test/resources/META-INF/services/ee.jakarta.tck.nosql.TemplateSupplier`). Run `mvn test -Ptck -pl jnosql-<db>`.

## 9. Common pitfalls

- Adding query or filtering methods to the key-value contract. Keep them in a database-specific interface or leave them out.
- Putting Redis-style structures (`SortedSet`, `Counter`) on the standard `BucketManager`.
- Mixing key namespacing, serialization and I/O inside the manager (a god class).
- A boolean flag such as `put(entity, useTtl)` instead of the existing TTL overload.
- Two owners of the native client (factory and manager both close it).
- Non-atomic set-then-expire sequences, and silent narrowing of a `Duration`.
- Treating an empty value as "absent" without documenting it.
- Duplicate JSON handling when `JsonbSupplier` already exists in `jnosql-database-commons`.
- Leaking `UnifiedJedis`-style types through public signatures.
- Copying legacy test style rather than the testing guideline.

## 10. Definition of Done

- [ ] `KeyValueConfiguration`, `BucketManagerFactory` and `BucketManager` implemented. The service file is registered.
- [ ] `<Db>Configurations` documents every key. Required settings are validated.
- [ ] `put`, `get` and `delete` (and TTL variants) behave per contract. Unsupported TTL or structures throw `UnsupportedOperationException`.
- [ ] A single owner closes the native client. `close()` is covered by tests.
- [ ] Database-specific features live on database-named sub-interfaces. No native types in standard signatures.
- [ ] Unit tests follow `TESTING-GUIDELINE.adoc`. Positive, negative and boundary cases are covered.
- [ ] Integration tests run with `-Djnosql.test.integration=true` against a pinned container image.
- [ ] The Jakarta NoSQL TCK supplier is added and `mvn test -Ptck -pl jnosql-<db>` passes.
- [ ] `mvn -pl jnosql-<db> verify` passes. Public API of other modules is unchanged.
- [ ] Settings and features are documented in the root [`README.adoc`](../README.adoc).
