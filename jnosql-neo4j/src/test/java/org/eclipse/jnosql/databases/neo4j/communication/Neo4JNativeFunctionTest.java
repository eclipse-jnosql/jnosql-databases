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
package org.eclipse.jnosql.databases.neo4j.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultUpdateQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Neo4j native function translation to Cypher")
class Neo4JNativeFunctionTest {

    private static FunctionArgument attr(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument value(Object value) {
        return new FunctionArgument.ValueArgument(org.eclipse.jnosql.communication.Value.of(value));
    }

    private static CriteriaOperand fn(String name, FunctionArgument... args) {
        return new CriteriaOperand.Function(NativeFunction.of(name, args));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(org.eclipse.jnosql.communication.Value.of(value));
    }

    private static CriteriaCondition eq(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private static String select(CriteriaCondition condition, Map<String, Object> parameters) {
        return Neo4JQueryBuilder.INSTANCE.buildQuery(
                new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false), parameters);
    }

    static Stream<Arguments> supported() {
        var nameAttr = new CriteriaOperand.Attribute("name");
        return Stream.of(
                Arguments.of("ABS", eq(fn("ABS", attr("balance")), constant(3)),
                        "MATCH (e:person) WHERE (abs(e.balance) = $nf) RETURN e", Map.of("nf", 3)),
                Arguments.of("LENGTH", eq(fn("LENGTH", attr("name")), constant(3)),
                        "MATCH (e:person) WHERE (char_length(e.name) = $nf) RETURN e", Map.of("nf", 3)),
                Arguments.of("LOWER", eq(fn("LOWER", attr("name")), constant("ota")),
                        "MATCH (e:person) WHERE (toLower(e.name) = $nf) RETURN e", Map.of("nf", "ota")),
                Arguments.of("UPPER", eq(fn("UPPER", attr("name")), constant("OTA")),
                        "MATCH (e:person) WHERE (toUpper(e.name) = $nf) RETURN e", Map.of("nf", "OTA")),
                Arguments.of("native on the right", eq(nameAttr, fn("UPPER", attr("other"))),
                        "MATCH (e:person) WHERE (e.name = toUpper(e.other)) RETURN e", Map.of()),
                Arguments.of("both sides", eq(fn("UPPER", attr("first")), fn("UPPER", attr("last"))),
                        "MATCH (e:person) WHERE (toUpper(e.first) = toUpper(e.last)) RETURN e", Map.of()),
                Arguments.of("nested", eq(fn("UPPER", new FunctionArgument.NativeArgument(
                                NativeFunction.of("LOWER", attr("name")))), constant("OTA")),
                        "MATCH (e:person) WHERE (toUpper(toLower(e.name)) = $nf) RETURN e", Map.of("nf", "OTA")),
                Arguments.of("mixed with an evaluated value", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "MATCH (e:person) WHERE (toUpper(e.name) = $nf) RETURN e", Map.of("nf", "OTAVIO")),
                Arguments.of("greater than", CriteriaCondition.of(fn("ABS", attr("balance")),
                                Condition.GREATER_THAN, constant(2)),
                        "MATCH (e:person) WHERE (abs(e.balance) > $nf) RETURN e", Map.of("nf", 2)),
                Arguments.of("null comparison", eq(fn("UPPER", attr("name")), constant(null)),
                        "MATCH (e:person) WHERE (toUpper(e.name) IS NULL) RETURN e", Map.of()),
                Arguments.of("IN", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.IN,
                                constant(List.of("A", "B"))),
                        "MATCH (e:person) WHERE (toUpper(e.name) IN $nf) RETURN e", Map.of("nf", List.of("A", "B"))),
                Arguments.of("BETWEEN", CriteriaCondition.of(fn("ABS", attr("balance")), Condition.BETWEEN,
                                constant(List.of(1, 5))),
                        "MATCH (e:person) WHERE (abs(e.balance) >= $nf AND abs(e.balance) <= $nf_1) RETURN e",
                        Map.of("nf", 1, "nf_1", 5)),
                Arguments.of("AND/OR/NOT grouping",
                        CriteriaCondition.and(CriteriaCondition.or(eq(fn("UPPER", attr("name")), constant("A")),
                                        CriteriaCondition.eq(Element.of("age", 1))),
                                eq(fn("LOWER", attr("name")), constant("b")).negate()),
                        "MATCH (e:person) WHERE (((toUpper(e.name) = $nf) OR e.age = $age) AND NOT ((toLower(e.name) = $nf_1))) RETURN e",
                        Map.of("nf", "A", "age", 1, "nf_1", "b")));
    }

    @Nested
    @DisplayName("Given supported native functions")
    class Supported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.neo4j.communication.Neo4JNativeFunctionTest#supported")
        void shouldTranslate(String name, CriteriaCondition condition, String cypher, Map<String, Object> expected) {
            Map<String, Object> parameters = new HashMap<>();

            var actual = select(condition, parameters);

            assertSoftly(soft -> {
                soft.assertThat(actual).isEqualTo(cypher);
                soft.assertThat(parameters).isEqualTo(expected);
            });
        }

