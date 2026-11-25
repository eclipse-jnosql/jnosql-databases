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
package org.eclipse.jnosql.databases.influxdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

/**
 * InfluxDB 3 has no delete operation, so the fixtures are written once with fixed timestamps and are only read.
 */
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("InfluxDB native functions through the communication API")
class NativeFunctionCommunicationIntegrationTest {

    private static final String ACCOUNTS = "native_function_account";
    private static final String EDGES = "native_function_edge";
    private static final String NUMBERS = "native_function_number";
    private static final String TEXTS = "native_function_text";
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private static InfluxDBTimeSeriesManager manager;

    @BeforeAll
    static void setUp() {
        manager = InfluxDBDatabase.INSTANCE.manager();
        account(1, "Otavio", "OTAVIO", -100);
        account(2, "otavio", "JAVA", 100);
        account(3, "JAVA", "java", -250);
        account(4, "Jakarta", "JAKARTA", 500);
        account(5, "Abc", "ABC", 0);
        edge(1, "😀ab");
        edge(2, "");
        edge(3, null);
        edge(4, "Ab");
        edge(5, "éa");
        number(1, -10.25);
        number(2, 0.0);
        text(1, "42");
    }

    private static CommunicationEntity point(String measurement, int id) {
        var entity = CommunicationEntity.of(measurement);
        entity.add("_id", BASE.plusSeconds(id));
        return entity;
    }

    private static void account(int id, String name, String otherName, int balance) {
        var entity = point(ACCOUNTS, id);
        entity.add("name", name);
        entity.add("otherName", otherName);
        entity.add("balance", balance);
        manager.insert(entity);
    }

    private static void edge(int id, String name) {
        var entity = point(EDGES, id);
        entity.add("marker", "x");
        if (name != null) {
            entity.add("name", name);
        }
        manager.insert(entity);
    }

    private static void number(int id, double balance) {
        var entity = point(NUMBERS, id);
        entity.add("balance", balance);
        manager.insert(entity);
    }

