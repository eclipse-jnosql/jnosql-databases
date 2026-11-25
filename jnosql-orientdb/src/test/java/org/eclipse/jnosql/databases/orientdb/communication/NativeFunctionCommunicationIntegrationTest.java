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
package org.eclipse.jnosql.databases.orientdb.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeAll;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("OrientDB native functions through the communication API")
class NativeFunctionCommunicationIntegrationTest {

    private static final String ACCOUNTS = "NativeFunctionAccount";
    private static final String MIXED = "NativeFunctionMixed";
    private static final String ID = "code";

    private static DatabaseManager manager;

    @BeforeAll
    static void setUp() {
        manager = DocumentDatabase.INSTANCE.get(Database.DATABASE);
        manager.insert(entity(ACCOUNTS, 0, "seed", "seed", 0));
        manager.insert(entity(MIXED, 0, "seed", "seed", 0));
    }

    @BeforeEach
    void insertDataset() {
        manager.delete(NativeFunctionQueries.delete(ACCOUNTS, null, List.of()));
        manager.delete(NativeFunctionQueries.delete(MIXED, null, List.of()));
        account(1, "Otavio", "OTAVIO", -100);
        account(2, "otavio", "JAVA", 100);
        account(3, "JAVA", "java", -250);
        account(4, "Jakarta", "JAKARTA", 500);
        account(5, "Abc", "ABC", 0);
    }

    private static CommunicationEntity entity(String collection, int id, String name, String otherName,
                                              Object balance) {
        var entity = CommunicationEntity.of(collection);
        entity.add(ID, id);
        if (name != null) {
            entity.add("name", name);
        }
        if (otherName != null) {
            entity.add("otherName", otherName);
        }
        if (balance != null) {
            entity.add("balance", balance);
        }
        return entity;
    }

