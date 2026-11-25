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

import com.influxdb.v3.client.InfluxDBClient;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("InfluxDB manager sends native function predicates to the client boundary")
class InfluxDBNativeFunctionCommunicationTest {

    private InfluxDBClient client;

    private InfluxDBTimeSeriesManager manager;

    private static FunctionArgument attr(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static CriteriaOperand fn(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition eq(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private static DefaultSelectQuery select(CriteriaCondition condition, boolean count) {
        return new DefaultSelectQuery(0, 0, "temperature", List.of(), List.of(), condition, count);
    }

    @BeforeEach
    void setUp() {
        client = mock(InfluxDBClient.class);
        when(client.queryRows(anyString(), anyMap())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("COUNT(*)")
                        ? Stream.of(Map.<String, Object>of("jnosql_count", 7L)) : Stream.<Map<String, Object>>empty());
        manager = new InfluxDBTimeSeriesManager(client, "metrics");
    }

    @Nested
    @DisplayName("Given a supported native predicate")
    class Supported {

        @Test
        void shouldSendSelectWithBoundParameters() {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of("location", "Lisbon")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")));

            manager.select(select(condition, false)).toList();

            var statement = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
            verify(client).queryRows(statement.capture(), parameters.capture());
            assertSoftly(soft -> {
                soft.assertThat(statement.getValue()).isEqualTo(
                        "SELECT * FROM \"temperature\" WHERE (\"location\" = $p0 AND (upper(\"name\") = $p1))");
                soft.assertThat(parameters.getValue()).containsExactly(Map.entry("p0", "Lisbon"),
                        Map.entry("p1", "OTAVIO"));
            });
        }

        @Test
        @DisplayName("COUNT sends the native predicate and returns the client result")
        void shouldSendCount() {
            var count = manager.count(select(eq(fn("LENGTH", attr("name")), constant(6)), true));

            var statement = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
            verify(client).queryRows(statement.capture(), parameters.capture());
            assertSoftly(soft -> {
                soft.assertThat(count).isEqualTo(7L);
                soft.assertThat(statement.getValue()).isEqualTo("SELECT COUNT(*) AS \"jnosql_count\" FROM "
                        + "\"temperature\" WHERE (char_length(\"name\") = $p0)");
                soft.assertThat(parameters.getValue()).containsExactly(Map.entry("p0", 6));
            });
        }

        @Test
        @DisplayName("Each execution carries its own values and nothing is retained")
        void shouldNotReuseValuesBetweenExecutions() {
            manager.select(select(eq(fn("UPPER", attr("name")), constant("ONE")), false)).toList();
            manager.select(select(eq(fn("UPPER", attr("name")), constant("TWO")), false)).toList();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
            verify(client, org.mockito.Mockito.times(2)).queryRows(anyString(), parameters.capture());
            assertThat(parameters.getAllValues()).extracting(map -> map.get("p0")).containsExactly("ONE", "TWO");
        }
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
                Arguments.of("LIKE", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.LIKE, constant("O%"))),
                Arguments.of("null comparison", eq(fn("UPPER", attr("name")), constant(null))),
                Arguments.of("temporal identifier argument", eq(fn("UPPER", attr("_id")), constant("A"))),
                Arguments.of("length from a stored attribute",
                        eq(fn("LEFT", attr("name"), attr("size")), constant("A"))),
                Arguments.of("unsupported function in a group", CriteriaCondition.or(
                        CriteriaCondition.eq(Element.of("location", "Lisbon")),
                        eq(new CriteriaOperand.Function(NativeFunction.of("SOUNDEX", attr("name"))), constant("A")))));
    }

    @Nested
    @DisplayName("Given an unsupported native predicate")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.influxdb.communication.InfluxDBNativeFunctionCommunicationTest#unsupported")
        void shouldRejectWithoutQuery(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select(condition, false)).toList())
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(select(condition, true)))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(client);
        }

        @Test
        @DisplayName("Mutations stay unsupported and never reach the client")
        void shouldRejectMutations() {
            var condition = eq(fn("UPPER", attr("name")), constant("OTAVIO"));

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("temperature", condition,
                        List.of()))).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.update(new DefaultUpdateQuery("temperature",
                        List.of(Element.of("value", 1)), condition))).isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(client);
        }
    }
}
