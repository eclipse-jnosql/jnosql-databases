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
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations;
import org.eclipse.jnosql.databases.mongodb.mapping.MongoDBTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.mongodb.communication.DocumentDatabase.INSTANCE;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("MongoDB native functions through JDQL and the mapping layer")
class NativeFunctionMappingIntegrationTest {

    static {
        INSTANCE.get("native_function_mapping");
        System.setProperty(MongoDBDocumentConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "native_function_mapping");
    }

    @Inject
    private DocumentTemplate template;

    @Inject
    @Database(DatabaseType.DOCUMENT)
    private NativeFunctionAccountRepository repository;

    @BeforeEach
    void insertDataset() {
        template.delete(NativeFunctionAccount.class).execute();
        template.insert(List.of(
                new NativeFunctionAccount(1, "Otavio", "OTAVIO", -100),
                new NativeFunctionAccount(2, "otavio", "JAVA", 100),
                new NativeFunctionAccount(3, "JAVA", "java", -250),
                new NativeFunctionAccount(4, "Jakarta", "JAKARTA", 500),
                new NativeFunctionAccount(5, "Abc", "ABC", 0)));
    }

    private Set<Long> ids(String jdql) {
        List<NativeFunctionAccount> result = template.query(jdql).result();
        return result.stream().map(NativeFunctionAccount::id).collect(Collectors.toSet());
    }

    private static Set<Long> ids(Long... ids) {
        return Set.of(ids);
    }

    static Stream<Arguments> literalQueries() {
        return Stream.of(
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE LOWER(name) = 'java'", ids(3L)),
                Arguments.of("FROM NativeFunctionAccount WHERE LENGTH(name) = 6", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE LEFT(name, 3) = 'Ota'", ids(1L)),
                Arguments.of("FROM NativeFunctionAccount WHERE RIGHT(name, 3) = 'vio'", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) = 100", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE name = UPPER(otherName)", ids(3L)),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(otherName)",
                        ids(1L, 3L, 4L, 5L)),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER('Otavio')", ids(1L, 2L)),
                Arguments.of("FROM NativeFunctionAccount WHERE name = UPPER(LEFT('java', 4))", ids(3L)),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) > 100 AND UPPER(name) <> 'ABC'",
                        ids(3L, 4L)),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'JAVA' OR ABS(balance) = 0",
                        ids(3L, 5L)),
                Arguments.of("FROM NativeFunctionAccount WHERE NOT (UPPER(name) = 'OTAVIO')", ids(3L, 4L, 5L)));
    }

    @Nested
    @DisplayName("Given literals and native functions")
    class Literals {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.mongodb.integration.NativeFunctionMappingIntegrationTest#literalQueries")
        void shouldReturnExactIds(String jdql, Set<Long> expected) {
            assertThat(ids(jdql)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Attributes are translated to their mapped database field names")
        void shouldUseMappedFieldNames() {
            assertThat(ids("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(otherName)"))
                    .isEqualTo(ids(1L, 3L, 4L, 5L));
        }

        @Test
        void shouldCount() {
            List<Long> count = template.query(
                    "SELECT COUNT(this) FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").result();

            assertThat(count).containsExactly(2L);
        }
    }

    @Nested
    @DisplayName("Given parameters")
    class Parameters {

        @Test
        void shouldBindNamedParameters() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name)")
                    .bind("name", "oTaVio").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        void shouldBindPositionalParameters() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(?1)")
                    .bind(1, "java").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly(3L);
        }

        @Test
        @DisplayName("LENGTH is evaluated by JNoSQL and LEFT is executed by the database")
        void shouldEvaluatePartially() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                    .bind("prefix", "Ota").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly(1L);
        }

        @Test
        @DisplayName("Rebinding the same query gives a result for each execution")
        void shouldRebindSameQuery() {
            var query = template.query("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");

            List<NativeFunctionAccount> first = query.bind("prefix", "Ota").result();
            List<NativeFunctionAccount> second = query.bind("prefix", "Jak").result();
            List<NativeFunctionAccount> third = query.bind("prefix", "ota").result();

            assertSoftly(soft -> {
                soft.assertThat(first).extracting(NativeFunctionAccount::id).containsExactly(1L);
                soft.assertThat(second).extracting(NativeFunctionAccount::id).containsExactly(4L);
                soft.assertThat(third).extracting(NativeFunctionAccount::id).containsExactly(2L);
            });
        }

        @Test
        @DisplayName("Injection payloads are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("$name", "' OR '1'='1", "\\", "{\"$ne\": null}", "'; DROP")) {
                List<NativeFunctionAccount> result = template
                        .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name)")
                        .bind("name", payload).result();
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
                        .containsExactlyInAnyOrder(1L, 2L);
                soft.assertThat(repository.sameUpperName("java")).extracting(NativeFunctionAccount::id)
                        .containsExactly(3L);
                soft.assertThat(repository.startingWith("Ja")).extracting(NativeFunctionAccount::id)
                        .containsExactly(4L);
                soft.assertThat(repository.startingWith("JA")).extracting(NativeFunctionAccount::id)
                        .containsExactly(3L);
                soft.assertThat(repository.startingWith("ja")).isEmpty();
                soft.assertThat(repository.upperOtavio()).extracting(NativeFunctionAccount::id)
                        .containsExactlyInAnyOrder(1L, 2L);
            });
        }
    }

    @Nested
    @DisplayName("Given DELETE and UPDATE")
    class Mutations {

        @Test
        void shouldDeleteOnlyMatchingRecords() {
            template.query("DELETE FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").executeUpdate();

            assertThat(ids("FROM NativeFunctionAccount")).isEqualTo(ids(3L, 4L, 5L));
        }

        @Test
        void shouldUpdateOnlyMatchingRecords() {
            template.query("UPDATE NativeFunctionAccount SET balance = 7 WHERE LOWER(name) = 'java'")
                    .executeUpdate();

            assertThat(ids("FROM NativeFunctionAccount WHERE balance = 7")).isEqualTo(ids(3L));
        }

        @Test
        @DisplayName("An unsupported native function fails before any record is changed")
        void shouldRejectUnsupportedMutation() {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> template
                                .query("DELETE FROM NativeFunctionAccount WHERE UPPER(LEFT(name, balance)) = 'A'")
                                .executeUpdate())
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> template
                                .query("UPDATE NativeFunctionAccount SET balance = 7 "
                                        + "WHERE UPPER(LEFT(name, balance)) = 'A'")
                                .executeUpdate())
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            assertThat(ids("FROM NativeFunctionAccount")).isEqualTo(ids(1L, 2L, 3L, 4L, 5L));
            assertThat(ids("FROM NativeFunctionAccount WHERE balance = 7")).isEmpty();
        }

        @Test
        void shouldRejectUnsupportedSelect() {
            assertThatThrownBy(() -> ids("FROM NativeFunctionAccount WHERE LEFT(name, balance) = 'A'"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("MongoDB");
        }
    }
}