    private static void account(int id, String name, String otherName, int balance) {
        manager.insert(entity(ACCOUNTS, id, name, otherName, balance));
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument nested(String name, FunctionArgument... arguments) {
        return new FunctionArgument.NativeArgument(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand function(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaOperand attributeOperand(String name) {
        return new CriteriaOperand.Attribute(name);
    }

    private static CriteriaCondition compare(CriteriaOperand left, Condition condition, CriteriaOperand right) {
        return CriteriaCondition.of(left, condition, right);
    }

    private static Set<Integer> select(String collection, CriteriaCondition condition) {
        var query = new DefaultSelectQuery(0, 0, collection, List.of(), List.of(), condition, false);
        try (Stream<CommunicationEntity> result = manager.select(query)) {
            return result.map(e -> e.find(ID).orElseThrow().get(Integer.class)).collect(Collectors.toSet());
        }
    }

    private static Set<Integer> accounts(CriteriaCondition condition) {
        return select(ACCOUNTS, condition);
    }

    private static Set<Integer> ids(Integer... ids) {
        return Set.of(ids);
    }

    static Stream<Arguments> standardPredicates() {
        return Stream.of(
                Arguments.of("UPPER(name) = 'OTAVIO'",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO")),
                        ids(1, 2)),
                Arguments.of("LOWER(name) = 'java'",
                        compare(function("LOWER", attribute("name")), Condition.EQUALS, constant("java")), ids(3)),
                Arguments.of("ABS(balance) = 100",
                        compare(function("ABS", attribute("balance")), Condition.EQUALS, constant(100)),
                        ids(1, 2)),
                Arguments.of("ABS(balance) > 100",
                        compare(function("ABS", attribute("balance")), Condition.GREATER_THAN, constant(100)),
                        ids(3, 4)),
                Arguments.of("ABS(balance) >= 100",
                        compare(function("ABS", attribute("balance")), Condition.GREATER_EQUALS_THAN,
                                constant(100)), ids(1, 2, 3, 4)),
                Arguments.of("ABS(balance) < 100",
                        compare(function("ABS", attribute("balance")), Condition.LESSER_THAN, constant(100)),
                        ids(5)),
                Arguments.of("ABS(balance) <= 100",
                        compare(function("ABS", attribute("balance")), Condition.LESSER_EQUALS_THAN,
                                constant(100)), ids(1, 2, 5)),
                Arguments.of("UPPER(name) LIKE 'OT%'",
                        compare(function("UPPER", attribute("name")), Condition.LIKE, constant("OT%")), ids(1, 2)),
                Arguments.of("UPPER(name) IN ('JAVA', 'ABC')",
                        compare(function("UPPER", attribute("name")), Condition.IN, constant(List.of("JAVA", "ABC"))),
                        ids(3, 5)));
    }

    static Stream<Arguments> operandPositions() {
        return Stream.of(
                Arguments.of("name = UPPER(otherName)",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids(3)),
                Arguments.of("UPPER(name) = UPPER(otherName)",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids(1, 3, 4, 5)),
                Arguments.of("UPPER(LOWER(name)) = 'OTAVIO'",
                        compare(function("UPPER", nested("LOWER", attribute("name"))),
                                Condition.EQUALS, constant("OTAVIO")), ids(1, 2)),
                Arguments.of("UPPER(name) = UPPER('otavio')",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS,
                                constant("OTAVIO")), ids(1, 2)),
                Arguments.of("name = LOWER(UPPER(otherName))",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("LOWER", nested("UPPER", attribute("otherName")))), ids()),
                Arguments.of("ABS(balance) = ABS(balance)",
                        compare(function("ABS", attribute("balance")), Condition.EQUALS,
                                function("ABS", attribute("balance"))), ids(1, 2, 3, 4, 5)));
    }

    @Nested
    @DisplayName("Given SELECT with native functions")
    class Select {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.orientdb.communication.NativeFunctionCommunicationIntegrationTest#standardPredicates")
        void shouldReturnExactIds(String label, CriteriaCondition condition, Set<Integer> expected) {
            assertThat(accounts(condition)).as(label).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.orientdb.communication.NativeFunctionCommunicationIntegrationTest#operandPositions")
        void shouldReturnExactIdsForOperandPositions(String label, CriteriaCondition condition,
                                                     Set<Integer> expected) {
            assertThat(accounts(condition)).as(label).isEqualTo(expected);
        }

        @Test
        @DisplayName("COUNT uses the same predicate")
        void shouldCount() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            var count = manager.count(new DefaultSelectQuery(0, 0, ACCOUNTS, List.of(), List.of(), condition, true));

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("AND, OR and NOT keep their grouping")
        void shouldKeepBooleanGrouping() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));
            var abs = compare(function("ABS", attribute("balance")), Condition.GREATER_THAN, constant(200));
            var java = CriteriaCondition.eq("name", "JAVA");
            var first = CriteriaCondition.eq(ID, 1);
            var fifth = CriteriaCondition.eq(ID, 5);

            assertSoftly(soft -> {
                soft.assertThat(accounts(upper.or(abs.and(java)))).isEqualTo(ids(1, 2, 3));
                soft.assertThat(accounts(upper.and(first.or(fifth)))).isEqualTo(ids(1));
                soft.assertThat(accounts(upper.or(first.and(fifth)))).isEqualTo(ids(1, 2));
                soft.assertThat(accounts(upper.negate())).isEqualTo(ids(3, 4, 5));
                soft.assertThat(accounts(upper.and(first.negate()))).isEqualTo(ids(2));
            });
        }

        @Test
        @DisplayName("A native predicate can be combined with an ordinary predicate")
        void shouldCombineWithOrdinaryPredicates() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            assertThat(accounts(upper.and(CriteriaCondition.gt("balance", 0)))).isEqualTo(ids(2));
        }
    }

    @Nested
    @DisplayName("Given case and numeric edge cases")
    class EdgeCases {

        private void insert(int id, String name, Object balance) {
            manager.insert(entity(MIXED, id, name, null, balance));
        }

        @Test
        @DisplayName("A missing attribute propagates null")
        void shouldPropagateNull() {
            insert(1, null, null);
            insert(2, "a", 1);

            assertSoftly(soft -> {
                soft.assertThat(select(MIXED, compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("A")))).isEqualTo(ids(2));
                soft.assertThat(select(MIXED, compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant(null)))).isEqualTo(ids(1));
                soft.assertThat(select(MIXED, compare(function("ABS", attribute("balance")),
                        Condition.LESSER_THAN, constant(10)))).isEqualTo(ids(2));
                soft.assertThat(select(MIXED, compare(function("ABS", attribute("balance")),
                        Condition.GREATER_EQUALS_THAN, constant(0)))).isEqualTo(ids(2));
            });
        }

        @Test
        @DisplayName("Unicode case conversion covers non-ASCII letters")
        void shouldConvertUnicodeCase() {
            insert(1, "éa", null);

            assertSoftly(soft -> {
                soft.assertThat(select(MIXED, compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("ÉA")))).isEqualTo(ids(1));
                soft.assertThat(select(MIXED, compare(function("LOWER", attribute("name")), Condition.EQUALS,
                        constant("éa")))).isEqualTo(ids(1));
            });
        }

        @Test
        @DisplayName("An empty string stays empty")
        void shouldKeepEmptyString() {
            insert(1, "", null);

            assertThat(select(MIXED, compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(""))))
                    .isEqualTo(ids(1));
        }

        @Test
        @DisplayName("ABS keeps decimal precision and zero")
        void shouldKeepDecimalPrecision() {
            insert(1, null, -10.25);
            insert(2, null, 0);

            assertSoftly(soft -> {
                soft.assertThat(select(MIXED, compare(function("ABS", attribute("balance")), Condition.EQUALS,
                        constant(10.25)))).isEqualTo(ids(1));
                soft.assertThat(select(MIXED, compare(function("ABS", attribute("balance")), Condition.EQUALS,
                        constant(0)))).isEqualTo(ids(2));
            });
        }

        @Test
        @DisplayName("Constants that look like OrientDB SQL syntax are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("?", "' OR 1=1 --", "\\", "\"quote\"", "'; DELETE FROM " + ACCOUNTS + " --")) {
                var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload));
                assertThat(accounts(condition)).as(payload).isEmpty();
            }
            assertThat(manager.count(ACCOUNTS)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Given DELETE with native functions")
    class Mutations {

        @Test
        void shouldDeleteOnlyMatchingRecords() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            manager.delete(NativeFunctionQueries.delete(ACCOUNTS, condition, List.of()));

            assertThat(accounts(null)).isEqualTo(ids(3, 4, 5));
        }
    }

    @Nested
    @DisplayName("Given evaluable functions")
    class Evaluable {

        @Test
        @DisplayName("An evaluated function reaches the driver as an ordinary value")
        void shouldTreatEvaluatedValueAsOrdinaryConstant() {
            var evaluated = Value.of("OTAVIO");

            assertThat(accounts(CriteriaCondition.eq(Element.of("otherName", evaluated)))).isEqualTo(ids(1));
        }
    }

}
