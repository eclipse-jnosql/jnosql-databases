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
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Oracle NoSQL native functions through the communication API")
class NativeFunctionCommunicationIntegrationTest {

    private static final String ACCOUNTS = "native_function_account";
    private static final String EDGES = "native_function_edge";
    private static final String MIXED = "native_function_mixed";

    private static OracleNoSQLDocumentManager manager;

    @BeforeAll
    static void setUp() throws InterruptedException {
        manager = (OracleNoSQLDocumentManager) Database.INSTANCE.managerFactory().apply("database");
        warmUp();
    }

    // The container rejects the first queries after it starts, for example with an invalid partition id
    private static void warmUp() throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(180).toNanos();
        while (true) {
            try {
                manager.delete(NativeFunctionQueries.delete(ACCOUNTS, null, List.of()));
                manager.count(new DefaultSelectQuery(0, 0, ACCOUNTS, List.of(), List.of(), null, true));
                accounts(compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("x")));
                return;
            } catch (RuntimeException exception) {
                if (System.nanoTime() > deadline) {
                    throw exception;
                }
                Thread.sleep(1_000);
            }
        }
    }

    @BeforeEach
    void insertDataset() {
        clear(ACCOUNTS);
        clear(EDGES);
        clear(MIXED);
        account(1, "Otavio", "OTAVIO", -100);
        account(2, "otavio", "JAVA", 100);
        account(3, "JAVA", "java", -250);
        account(4, "Jakarta", "JAKARTA", 500);
        account(5, "Abc", "ABC", 0);
        edge(1, "😀ab");
        edge(2, "");
        edge(3, null);
        edge(4, "Ab");
    }

    private static void clear(String entity) {
        manager.delete(NativeFunctionQueries.delete(entity, null, List.of()));
    }

    private static void account(int id, String name, String otherName, int balance) {
        var entity = CommunicationEntity.of(ACCOUNTS);
        entity.add("_id", String.valueOf(id));
        entity.add("name", name);
        entity.add("otherName", otherName);
        entity.add("balance", balance);
        manager.insert(entity);
    }

    private static void edge(int id, Object name) {
        var entity = CommunicationEntity.of(EDGES);
        entity.add("_id", String.valueOf(id));
        if (name != null) {
            entity.add("name", name);
        }
        manager.insert(entity);
    }

    private static void mixed(String id, Object name) {
        var entity = CommunicationEntity.of(MIXED);
        entity.add("_id", id);
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

    private static Set<String> select(String entity, CriteriaCondition condition) {
        var query = new DefaultSelectQuery(0, 0, entity, List.of(), List.of(), condition, false);
        try (Stream<CommunicationEntity> result = manager.select(query)) {
            return result.map(e -> e.find("_id").orElseThrow().get(String.class))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> accounts(CriteriaCondition condition) {
        return select(ACCOUNTS, condition);
    }

    private static Set<String> edges(CriteriaCondition condition) {
        return select(EDGES, condition);
    }

    private static Set<String> mixedRecords(CriteriaCondition condition) {
        return select(MIXED, condition);
    }

    private static Set<String> ids(String... ids) {
        return new TreeSet<>(Set.of(ids));
    }

    static Stream<Arguments> standardPredicates() {
        return Stream.of(
                Arguments.of("UPPER(name) = 'OTAVIO'",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO")),
                        ids("1", "2")),
                Arguments.of("LOWER(name) = 'java'",
                        compare(function("LOWER", attribute("name")), Condition.EQUALS, constant("java")), ids("3")),
                Arguments.of("LENGTH(name) = 6",
                        compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(6)), ids("1", "2")),
                Arguments.of("LEFT(name, 3) = 'Ota'",
                        compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                                Condition.EQUALS, constant("Ota")), ids("1")),
                Arguments.of("RIGHT(name, 3) = 'vio'",
                        compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                                Condition.EQUALS, constant("vio")), ids("1", "2")),
                Arguments.of("ABS(balance) = 100",
                        compare(function("ABS", attribute("balance")), Condition.EQUALS, constant(100)),
                        ids("1", "2")),
                Arguments.of("ABS(balance) > 100",
                        compare(function("ABS", attribute("balance")), Condition.GREATER_THAN, constant(100)),
                        ids("3", "4")),
                Arguments.of("ABS(balance) >= 100",
                        compare(function("ABS", attribute("balance")), Condition.GREATER_EQUALS_THAN,
                                constant(100)), ids("1", "2", "3", "4")),
                Arguments.of("ABS(balance) < 100",
                        compare(function("ABS", attribute("balance")), Condition.LESSER_THAN, constant(100)),
                        ids("5")),
                Arguments.of("ABS(balance) <= 100",
                        compare(function("ABS", attribute("balance")), Condition.LESSER_EQUALS_THAN,
                                constant(100)), ids("1", "2", "5")),
                Arguments.of("UPPER(name) LIKE 'OT%'",
                        compare(function("UPPER", attribute("name")), Condition.LIKE, constant("OT%")), ids("1", "2")),
                Arguments.of("LOWER(name) LIKE '_ava'",
                        compare(function("LOWER", attribute("name")), Condition.LIKE, constant("_ava")), ids("3")),
                Arguments.of("UPPER(name) IN ('JAVA', 'ABC')",
                        compare(function("UPPER", attribute("name")), Condition.IN, constant(List.of("JAVA", "ABC"))),
                        ids("3", "5")),
                Arguments.of("ABS(balance) BETWEEN 100 AND 250",
                        compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                                constant(List.of(100, 250))), ids("1", "2", "3")));
    }

    static Stream<Arguments> operandPositions() {
        return Stream.of(
                Arguments.of("name = UPPER(otherName)",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids("3")),
                Arguments.of("UPPER(name) = UPPER(otherName)",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS,
                                function("UPPER", attribute("otherName"))), ids("1", "3", "4", "5")),
                Arguments.of("UPPER(LEFT(name, 3)) = 'OTA'",
                        compare(function("UPPER", nested("LEFT", attribute("name"), FunctionArgument.literal(3))),
                                Condition.EQUALS, constant("OTA")), ids("1", "2")),
                Arguments.of("LEFT(name, LENGTH(:prefix)) = :prefix with an evaluated length",
                        compare(function("LEFT", attribute("name"), value(3)), Condition.EQUALS, constant("Ota")),
                        ids("1")),
                Arguments.of("name = LEFT(name, LENGTH(otherName))",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("LEFT", attribute("name"), nested("LENGTH", attribute("otherName")))),
                        ids("1", "3", "4", "5")),
                Arguments.of("name = UPPER(LEFT(otherName, 4))",
                        compare(attributeOperand("name"), Condition.EQUALS,
                                function("UPPER", nested("LEFT", attribute("otherName"),
                                        FunctionArgument.literal(4)))), ids("3")),
                Arguments.of("LENGTH(name) = LENGTH(otherName)",
                        compare(function("LENGTH", attribute("name")), Condition.EQUALS,
                                function("LENGTH", attribute("otherName"))), ids("1", "3", "4", "5")),
                Arguments.of("LEFT('Otavio', LENGTH(name)) = name (text from a value)",
                        compare(function("LEFT", value("Otavio"), nested("LENGTH", attribute("name"))),
                                Condition.EQUALS, attributeOperand("name")), ids("1")),
                Arguments.of("RIGHT('Otavio', LENGTH(name)) = name (text from a value)",
                        compare(function("RIGHT", value("Otavio"), nested("LENGTH", attribute("name"))),
                                Condition.EQUALS, attributeOperand("name")), ids("1")));
    }

    @Nested
    @DisplayName("Given SELECT with native functions")
    class Select {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.communication.NativeFunctionCommunicationIntegrationTest#standardPredicates")
        void shouldReturnExactIds(String label, CriteriaCondition condition, Set<String> expected) {
            assertThat(accounts(condition)).as(label).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.communication.NativeFunctionCommunicationIntegrationTest#operandPositions")
        void shouldReturnExactIdsForOperandPositions(String label, CriteriaCondition condition,
                                                     Set<String> expected) {
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
        @DisplayName("COUNT of a negated native predicate counts only the other records")
        void shouldCountNegation() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"))
                    .negate();

            var count = manager.count(new DefaultSelectQuery(0, 0, ACCOUNTS, List.of(), List.of(), condition, true));

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Entities of another name are not selected")
        void shouldIgnoreOtherEntities() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("AB"));

            assertSoftly(soft -> {
                soft.assertThat(accounts(condition)).isEmpty();
                soft.assertThat(edges(condition)).isEqualTo(ids("4"));
            });
        }

        @Test
        @DisplayName("AND, OR and NOT keep their grouping")
        void shouldKeepBooleanGrouping() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));
            var abs = compare(function("ABS", attribute("balance")), Condition.EQUALS, constant(100));
            var first = CriteriaCondition.eq("balance", -100);
            var fifth = CriteriaCondition.eq("balance", 0);

            assertSoftly(soft -> {
                soft.assertThat(accounts(upper.and(first.or(fifth)))).isEqualTo(ids("1"));
                soft.assertThat(accounts(upper.or(first.and(fifth)))).isEqualTo(ids("1", "2"));
                soft.assertThat(accounts(upper.negate())).isEqualTo(ids("3", "4", "5"));
                soft.assertThat(accounts(upper.and(abs.negate()))).isEqualTo(ids());
                soft.assertThat(accounts(upper.and(first.negate()))).isEqualTo(ids("2"));
                soft.assertThat(accounts(upper.or(abs).or(fifth))).isEqualTo(ids("1", "2", "5"));
                soft.assertThat(accounts(CriteriaCondition.and(upper, abs))).isEqualTo(ids("1", "2"));
                soft.assertThat(accounts(upper.negate().negate())).isEqualTo(ids("1", "2"));
            });
        }

        @Test
        @DisplayName("A native predicate can be combined with an ordinary predicate")
        void shouldCombineWithOrdinaryPredicates() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            assertSoftly(soft -> {
                soft.assertThat(accounts(upper.and(CriteriaCondition.gt("balance", 0)))).isEqualTo(ids("2"));
                soft.assertThat(accounts(upper.and(CriteriaCondition.eq("name", "Otavio")))).isEqualTo(ids("1"));
                soft.assertThat(accounts(upper.and(CriteriaCondition.like("otherName", "J%")))).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("The _id attribute inside a native function is not supported")
        void shouldRejectIdAttribute() {
            var condition = compare(function("UPPER", attribute("_id")), Condition.EQUALS, constant("1"));

            assertThatThrownBy(() -> accounts(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("An id equality combined with a native predicate does not bypass the native predicate")
        void shouldNotBypassNativePredicateWithIdLookup() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            assertSoftly(soft -> {
                soft.assertThat(accounts(upper.and(CriteriaCondition.eq("_id", "1")))).isEqualTo(ids("1"));
                soft.assertThat(accounts(upper.and(CriteriaCondition.eq("_id", "3")))).isEmpty();
                soft.assertThat(accounts(upper.and(CriteriaCondition.in("_id", List.of("1", "3", "5")))))
                        .isEqualTo(ids("1"));
                soft.assertThat(accounts(CriteriaCondition.eq("_id", "3").and(upper))).isEmpty();
                soft.assertThat(accounts(upper.or(CriteriaCondition.eq("_id", "5")))).isEqualTo(ids("1", "2", "5"));
                soft.assertThat(accounts(upper.negate().and(CriteriaCondition.eq("_id", "1")))).isEmpty();
            });
        }

        @Test
        @DisplayName("The same condition can be executed repeatedly")
        void shouldExecuteRepeatedly() {
            var condition = compare(function("LEFT", attribute("name"), value(3)), Condition.EQUALS,
                    constant("Ota"));

            assertSoftly(soft -> {
                soft.assertThat(accounts(condition)).isEqualTo(ids("1"));
                soft.assertThat(accounts(condition)).isEqualTo(ids("1"));
            });
        }

        @Test
        @DisplayName("A native predicate honors limit and sort")
        void shouldHonorLimitAndSort() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));
            var query = new DefaultSelectQuery(1, 0, ACCOUNTS, List.of(),
                    List.of(jakarta.data.Sort.desc("balance")), condition, false);

            try (var result = manager.select(query)) {
                assertThat(result.map(e -> e.find("_id").orElseThrow().get(String.class)).toList())
                        .containsExactly("2");
            }
        }
    }

    @Nested
    @DisplayName("Given LEFT, RIGHT and LENGTH edge cases")
    class EdgeCases {

        @Test
        @DisplayName("LENGTH counts Unicode code points, not UTF-16 units")
        void shouldCountCodePoints() {
            assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(3))))
                    .isEqualTo(ids("1"));
        }

        @Test
        @DisplayName("LEFT and RIGHT slice whole code points")
        void shouldSliceCodePoints() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(1)),
                        Condition.EQUALS, constant("😀")))).isEqualTo(ids("1"));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(2)),
                        Condition.EQUALS, constant("😀a")))).isEqualTo(ids("1"));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(2)),
                        Condition.EQUALS, constant("ab")))).isEqualTo(ids("1"));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                        Condition.EQUALS, constant("😀ab")))).isEqualTo(ids("1"));
            });
        }

        @Test
        @DisplayName("A zero length gives an empty string and an excessive length gives the whole string")
        void shouldHandleBoundaryLengths() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(0)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids("1", "2", "4"));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(0)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids("1", "2", "4"));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(99)),
                        Condition.EQUALS, constant("Ab")))).isEqualTo(ids("4"));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(99)),
                        Condition.EQUALS, constant("Ab")))).isEqualTo(ids("4"));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(4)),
                        Condition.EQUALS, constant("😀ab")))).isEqualTo(ids("1"));
            });
        }

        @Test
        @DisplayName("An empty string has length zero and keeps empty for LEFT and RIGHT")
        void shouldHandleEmptyString() {
            assertSoftly(soft -> {
                soft.assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.EQUALS,
                        constant(0)))).isEqualTo(ids("2"));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(2)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids("2"));
                soft.assertThat(edges(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(2)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids("2"));
                soft.assertThat(edges(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("")))).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("A null or missing attribute propagates null")
        void shouldPropagateNull() {
            var upper = function("UPPER", attribute("name"));

            assertSoftly(soft -> {
                soft.assertThat(edges(compare(upper, Condition.EQUALS, constant(null)))).isEqualTo(ids("3"));
                soft.assertThat(edges(compare(function("LENGTH", attribute("name")), Condition.LESSER_THAN,
                        constant(10)))).isEqualTo(ids("1", "2", "4"));
                soft.assertThat(edges(compare(function("LEFT", attribute("name"), FunctionArgument.literal(1)),
                        Condition.EQUALS, constant("")))).isEqualTo(ids("2"));
                soft.assertThat(edges(compare(upper, Condition.EQUALS, constant("AB")))).isEqualTo(ids("4"));
                soft.assertThat(edges(compare(upper, Condition.EQUALS, constant(null)).negate()))
                        .isEqualTo(ids("1", "2", "4"));
            });
        }

        @Test
        @DisplayName("A function that receives a null value results in null")
        void shouldPropagateNullArgument() {
            var condition = compare(function("LEFT", attribute("name"), FunctionArgument.literal(null)),
                    Condition.EQUALS, constant(null));

            assertThat(edges(condition)).isEqualTo(ids("1", "2", "3", "4"));
        }

        @Test
        @DisplayName("A non-string attribute makes the string function null instead of failing")
        void shouldNotMatchNonStringAttribute() {
            mixed("1", 42);
            mixed("2", "42");
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("42"));
            var length = compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(2));
            var left = compare(function("LEFT", attribute("name"), FunctionArgument.literal(1)), Condition.EQUALS,
                    constant("4"));

            assertSoftly(soft -> {
                soft.assertThat(mixedRecords(upper)).isEqualTo(ids("2"));
                soft.assertThat(mixedRecords(length)).isEqualTo(ids("2"));
                soft.assertThat(mixedRecords(left)).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("A non-numeric attribute makes ABS null instead of failing")
        void shouldNotMatchNonNumericAbs() {
            mixed("1", "text");
            mixed("2", -5);

            assertThat(mixedRecords(compare(function("ABS", attribute("name")), Condition.EQUALS, constant(5))))
                    .isEqualTo(ids("2"));
        }

        @Test
        @DisplayName("UPPER and LOWER convert non-ASCII letters")
        void shouldConvertNonAsciiCase() {
            mixed("1", "éa");
            mixed("2", "É");

            assertSoftly(soft -> {
                soft.assertThat(mixedRecords(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("ÉA")))).isEqualTo(ids("1"));
                soft.assertThat(mixedRecords(compare(function("LOWER", attribute("name")), Condition.EQUALS,
                        constant("é")))).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("Known limitation: ABS does not report an overflow")
        void shouldNotReportAbsOverflow() {
            mixed("1", Long.MIN_VALUE);

            assertThat(mixedRecords(compare(function("ABS", attribute("name")), Condition.LESSER_THAN,
                    constant(0)))).isEqualTo(ids("1"));
        }

        @Test
        @DisplayName("A negative length is rejected before the query is executed")
        void shouldRejectNegativeLength() {
            var condition = compare(function("LEFT", attribute("name"), value(-1)), Condition.EQUALS, constant("a"));

            assertThatThrownBy(() -> accounts(condition)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Constants that look like SQL syntax are values")
        void shouldTreatPayloadsAsValues() {
            for (String payload : List.of("?", "$name", "' || 1 = 1", "\\", "\"quote\"", "'",
                    "{\"$where\": \"1\"}", "'; DROP TABLE native_function_account; --", "`", "é😀")) {
                var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload));
                assertThat(accounts(condition)).as(payload).isEmpty();
            }
            assertThat(accounts(null)).isEqualTo(ids("1", "2", "3", "4", "5"));
        }

        @Test
        @DisplayName("Quotes and backslashes in stored values are matched as values")
        void shouldMatchStoredQuotes() {
            mixed("1", "o'neil");
            mixed("2", "back\\slash");

            assertSoftly(soft -> {
                soft.assertThat(mixedRecords(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("O'NEIL")))).isEqualTo(ids("1"));
                soft.assertThat(mixedRecords(compare(function("LENGTH", attribute("name")), Condition.EQUALS,
                        constant(10)))).isEqualTo(ids("2"));
                soft.assertThat(mixedRecords(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                        constant("BACK\\SLASH")))).isEqualTo(ids("2"));
            });
        }

        @Test
        @DisplayName("A hostile attribute name is rejected")
        void shouldRejectHostileAttribute() {
            var condition = compare(function("UPPER", attribute("name) OR (1 = 1")), Condition.EQUALS,
                    constant("x"));

            assertThatThrownBy(() -> accounts(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given DELETE and UPDATE with native functions")
    class Mutations {

        @Test
        void shouldDeleteOnlyMatchingRecords() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            var deleted = manager.deleteAndCount(NativeFunctionQueries.delete(ACCOUNTS, condition, List.of()));

            assertSoftly(soft -> {
                soft.assertThat(deleted).isEqualTo(2);
                soft.assertThat(accounts(null)).isEqualTo(ids("3", "4", "5"));
                soft.assertThat(edges(null)).isEqualTo(ids("1", "2", "3", "4"));
            });
        }

        @Test
        @DisplayName("DELETE with an id predicate does not bypass the native predicate")
        void shouldNotDeleteByIdWhenNativePredicateDoesNotMatch() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            var deleted = manager.deleteAndCount(NativeFunctionQueries.delete(ACCOUNTS,
                    upper.and(CriteriaCondition.eq("_id", "3")), List.of()));

            assertSoftly(soft -> {
                soft.assertThat(deleted).isZero();
                soft.assertThat(accounts(null)).isEqualTo(ids("1", "2", "3", "4", "5"));
            });
        }

        @Test
        void shouldDeleteWithIdPredicateAndNativePredicate() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

            var deleted = manager.deleteAndCount(NativeFunctionQueries.delete(ACCOUNTS,
                    upper.and(CriteriaCondition.eq("_id", "1")), List.of()));

            assertSoftly(soft -> {
                soft.assertThat(deleted).isEqualTo(1);
                soft.assertThat(accounts(null)).isEqualTo(ids("2", "3", "4", "5"));
            });
        }

        @Test
        void shouldDeleteWithNegatedPredicate() {
            var condition = compare(function("ABS", attribute("balance")), Condition.GREATER_THAN, constant(100))
                    .negate();

            manager.delete(NativeFunctionQueries.delete(ACCOUNTS, condition, List.of()));

            assertThat(accounts(null)).isEqualTo(ids("3", "4"));
        }

        @Test
        void shouldUpdateOnlyMatchingRecords() {
            var condition = compare(function("LOWER", attribute("name")), Condition.EQUALS, constant("java"));

            manager.update(NativeFunctionQueries.update(ACCOUNTS, List.of(Element.of("flag", true)), condition));

            assertSoftly(soft -> {
                soft.assertThat(accounts(CriteriaCondition.eq("flag", true))).isEqualTo(ids("3"));
                soft.assertThat(accounts(null)).isEqualTo(ids("1", "2", "3", "4", "5"));
            });
        }

        @Test
        void shouldRejectUnsupportedDeleteWithoutDeleting() {
            var condition = compare(function("SOUNDEX", attribute("name")), Condition.EQUALS, constant("O300"));

            assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete(ACCOUNTS, condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Oracle NoSQL").hasMessageContaining("SOUNDEX");
            assertThat(accounts(null)).isEqualTo(ids("1", "2", "3", "4", "5"));
        }

        @Test
        void shouldRejectUnsupportedDeleteWithIdPredicateWithoutDeleting() {
            var condition = compare(function("LEFT", attribute("name"), attribute("balance")), Condition.EQUALS,
                    constant("O")).and(CriteriaCondition.eq("_id", "1"));

            assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete(ACCOUNTS, condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(accounts(null)).isEqualTo(ids("1", "2", "3", "4", "5"));
        }

        @Test
        void shouldRejectUnsupportedUpdateWithoutUpdating() {
            var condition = compare(function("LEFT", attribute("name"), attribute("balance")), Condition.EQUALS,
                    constant("O"));

            assertThatThrownBy(() -> manager.update(NativeFunctionQueries.update(ACCOUNTS,
                    List.of(Element.of("flag", true)), condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(accounts(CriteriaCondition.eq("flag", true))).isEmpty();
        }

        @Test
        void shouldRejectUnsupportedSelect() {
            var condition = compare(function("SOUNDEX", attribute("name")), Condition.EQUALS, constant("O300"));

            assertThatThrownBy(() -> accounts(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldRejectUnsupportedCount() {
            var condition = compare(function("LIKE_NUMERIC", attribute("name")), Condition.EQUALS, constant("O300"));

            assertThatThrownBy(() -> manager.count(new DefaultSelectQuery(0, 0, ACCOUNTS, List.of(), List.of(),
                    condition, true))).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Given evaluable functions")
    class Evaluable {

        @Test
        @DisplayName("An evaluated function reaches the driver as an ordinary value")
        void shouldTreatEvaluatedValueAsOrdinaryConstant() {
            var evaluated = Value.of("OTAVIO");

            assertThat(accounts(CriteriaCondition.eq(Element.of("otherName", evaluated)))).isEqualTo(ids("1"));
        }

        @Test
        @DisplayName("A constant argument keeps the same result as the stored attribute")
        void shouldBeTransparentForConstants() {
            var viaAttribute = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("JAVA"));
            var viaValue = compare(function("UPPER", nested("LEFT", value("java"),
                    nested("LENGTH", attribute("name")))), Condition.EQUALS, attributeOperand("name"));

            assertSoftly(soft -> {
                soft.assertThat(accounts(viaAttribute)).isEqualTo(ids("3"));
                soft.assertThat(accounts(viaValue)).isEqualTo(ids("3"));
            });
        }
    }
}
