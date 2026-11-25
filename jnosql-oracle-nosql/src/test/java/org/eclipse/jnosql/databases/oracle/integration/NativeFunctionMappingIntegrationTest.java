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
package org.eclipse.jnosql.databases.oracle.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.oracle.communication.Database;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLConfigurations;
import org.eclipse.jnosql.databases.oracle.mapping.OracleNoSQLTemplate;
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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(NativeFunctionAccount.class)
@AddPackages(OracleNoSQLTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Oracle NoSQL native functions through JDQL and the mapping layer")
class NativeFunctionMappingIntegrationTest {

    private static final String ALL = "FROM NativeFunctionAccount";

    private static boolean warmedUp;

    static {
        System.setProperty(OracleNoSQLConfigurations.HOST.get(), Database.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Inject
    private OracleNoSQLTemplate template;

    @Inject
    private NativeFunctionAccountRepository repository;

    @BeforeEach
    void insertDataset() throws InterruptedException {
        warmUp();
        template.delete(NativeFunctionAccount.class).execute();
        template.insert(List.of(
                new NativeFunctionAccount("1", "Otavio", "OTAVIO", -100),
                new NativeFunctionAccount("2", "otavio", "JAVA", 100),
                new NativeFunctionAccount("3", "JAVA", "java", -250),
                new NativeFunctionAccount("4", "Jakarta", "JAKARTA", 500),
                new NativeFunctionAccount("5", "Abc", "ABC", 0)));
    }

    // The container rejects the first queries after it starts, for example with an invalid partition id
    private void warmUp() throws InterruptedException {
        if (warmedUp) {
            return;
        }
        var deadline = System.nanoTime() + Duration.ofSeconds(180).toNanos();
        while (true) {
            try {
                template.delete(NativeFunctionAccount.class).execute();
                found(ALL);
                warmedUp = true;
                return;
            } catch (RuntimeException exception) {
                if (System.nanoTime() > deadline) {
                    throw exception;
                }
                Thread.sleep(1_000);
            }
        }
    }

    private Set<String> found(String jdql) {
        List<NativeFunctionAccount> result = template.query(jdql).result();
        return result.stream().map(NativeFunctionAccount::id).collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> ids(String... ids) {
        return new TreeSet<>(Set.of(ids));
    }

    private static Set<String> idsOf(List<NativeFunctionAccount> accounts) {
        return accounts.stream().map(NativeFunctionAccount::id).collect(Collectors.toCollection(TreeSet::new));
    }

    static Stream<Arguments> literalQueries() {
        return Stream.of(
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE LOWER(name) = 'java'", ids("3")),
                Arguments.of("FROM NativeFunctionAccount WHERE LENGTH(name) = 6", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE LEFT(name, 3) = 'Ota'", ids("1")),
                Arguments.of("FROM NativeFunctionAccount WHERE RIGHT(name, 3) = 'vio'", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) = 100", ids("1", "2")),
                Arguments.of("FROM NativeFunctionAccount WHERE ABS(balance) BETWEEN 100 AND 250", ids("1", "2", "3")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) LIKE 'OT%'", ids("1", "2")),
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
                Arguments.of("FROM NativeFunctionAccount WHERE NOT (UPPER(name) = 'OTAVIO')", ids("3", "4", "5")),
                Arguments.of("FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO' AND balance > 0", ids("2")),
                Arguments.of("FROM NativeFunctionAccount WHERE id = '3' AND UPPER(name) = 'OTAVIO'", ids()),
                Arguments.of("FROM NativeFunctionAccount WHERE id = '1' AND UPPER(name) = 'OTAVIO'", ids("1")));
    }

    @Nested
    @DisplayName("Given literals and native functions")
    class Literals {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.integration.NativeFunctionMappingIntegrationTest#literalQueries")
        void shouldReturnExactIds(String jdql, Set<String> expected) {
            assertThat(found(jdql)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Attributes are translated to their mapped database field names")
        void shouldUseMappedFieldNames() {
            assertSoftly(soft -> {
                soft.assertThat(found("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(otherName)"))
                        .isEqualTo(ids("1", "3", "4", "5"));
                soft.assertThat(found("FROM NativeFunctionAccount WHERE ABS(balance) = 250")).isEqualTo(ids("3"));
            });
        }

        @Test
        void shouldCount() {
            List<Long> count = template.query(
                    "SELECT COUNT(this) FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").result();

            assertThat(count).containsExactly(2L);
        }

        @Test
        @DisplayName("A native predicate honors ORDER BY")
        void shouldHonorOrder() {
            List<NativeFunctionAccount> result = template.query(
                    "FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO' ORDER BY balance DESC").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly("2", "1");
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

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactlyInAnyOrder("1", "2");
        }

        @Test
        void shouldBindPositionalParameters() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(?1)")
                    .bind(1, "java").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly("3");
        }

        @Test
        @DisplayName("LENGTH is evaluated by JNoSQL and LEFT is executed by the database")
        void shouldEvaluatePartially() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                    .bind("prefix", "Ota").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactly("1");
        }

        @Test
        @DisplayName("RIGHT with a length evaluated from a parameter")
        void shouldEvaluateRightPartially() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE RIGHT(name, LENGTH(:suffix)) = :suffix")
                    .bind("suffix", "vio").result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactlyInAnyOrder("1", "2");
        }

        @Test
        @DisplayName("A parameter can be the function argument and the other side of the comparison")
        void shouldBindFunctionOnBothSides() {
            List<NativeFunctionAccount> result = template
                    .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name) AND ABS(balance) = :amount")
                    .bind("name", "otavio").bind("amount", 100).result();

            assertThat(result).extracting(NativeFunctionAccount::id).containsExactlyInAnyOrder("1", "2");
        }

        @Test
        @DisplayName("Rebinding the same query gives a result for each execution")
        void shouldRebindSameQuery() {
            var query = template.query("FROM NativeFunctionAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");

            List<NativeFunctionAccount> first = query.bind("prefix", "Ota").result();
            List<NativeFunctionAccount> second = query.bind("prefix", "Jak").result();
            List<NativeFunctionAccount> third = query.bind("prefix", "ota").result();

            assertSoftly(soft -> {
                soft.assertThat(idsOf(first)).isEqualTo(ids("1"));
                soft.assertThat(idsOf(second)).isEqualTo(ids("4"));
                soft.assertThat(idsOf(third)).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("Injection payloads are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("?", "' OR '1'='1", "\\", "{\"$ne\": null}",
                    "'; DROP TABLE NativeFunctionAccount; --", "`", "é😀")) {
                List<NativeFunctionAccount> result = template
                        .query("FROM NativeFunctionAccount WHERE UPPER(name) = UPPER(:name)")
                        .bind("name", payload).result();
                assertThat(result).as(payload).isEmpty();
            }
            assertThat(found(ALL)).isEqualTo(ids("1", "2", "3", "4", "5"));
        }
    }

    @Nested
    @DisplayName("Given repositories")
    class Repositories {

        @Test
        void shouldExecuteRepeatedly() {
            assertSoftly(soft -> {
                soft.assertThat(idsOf(repository.sameUpperName("otavio"))).isEqualTo(ids("1", "2"));
                soft.assertThat(idsOf(repository.sameUpperName("java"))).isEqualTo(ids("3"));
                soft.assertThat(idsOf(repository.startingWith("Ja"))).isEqualTo(ids("4"));
                soft.assertThat(idsOf(repository.startingWith("JA"))).isEqualTo(ids("3"));
                soft.assertThat(repository.startingWith("ja")).isEmpty();
                soft.assertThat(idsOf(repository.upperOtavio())).isEqualTo(ids("1", "2"));
            });
        }

        @Test
        void shouldSupportNativeFunctionOnBothSides() {
            assertThat(idsOf(repository.sameNameIgnoringCase())).isEqualTo(ids("1", "3", "4", "5"));
        }

        @Test
        void shouldSupportNestedFunctionsAndRightLength() {
            assertSoftly(soft -> {
                soft.assertThat(idsOf(repository.upperPrefix("OTA"))).isEqualTo(ids("1", "2"));
                soft.assertThat(idsOf(repository.upperPrefix("JAK"))).isEqualTo(ids("4"));
                soft.assertThat(idsOf(repository.endingWith("vio"))).isEqualTo(ids("1", "2"));
                soft.assertThat(idsOf(repository.endingWith("A"))).isEqualTo(ids("3"));
            });
        }

        @Test
        void shouldCombineNativePredicates() {
            assertSoftly(soft -> {
                soft.assertThat(idsOf(repository.absoluteBalance(100))).isEqualTo(ids("1", "2"));
                soft.assertThat(idsOf(repository.upperNameAndAbsoluteAbove("OTAVIO", 100))).isEmpty();
                soft.assertThat(idsOf(repository.upperNameAndAbsoluteAbove("OTAVIO", 99))).isEqualTo(ids("1", "2"));
                soft.assertThat(idsOf(repository.lowerLike("ja%"))).isEqualTo(ids("3", "4"));
            });
        }

        @Test
        @DisplayName("A repository method with an unsupported native function is rejected")
        void shouldRejectUnsupportedFunction() {
            assertThatThrownBy(() -> repository.unsupportedLength())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Given DELETE and UPDATE")
    class Mutations {

        @Test
        void shouldDeleteOnlyMatchingRecords() {
            template.query("DELETE FROM NativeFunctionAccount WHERE UPPER(name) = 'OTAVIO'").executeUpdate();

            assertThat(found(ALL)).isEqualTo(ids("3", "4", "5"));
        }

        @Test
        void shouldUpdateOnlyMatchingRecords() {
            template.query("UPDATE NativeFunctionAccount SET balance = 7 WHERE LOWER(name) = 'java'")
                    .executeUpdate();

            assertSoftly(soft -> {
                soft.assertThat(found("FROM NativeFunctionAccount WHERE balance = 7")).isEqualTo(ids("3"));
                soft.assertThat(found(ALL)).isEqualTo(ids("1", "2", "3", "4", "5"));
            });
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
            assertThat(found(ALL)).isEqualTo(ids("1", "2", "3", "4", "5"));
            assertThat(found("FROM NativeFunctionAccount WHERE balance = 7")).isEmpty();
        }

        @Test
        void shouldRejectUnsupportedSelect() {
            assertThatThrownBy(() -> found("FROM NativeFunctionAccount WHERE LEFT(name, balance) = 'A'"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Oracle NoSQL");
        }
    }
}