    private static void text(int id, String name) {
        var entity = point(TEXTS, id);
        entity.add("balance", 1);
        entity.add("name", name);
        manager.insert(entity);
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument nested(String name, FunctionArgument... arguments) {
        return new FunctionArgument.NativeArgument(NativeFunction.of(name, arguments));
    }

    private static FunctionArgument value(Object value) {
        return new FunctionArgument.ValueArgument(Value.of(value));
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

    private static Set<Integer> select(String measurement, CriteriaCondition condition) {
        return select(new DefaultSelectQuery(0, 0, measurement, List.of(), List.of(), condition, false));
    }

    private static Set<Integer> select(DefaultSelectQuery query) {
        try (Stream<CommunicationEntity> result = manager.select(query)) {
            return result.map(e -> (int) Duration.between(BASE, e.find("_id", Instant.class).orElseThrow())
                    .getSeconds()).collect(Collectors.toSet());
        }
    }

    private static Set<Integer> accounts(CriteriaCondition condition) {
        return select(ACCOUNTS, condition);
    }

    private static Set<Integer> edges(CriteriaCondition condition) {
        return select(EDGES, condition);
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
                Arguments.of("LENGTH(name) = 6",
                        compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(6)), ids(1, 2)),
                Arguments.of("LEFT(name, 3) = 'Ota'",
                        compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                                Condition.EQUALS, constant("Ota")), ids(1)),
                Arguments.of("RIGHT(name, 3) = 'vio'",
                        compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                                Condition.EQUALS, constant("vio")), ids(1, 2)),
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
                Arguments.of("UPPER(name) IN ('JAVA', 'ABC')",
                        compare(function("UPPER", attribute("name")), Condition.IN, constant(List.of("JAVA", "ABC"))),
                        ids(3, 5)),
                Arguments.of("ABS(balance) BETWEEN 100 AND 250",
                        compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                                constant(List.of(100, 250))), ids(1, 2, 3)));
    }

    static Stream<Arguments> operandPositions() {
        return Stream.of(
                Arguments.of("name = UPPER(otherName)",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids(3)),
                Arguments.of("UPPER(name) = UPPER(otherName)",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids(1, 3, 4, 5)),
                Arguments.of("UPPER(LEFT(name, 3)) = 'OTA'",
                        compare(function("UPPER", nested("LEFT", attribute("name"), FunctionArgument.literal(3))),
                                Condition.EQUALS, constant("OTA")), ids(1, 2)),
                Arguments.of("UPPER(name) = UPPER('otavio')",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO")),
                        ids(1, 2)),
                Arguments.of("LEFT(name, LENGTH('Ota')) = 'Ota'",
                        compare(function("LEFT", attribute("name"), value(3)), Condition.EQUALS, constant("Ota")),
                        ids(1)),
                Arguments.of("name = LEFT(name, LENGTH(otherName))",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("LEFT", attribute("name"), nested("LENGTH", attribute("otherName")))),
                        ids(1, 3, 4, 5)),
                Arguments.of("name = UPPER(LEFT(otherName, 4))",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("UPPER", nested("LEFT", attribute("otherName"),
                                        FunctionArgument.literal(4)))), ids(3)));
    }

    @Nested
    @DisplayName("Given SELECT with native functions")
    class Select {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.influxdb.communication.NativeFunctionCommunicationIntegrationTest#standardPredicates")
        void shouldReturnExactIds(String label, CriteriaCondition condition, Set<Integer> expected) {
            assertThat(accounts(condition)).as(label).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.influxdb.communication.NativeFunctionCommunicationIntegrationTest#operandPositions")
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
            var first = CriteriaCondition.eq("_id", BASE.plusSeconds(1));
            var fifth = CriteriaCondition.eq("_id", BASE.plusSeconds(5));

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
    @DisplayName("Given temporal restrictions")
    class Temporal {

        private final CriteriaCondition upper = compare(function("UPPER", attribute("name")), Condition.EQUALS,
                constant("OTAVIO"));

        @Test
        @DisplayName("A time range is preserved next to a native predicate")
        void shouldPreserveTimeRange() {
            assertSoftly(soft -> {
                soft.assertThat(accounts(upper.and(CriteriaCondition.gt("_id", BASE.plusSeconds(1)))))
                        .isEqualTo(ids(2));
                soft.assertThat(accounts(upper.and(CriteriaCondition.lte("_id", BASE.plusSeconds(1)))))
                        .isEqualTo(ids(1));
                soft.assertThat(accounts(upper.and(CriteriaCondition.gte("_id", BASE.plusSeconds(2)))
                        .and(CriteriaCondition.lte("_id", BASE.plusSeconds(4))))).isEqualTo(ids(2));
                soft.assertThat(accounts(upper.and(CriteriaCondition.gt("_id", BASE.plusSeconds(2))))).isEmpty();
            });
        }

        @Test
        @DisplayName("Ordering and limit apply after the native predicate")
        void shouldKeepOrderingAndLimit() {
            var condition = compare(function("ABS", attribute("balance")), Condition.GREATER_EQUALS_THAN,
                    constant(100));

            var query = new DefaultSelectQuery(2, 0, ACCOUNTS, List.of(), List.of(Sort.desc("_id")), condition,
                    false);

            assertThat(select(query)).isEqualTo(ids(4, 3));
        }

        @Test
        @DisplayName("The temporal identifier cannot be a function operand")
        void shouldRejectTemporalIdentifier() {
            var condition = compare(function("UPPER", attribute("_id")), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> accounts(condition)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Given string and numeric edge cases")
    class EdgeCases {

        @Test
        @DisplayName("LENGTH counts Unicode characters, not UTF-16 units")
        void shouldCountCodePoints() {
            assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(3))))
                    .isEqualTo(ids(1));
        }

        @Test
        @DisplayName("LEFT and RIGHT slice whole code points")
        void shouldSliceCodePoints() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(1)),
                        Condition.EQUALS, constant("😀")))).isEqualTo(ids(1));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(2)),
                        Condition.EQUALS, constant("ab")))).isEqualTo(ids(1));
            });
        }

        @Test
        @DisplayName("A zero length gives an empty string and an excessive length gives the whole string")
        void shouldHandleBoundaryLengths() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(0)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids(1, 2, 4, 5));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(0)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids(1, 2, 4, 5));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(99)),
                        Condition.EQUALS, constant("Ab")))).isEqualTo(ids(4));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(99)),
                        Condition.EQUALS, constant("Ab")))).isEqualTo(ids(4));
            });
        }

        @Test
        @DisplayName("An empty string has length zero")
        void shouldHandleEmptyString() {
            assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(0))))
                    .isEqualTo(ids(2));
        }

        @Test
        @DisplayName("A missing field propagates null")
        void shouldPropagateNull() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.LESSER_THAN,
                        constant(10)))).isEqualTo(ids(1, 2, 4, 5));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(1)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids(2));
                soft.assertThat(edges(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("")))).isEqualTo(ids(2));
            });
        }

        @Test
        @DisplayName("Unicode case conversion covers non-ASCII letters")
        void shouldConvertUnicodeCase() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("ÉA")))).isEqualTo(ids(5));
                soft.assertThat(edges(compare(function("LOWER", attribute("name")), Condition.EQUALS,
                        constant("éa")))).isEqualTo(ids(5));
            });
        }

        @Test
        @DisplayName("ABS keeps decimal precision and zero")
        void shouldKeepDecimalPrecision() {
            assertSoftly(soft -> {
                soft.assertThat(select(NUMBERS, compare(function("ABS", attribute("balance")), Condition.EQUALS,
                        constant(10.25)))).isEqualTo(ids(1));
                soft.assertThat(select(NUMBERS, compare(function("ABS", attribute("balance")), Condition.EQUALS,
                        constant(0.0)))).isEqualTo(ids(2));
            });
        }

        @Test
        @DisplayName("A string function on a numeric column is rejected by the database")
        void shouldRejectIncompatibleColumnType() {
            var condition = compare(function("UPPER", attribute("balance")), Condition.EQUALS, constant("1"));

            assertThatThrownBy(() -> select(NUMBERS, condition)).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Constants that look like SQL syntax are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("$p1", "' OR 1=1 --", "\\", "\"quote\"", "'; DROP TABLE x; --")) {
                var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload));
                assertThat(accounts(condition)).as(payload).isEmpty();
            }
            assertThat(manager.count(ACCOUNTS)).isEqualTo(5);
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
