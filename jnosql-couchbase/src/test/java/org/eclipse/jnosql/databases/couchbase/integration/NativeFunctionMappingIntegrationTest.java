/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.couchbase.communication.CouchbaseUtil;
import org.eclipse.jnosql.databases.couchbase.communication.Database;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseExtension;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseTemplate;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(NativeFunctionAccount.class)
@AddPackages(CouchbaseTemplate.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class,
        CouchbaseExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Couchbase native functions through JDQL and the mapping layer")
class NativeFunctionMappingIntegrationTest {

    static {
        CouchbaseUtil.systemPropertySetup();
    }

    @Inject
    private DocumentTemplate template;

    @Inject
    private NativeFunctionAccountRepository repository;

    @BeforeEach
    void insertDataset() {
        template.delete(NativeFunctionAccount.class).execute();
        await().atMost(Duration.ofSeconds(30)).until(() -> found("FROM NativeFunctionAccount").isEmpty());
        template.insert(List.of(
                new NativeFunctionAccount("1", "Otavio", "OTAVIO", -100),
                new NativeFunctionAccount("2", "otavio", "JAVA", 100),
                new NativeFunctionAccount("3", "JAVA", "java", -250),
                new NativeFunctionAccount("4", "Jakarta", "JAKARTA", 500),
                new NativeFunctionAccount("5", "Abc", "ABC", 0)));
        await().atMost(Duration.ofSeconds(30)).until(() -> found("FROM NativeFunctionAccount").size() == 5);
    }

    private Set<String> found(String jdql) {
        List<NativeFunctionAccount> result = template.prepare(jdql).<NativeFunctionAccount>result().toList();
        return result.stream().map(NativeFunctionAccount::id).collect(Collectors.toSet());
    }

    private static Set<String> ids(String... ids) {
        return Set.of(ids);
    }

    static Stream<Arguments> literalQueries() {
        return Stream.of(
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE LOWER(name) = 'java'", ids("3")),
                Arguments.of("FROM NativeFunctionAccount WHERE LENGTH(name) = 6", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE LEFT(name, 3) = 'Ota'", ids("1")),
                Arguments.of("FROM NativeFunctionAccount WHERE RIGHT(name, 3) = 'vio'", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) = 100", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE name = UPPER(otherName)", ids("3")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(otherName)",
                        ids("1", "3", "4", "5")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER('Otavio')", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE name = UPPER(LEFT('java', 4))", ids("3")),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) > 100 AND UPPER(name) <> 'ABC'",
                        ids("3", "4")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'JAVA' OR ABS(balance) = 0",
                        ids("3", "5")),
                Arguments.of("FROM NativeFunctionAccount WHERE NOT (UPPER(name) = 'OTAVIO')", ids("3", "4", "5")));
    }

    @Nested
    @DisplayName("Given literals and native functions")
    class Literals {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.couchbase.integration.NativeFunctionMappingIntegrationTest#literalQueries")
        void shouldReturnExactIds(String jdql, Set<String> expected) {
            assertThat(found(jdql)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Attributes are translated to their mapped database field names")
        void shouldUseMappedFieldNames() {
            assertThat(found("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(otherName)"))
                    .isEqualTo(ids("1", "3", "4", "5"));
        }

        @Test
        void shouldCount() {
            List<Long> count = template.prepare(
                    "SELECT COUNT(this) FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").<Long>result().toList();

            assertThat(count).containsExactly(2L);
        }
    }

    @Nested
    @DisplayName("Given parameters")
    class Parameters {

        @Test
        void shouldBindNamedParameters() {
            List<NativeFunctionAccount> result = template
                    .prepare("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name)")
                    .bind("name", "oTaVio").<NativeFunctionAccount>result().toList();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactlyInAnyOrder("1", "2");
        }

        @Test
        void shouldBindPositionalParameters() {
            List<NativeFunctionAccount> result = template
                    .prepare("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(?1)")
                    .bind("1", "java").<NativeFunctionAccount>result().toList();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly("3");
        }

        @Test
        @DisplayName("LENGTH is evaluated by JNoSQL and LEFT is executed by the database")
        void shouldEvaluatePartially() {
            List<NativeFunctionAccount> result = template
                    .prepare("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                    .bind("prefix", "Ota").<NativeFunctionAccount>result().toList();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly("1");
        }

        @Test
        @DisplayName("Rebinding the same query gives a result for each execution")
        void shouldRebindSameQuery() {
            var query = template.prepare("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");

            List<NativeFunctionAccount> first = query.bind("prefix", "Ota").<NativeFunctionAccount>result().toList();
            List<NativeFunctionAccount> second = query.bind("prefix", "Jak").<NativeFunctionAccount>result().toList();
            List<NativeFunctionAccount> third = query.bind("prefix", "ota").<NativeFunctionAccount>result().toList();

            assertSoftly(soft -> {
                soft.assertThat(first).extracting(NativeFunctionAccount::id).containsExactly("1");
                soft.assertThat(second).extracting(NativeFunctionAccount::id).containsExactly("4");
                soft.assertThat(third).extracting(NativeFunctionAccount::id).containsExactly("2");
            });
        }

        @Test
        @DisplayName("Injection payloads are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("$name", "' OR '1'='1", "\\", "{\"$ne\": null}", "'; DROP COLLECTION NativeFunctionAccount; --", "`")) {
                List<NativeFunctionAccount> result = template
                        .prepare("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name)")
                        .bind("name", payload).<NativeFunctionAccount>result().toList();
                assertThat(result).as(payload).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Given repositories")
    class Repositories {

        @Test
        void shouldExecuteRepeatedly() {
            assertSoftly(soft -> {
                soft.assertThat(repository.sameUpperName("otavio")).extracting(NativeFunctionAccount::id)
                        .containsExactlyInAnyOrder("1", "2");
                soft.assertThat(repository.sameUpperName("java")).extracting(NativeFunctionAccount::id)
                        .containsExactly("3");
                soft.assertThat(repository.startingWith("Ja")).extracting(NativeFunctionAccount::id)
                        .containsExactly("4");
                soft.assertThat(repository.startingWith("JA")).extracting(NativeFunctionAccount::id)
                        .containsExactly("3");
                soft.assertThat(repository.startingWith("ja")).isEmpty();
                soft.assertThat(repository.upperOtavio()).extracting(NativeFunctionAccount::id)
                        .containsExactlyInAnyOrder("1", "2");
            });
        }
    }

    @Nested
    @DisplayName("Given DELETE and UPDATE")
    class Mutations {

        @Test
        void shouldDeleteOnlyMatchingRecords() {
            template.prepare("DELETE FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").result().toList();

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    assertThat(found("FROM NativeFunctionAccount")).isEqualTo(ids("3", "4", "5")));
        }

        @Test
        void shouldUpdateOnlyMatchingRecords() {
            template.prepare("UPDATE NativeFunctionAccount SET balance = 7 WHERE LOWER(name) = 'java'")
                    .result().toList();

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    assertThat(found("FROM NativeFunctionAccount WHERE balance = 7")).isEqualTo(ids("3")));
        }

        @Test
        @DisplayName("An unsupported native function fails before any record is changed")
        void shouldRejectUnsupportedMutation() {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> template
                                .prepare("DELETE FROM NativeFunctionAccount WHERE UPPER(LEFT(name, balance)) = 'A'")
                                .result())
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> template
                                .prepare("UPDATE NativeFunctionAccount SET balance = 7 "
                                        + "WHERE UPPER(LEFT(name, balance)) = 'A'")
                                .result())
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            assertThat(found("FROM NativeFunctionAccount")).isEqualTo(ids("1", "2", "3", "4", "5"));
            assertThat(found("FROM NativeFunctionAccount WHERE balance = 7")).isEmpty();
        }

        @Test
        void shouldRejectUnsupportedSelect() {
            assertThatThrownBy(() -> found("FROM NativeFunctionAccount WHERE LEFT(name, balance) = 'A'"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Couchbase");
        }
    }
}
