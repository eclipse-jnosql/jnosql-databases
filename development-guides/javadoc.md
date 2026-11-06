# Public API Javadoc Guidelines

Javadoc rules for every public type a driver adds to Eclipse JNoSQL Databases.
Read together with [`../AGENTS.md`](../AGENTS.md) and [`common.md`](common.md). Test conventions stay in [`../TESTING-GUIDELINE.adoc`](../TESTING-GUIDELINE.adoc).

The reader is a **Java developer using Eclipse JNoSQL**, not a maintainer of your driver. This guide does not ask you to rewrite existing Javadoc. It applies to the public APIs you introduce or change.

---

## 1. Documentation philosophy

Javadoc is part of the public API contract. A reader must learn:

- what the API does;
- when to use it;
- how to use it;
- what it returns;
- what can fail.

Describe observable behavior only. Mention internals (a native client, a query language, a pattern) only when they change what the caller sees, such as ordering, exceptions or resource ownership.

## 2. Public API requirements

Write meaningful Javadoc for:

- every public interface;
- every public class, record and enum;
- every public method declared by them;
- every public constructor;
- public constants whose meaning is not self-evident (for example default values and configuration keys).

Package-private and `private` types (such as `Neo4JQueryBuilder`) do not need it. A short comment where the code is not obvious is enough.

Use `{@inheritDoc}` when the inherited contract already describes the behavior. Add text only for what your implementation changes:

```java
/**
 * {@inheritDoc}
 *
 * <p>TTL is not supported by this driver and always throws {@link UnsupportedOperationException}.</p>
 */
@Override
public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) { ... }
```

Do not copy the parent description into the child.

## 3. Concise and semantic documentation

Javadoc must be short, clear, accurate, written in plain English and based on what the code does. Prefer intent over the signature.

| Avoid | Prefer |
|---|---|
| `Gets the id.` on `getId()` | Say what the id identifies, and its type or format if it is not obvious. |
| Marketing words (`powerful`, `seamless`) | Facts. |
| "This class uses the Strategy pattern" | What a caller can plug in or rely on. |
| History (`was added to fix...`) | Nothing. Use the commit message or `CHANGELOG.adoc`. |
| Several paragraphs | One summary sentence, then only the details a caller needs. |
| Snippets that do not compile | Verified snippets. |

The first sentence is the summary. It ends with a period and stands alone in the generated index.

## 4. API examples are essential

Add an example to every important public operation. Priorities:

1. public interfaces (one example on the type);
2. factory methods and configuration;
3. query and persistence methods;
4. conversion APIs;
5. methods with non-obvious semantics;
6. model-specific operations (`cypher`, `search`, ...).

The project builds with Java 21 (`maven.compiler.source` is 21 in the parent `pom.xml` of the `jnosql` repository). `{@snippet}` (JDK 18+) is therefore available. Use:

- `{@code ...}` for inline expressions;
- `{@snippet lang="java" : ... }` for multi-line examples;
- `<pre>{@code ... }</pre>` only to match surrounding existing Javadoc in the same file;
- `{@link ...}` to connect related types.

Rules for examples:

- Use real signatures from this repository or the framework. Open the declaration before writing the line.
- Show one realistic use case. Skip setup that does not matter and say what is assumed (`manager` is an existing instance).
- Do not use `{@snippet}` for pseudo-code. Pseudo-code goes in a plain sentence.
- In a `{@snippet}`, keep braces balanced and avoid a leading `@` on a line.

## 5. Method-level Javadoc

Use the tags that apply:

| Tag | Use |
|---|---|
| `@param` | Every parameter. Say what is accepted, not the type again. Mention `null` and range rules. |
| `@return` | What the result means. State empty results explicitly (`Optional.empty()`, empty `Stream`, `0`). |
| `@throws` | Each exception the code really throws, and when. Include unchecked exceptions that callers can handle. |
| `@see` | Related contracts. |
| `@since` | New public API of a released module. Use the version of the next release. |

State what matters to the caller:

- **null handling**: what is rejected, and with which exception;
- **empty results**: absent value versus empty collection;
- **idempotency**: does repeating the call change anything;
- **ordering**: guaranteed, or unspecified;
- **unsupported capabilities**: `UnsupportedOperationException` and which inputs trigger it;
- **resource ownership**: who closes a `Stream`, session or client;
- **conversion semantics**: what Java type comes back.

Do not document guarantees the code does not give. Read the implementation and tests, including the branches that throw.

## 6. Verified example

The following Javadoc matches the real contract. It does not assume a `null` result: `GraphDatabaseManager.findEdgeById` returns an `Optional`, and `Neo4JDatabaseManager` rejects a `null` id with `NullPointerException`.

```java
/**
 * Finds an edge by its identifier.
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Optional<CommunicationEdge> edge = manager.findEdgeById(id);
 * edge.ifPresent(found -> System.out.println(found.label()));
 * }
 *
 * @param id the edge identifier
 * @param <K> the identifier type
 * @return the edge, or {@link Optional#empty()} when no edge has that identifier
 * @throws NullPointerException if {@code id} is {@code null}
 */
<K> Optional<CommunicationEdge> findEdgeById(K id);
```

Similarly, `jakarta.nosql.Template.find(Class, K)` returns `Optional<T>`, not `null`. Do not copy a "returns `null` if absent" sentence from another API. Check the contract of the specific manager, template or repository first.

## 7. UUID documentation

When an API accepts, returns or converts a `java.util.UUID`, say so. For public conversion APIs state:

- **accepted inputs**: for example a `UUID` or its canonical text form;
- **returned Java type**: `UUID` or `String`;
- **failures**: malformed text and the exception raised, with the cause preserved;
- **null behavior**;
- **whether the value is preserved**: a `UUID -> String -> UUID` round trip must return an equal UUID.

Example, adapted from the real `UUIDValueWriter` (`ValueWriter<UUID, String>`):

```java
/**
 * Converts a {@link UUID} to its canonical text form.
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * String text = new UUIDValueWriter().write(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
 * // text is "550e8400-e29b-41d4-a716-446655440000"
 * }
 *
 * @param uuid the value to convert, may be {@code null}
 * @return the text form, or {@code null} when {@code uuid} is {@code null}
 */
public String write(UUID uuid)
```

Do not describe the storage format of a particular database unless the caller can observe it. The driver-level rules are in [UUID Compatibility and Conversion](common.md#uuid-compatibility-and-conversion).

## 8. Documentation quality checklist

- [ ] Every new public interface, class, constructor and method has Javadoc, and each summary sentence is meaningful.
- [ ] Important operations include an example that uses real APIs.
- [ ] `@param`, `@return` and `@throws` match the code, including the cases that throw.
- [ ] Null, empty-result, ordering, unsupported-operation and resource-ownership behavior is stated where relevant.
- [ ] UUID behavior is described for APIs that accept, return or convert UUIDs.
- [ ] `{@link}` and `@see` targets exist. `{@inheritDoc}` is used instead of copying.
- [ ] Examples are valid Java and compile against the current API.
- [ ] No implementation history, marketing text or pattern explanations.
- [ ] Javadoc builds without errors. Where the build supports it, run `mvn -pl jnosql-<db> javadoc:javadoc` and fix new warnings you introduced. The `maven-javadoc-plugin` is configured in the `jnosql` parent POM. If it cannot run in your environment, report that instead of claiming success.
