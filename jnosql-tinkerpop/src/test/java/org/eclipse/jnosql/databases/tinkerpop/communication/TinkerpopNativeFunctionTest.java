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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("TinkerPop rejects native functions")
class TinkerpopNativeFunctionTest {

    private static FunctionArgument attr(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument value(Object value) {
        return new FunctionArgument.ValueArgument(Value.of(value));
    }

    private static CriteriaOperand fn(String name, FunctionArgument... args) {
        return new CriteriaOperand.Function(NativeFunction.of(name, args));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition eq(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private Graph graph;

    @BeforeEach
    void setUp() {
        graph = TinkerGraph.open();
        graph.addVertex(T.label, "person", "name", "Otavio", "other", "OTAVIO", "age", 30);
        graph.addVertex(T.label, "person", "name", "otavio", "other", "x", "age", 31);
        graph.addVertex(T.label, "person", "name", "Maria", "other", "y", "age", 32);
        graph.addVertex(T.label, "person", "other", "z", "age", 33);
    }

    @AfterEach
    void tearDown() throws Exception {
        graph.close();
    }

    private List<Object> ages(CriteriaCondition condition) {
        var manager = new DefaultTinkerpopGraphDatabaseManager(graph);
        return manager.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false))
                .map(entity -> entity.find("age").orElseThrow().get())
                .sorted().toList();
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
                Arguments.of("UPPER", eq(fn("UPPER", attr("name")), constant("OTAVIO"))),
                Arguments.of("LOWER", eq(fn("LOWER", attr("name")), constant("maria"))),
                Arguments.of("UPPER IN", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.IN,
                        constant(List.of("MARIA", "NOBODY")))),
                Arguments.of("ABS", eq(fn("ABS", attr("age")), constant(3))),
                Arguments.of("LENGTH", eq(fn("LENGTH", attr("name")), constant(3))),
                Arguments.of("LEFT", eq(fn("LEFT", attr("name"), value(3)), constant("Ota"))),
                Arguments.of("RIGHT", eq(fn("RIGHT", attr("name"), value(3)), constant("Ota"))),
                Arguments.of("nested LEFT", eq(fn("UPPER", new FunctionArgument.NativeArgument(
                        NativeFunction.of("LEFT", attr("name"), value(3)))), constant("OTA"))),
                Arguments.of("partial evaluation", eq(fn("LEFT", attr("name"), new FunctionArgument.NativeArgument(
                        NativeFunction.of("LENGTH", attr("other")))), constant("Ota"))),
                Arguments.of("native on the right", eq(new CriteriaOperand.Attribute("name"),
                        fn("UPPER", attr("other")))),
                Arguments.of("both sides", eq(fn("UPPER", attr("name")), fn("UPPER", attr("other")))),
                Arguments.of("null constant", eq(fn("UPPER", attr("name")), constant(null))),
                Arguments.of("LIKE", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.LIKE, constant("O%"))),
                Arguments.of("AND group", CriteriaCondition.and(CriteriaCondition.eq(Element.of("age", 1)),
                        eq(fn("LEFT", attr("name"), value(3)), constant("Ota")))),
                Arguments.of("NOT", eq(fn("ABS", attr("age")), constant(3)).negate()));
    }

    @Nested
    @DisplayName("Given a JVM with the Turkish default locale")
    class TurkishLocale {

        private Locale previous;

        @BeforeEach
        void changeLocale() {
            previous = Locale.getDefault();
            Locale.setDefault(Locale.forLanguageTag("tr"));
        }

        @AfterEach
        void restoreLocale() {
            Locale.setDefault(previous);
        }

        @Test
        @DisplayName("UPPER is rejected because toUpper() follows the default locale")
        void shouldRejectUpper() {
            var manager = new DefaultTinkerpopGraphDatabaseManager(graph);
            var condition = eq(fn("UPPER", attr("name")), constant("OTAVIO"));

            assertThatThrownBy(() -> manager.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    condition, false)).toList()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Given unsupported native functions")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopNativeFunctionTest#unsupported")
        void shouldRejectBeforeTouchingTheGraph(String name, CriteriaCondition condition) {
            var spied = spy(graph);
            var manager = new DefaultTinkerpopGraphDatabaseManager(spied);

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(
                        new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false)).toList())
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(
                        new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("person", condition, List.of())))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verify(spied, never()).tx();
            verify(spied, never()).addVertex(any(Object[].class));
            assertThat(graph.traversal().V().count().next()).isEqualTo(4L);
        }

        @Test
        @DisplayName("A rejected delete removes no vertex from a real in-memory graph")
        void shouldNotDeleteAnything() {
            var manager = new DefaultTinkerpopGraphDatabaseManager(graph);
            var condition = eq(fn("ABS", attr("age")), constant(30));

            assertSoftly(soft -> soft.assertThatThrownBy(() ->
                    manager.delete(NativeFunctionQueries.delete("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class));
            assertThat(graph.traversal().V().count().next()).isEqualTo(4L);
        }
    }
}