        @Test
        @DisplayName("Count, delete and update use the same expression")
        void shouldTranslateOtherOperations() {
            var condition = eq(fn("UPPER", attr("name")), constant("OTA"));
            Map<String, Object> count = new HashMap<>();
            Map<String, Object> delete = new HashMap<>();
            Map<String, Object> update = new HashMap<>();

            var countQuery = Neo4JQueryBuilder.INSTANCE.buildCountQuery(
                    new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true), count);
            var deleteQuery = Neo4JQueryBuilder.INSTANCE.buildQuery(
                    new DefaultDeleteQuery("person", condition, List.of()), delete);
            var updateQuery = Neo4JQueryBuilder.INSTANCE.buildQuery(
                    new DefaultUpdateQuery("person", List.of(Element.of("flag", true)), condition), update);

            assertSoftly(soft -> {
                soft.assertThat(countQuery).isEqualTo("MATCH (e:person) WHERE (toUpper(e.name) = $nf) RETURN COUNT(e) as count");
                soft.assertThat(deleteQuery).isEqualTo("MATCH (e:person) WHERE (toUpper(e.name) = $nf) DELETE e");
                soft.assertThat(updateQuery).isEqualTo("MATCH (e:person) WHERE (toUpper(e.name) = $nf) SET e.flag = $flag");
                soft.assertThat(update).containsEntry("nf", "OTA").containsEntry("flag", true);
            });
        }

        @ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"x') DETACH DELETE e //", "a\\'b", "$nf", "x\" OR 1=1"})
        @DisplayName("Hostile values are bound, never interpolated")
        void shouldBindInjectionPayloads(String payload) {
            Map<String, Object> parameters = new HashMap<>();

            var cypher = select(eq(fn("UPPER", attr("name")), constant(payload)), parameters);

            assertSoftly(soft -> {
                soft.assertThat(cypher).isEqualTo("MATCH (e:person) WHERE (toUpper(e.name) = $nf) RETURN e");
                soft.assertThat(parameters).containsEntry("nf", payload);
            });
        }

        @Test
        @DisplayName("A hostile attribute name is rejected")
        void shouldRejectHostileAttribute() {
            var condition = eq(fn("UPPER", attr("name) DETACH DELETE e //")), constant("A"));

            assertSoftly(soft -> soft.assertThatThrownBy(() -> select(condition, new HashMap<>()))
                    .isInstanceOf(IllegalArgumentException.class));
        }
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
                Arguments.of("LEFT", eq(fn("LEFT", attr("name"), value(3)), constant("Ota"))),
                Arguments.of("RIGHT", eq(fn("RIGHT", attr("name"), value(3)), constant("Ota"))),
                Arguments.of("nested LEFT", eq(fn("UPPER", new FunctionArgument.NativeArgument(
                        NativeFunction.of("LEFT", attr("name"), value(3)))), constant("OTA"))),
                Arguments.of("partial evaluation with LEFT", eq(fn("LEFT", attr("name"),
                        new FunctionArgument.NativeArgument(NativeFunction.of("LENGTH", attr("other")))),
                        constant("Ota"))),
                Arguments.of("null argument", eq(fn("LEFT", attr("name"), value(null)), constant("x"))),
                Arguments.of("native on the right", eq(new CriteriaOperand.Attribute("name"),
                        fn("RIGHT", attr("other"), value(2)))),
                Arguments.of("LIKE on LENGTH", CriteriaCondition.of(fn("LENGTH", attr("name")), Condition.LIKE,
                        constant("1%"))),
                Arguments.of("AND group", CriteriaCondition.and(CriteriaCondition.eq(Element.of("age", 1)),
                        eq(fn("LEFT", attr("name"), value(3)), constant("Ota")))));
    }

    @Nested
    @DisplayName("Given unsupported native functions")
    class Unsupported {

        private Session session;
        private DefaultNeo4JDatabaseManager manager;

        @BeforeEach
        void setUp() {
            session = mock(Session.class);
            manager = new DefaultNeo4JDatabaseManager(session, "neo4j");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.neo4j.communication.Neo4JNativeFunctionTest#unsupported")
        void shouldRejectBeforeAnySession(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(
                        new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(
                        new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true)))
                        .isInstanceOfAny(UnsupportedOperationException.class,
                                org.eclipse.jnosql.communication.CommunicationException.class);
                soft.assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("person", condition, List.of())))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.update(new DefaultUpdateQuery("person",
                        List.of(Element.of("flag", true)), condition)))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(session);
        }
    }

    @Nested
    @DisplayName("Given a mocked Neo4j session")
    class Communication {

        @Test
        @DisplayName("The Cypher and parameters reach the transaction")
        void shouldReachTheDriver() {
            var session = mock(Session.class);
            var tx = mock(Transaction.class);
            var result = mock(org.neo4j.driver.Result.class);
            when(session.beginTransaction()).thenReturn(tx);
            when(tx.run(anyString(), any(Value.class))).thenReturn(result);
            when(result.list(any())).thenReturn(new ArrayList<>());
            var manager = new DefaultNeo4JDatabaseManager(session, "neo4j");

            manager.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    eq(fn("UPPER", attr("name")), constant("OTA")), false)).toList();

            var cypher = ArgumentCaptor.forClass(String.class);
            var params = ArgumentCaptor.forClass(Value.class);
            verify(tx).run(cypher.capture(), params.capture());
            assertSoftly(soft -> {
                soft.assertThat(cypher.getValue()).isEqualTo("MATCH (e:person) WHERE (toUpper(e.name) = $nf) RETURN e");
                soft.assertThat(params.getValue().get("nf").asString()).isEqualTo("OTA");
            });
        }
    }
}
