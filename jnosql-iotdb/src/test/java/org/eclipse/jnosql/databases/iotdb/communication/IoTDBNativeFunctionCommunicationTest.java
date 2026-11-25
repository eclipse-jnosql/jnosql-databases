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
package org.eclipse.jnosql.databases.iotdb.communication;

import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.pool.ITableSessionPool;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("IoTDB manager sends native function predicates to the client boundary")
class IoTDBNativeFunctionCommunicationTest {

    private ITableSessionPool pool;

    private ITableSession session;

    private IoTDBTimeSeriesManager manager;

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
        return new DefaultSelectQuery(0, 0, "sensor_reading", List.of(), List.of(), condition, count);
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
                Arguments.of("LIKE", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.LIKE, constant("O%"))),
                Arguments.of("null comparison", eq(fn("UPPER", attr("name")), constant(null))),
                Arguments.of("temporal identifier argument", eq(fn("UPPER", attr("_id")), constant("A"))),
                Arguments.of("unsupported function in a group", CriteriaCondition.or(
                        CriteriaCondition.eq(Element.of("location", "Lisbon")),
                        eq(new CriteriaOperand.Function(NativeFunction.of("SOUNDEX", attr("name"))), constant("A")))));
    }

    @BeforeEach
    void setUp() throws Exception {
        pool = mock(ITableSessionPool.class);
        session = mock(ITableSession.class);
        var dataSet = mock(SessionDataSet.class);
        when(dataSet.getColumnNames()).thenReturn(List.of());
        when(dataSet.getColumnTypes()).thenReturn(List.of());
        when(dataSet.hasNext()).thenReturn(false);
        when(session.executeQueryStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(dataSet);
        when(pool.getSession()).thenReturn(session);
        manager = new IoTDBTimeSeriesManager(pool, "metrics");
    }

    private String capturedSql() throws Exception {
        var statement = ArgumentCaptor.forClass(String.class);
        verify(session).executeQueryStatement(statement.capture());
        return statement.getValue();
    }

    @Nested
    @DisplayName("Given a supported native predicate")
    class Supported {

        @Test
        void shouldSendSelectWithInlinedValues() throws Exception {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of("location", "Lisbon")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")));

            manager.select(select(condition, false));

            assertThat(capturedSql()).isEqualTo(
                    "SELECT * FROM \"sensor_reading\" WHERE (\"location\" = 'Lisbon' AND (upper(\"name\") = 'OTAVIO'))");
        }

        @Test
        @DisplayName("COUNT sends the native predicate and defaults to zero for an empty result")
        void shouldSendCount() throws Exception {
            var count = manager.count(select(eq(fn("LENGTH", attr("name")), constant(6)), true));

            var sql = capturedSql();
            assertSoftly(soft -> {
                soft.assertThat(count).isZero();
                soft.assertThat(sql).isEqualTo(
                        "SELECT COUNT(*) FROM \"sensor_reading\" WHERE (length(\"name\") = 6)");
            });
        }
    }

    @Nested
    @DisplayName("Given an unsupported native predicate")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.iotdb.communication.IoTDBNativeFunctionCommunicationTest#unsupported")
        void shouldRejectWithoutQuery(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select(condition, false)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(select(condition, true)))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(pool, session);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("leftAndRight")
        @DisplayName("LEFT and RIGHT have no table-model equivalent")
        void shouldRejectLeftAndRight(String function) {
            var condition = eq(fn(function, attr("name"), new FunctionArgument.ValueArgument(Value.of(3))), constant("Ota"));

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.select(select(condition, false)))
                    .isInstanceOf(UnsupportedOperationException.class));
            verifyNoInteractions(pool, session);
        }

        static Stream<String> leftAndRight() {
            return Stream.of("LEFT", "RIGHT");
        }

        @Test
        @DisplayName("Mutations stay unsupported and never reach the client")
        void shouldRejectMutations() {
            var condition = eq(fn("UPPER", attr("name")), constant("OTAVIO"));

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("sensor_reading", condition,
                        List.of()))).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.update(new DefaultUpdateQuery("sensor_reading",
                        List.of(Element.of("value", 1)), condition))).isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(pool, session);
        }
    }
}
