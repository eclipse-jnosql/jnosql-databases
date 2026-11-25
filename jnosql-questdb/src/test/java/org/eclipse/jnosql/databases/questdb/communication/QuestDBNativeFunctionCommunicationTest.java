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
package org.eclipse.jnosql.databases.questdb.communication;

import io.questdb.client.Completion;
import io.questdb.client.Query;
import io.questdb.client.QuestDB;
import io.questdb.client.cutlass.qwp.client.QwpBindSetter;
import io.questdb.client.cutlass.qwp.client.QwpBindValues;
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

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("QuestDB manager sends native function predicates to the client boundary")
class QuestDBNativeFunctionCommunicationTest {

    private QuestDB questDB;

    private Query query;

    private QuestDBTimeSeriesManager manager;

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
        questDB = mock(QuestDB.class);
        query = mock(Query.class, org.mockito.Answers.RETURNS_SELF);
        var completion = mock(Completion.class);
        when(completion.await(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        when(query.submit()).thenReturn(completion);
        when(questDB.borrowQuery()).thenReturn(query);
        manager = new QuestDBTimeSeriesManager(questDB, "qdb");
    }

    private String capturedSql(int invocation) {
        var statement = ArgumentCaptor.forClass(CharSequence.class);
        verify(query, org.mockito.Mockito.atLeast(invocation + 1)).sql(statement.capture());
        return statement.getAllValues().get(invocation).toString();
    }

    private QwpBindValues capturedBinds(int invocation) {
        var setter = ArgumentCaptor.forClass(QwpBindSetter.class);
        verify(query, org.mockito.Mockito.atLeast(invocation + 1)).binds(setter.capture());
        var values = mock(QwpBindValues.class);
        setter.getAllValues().get(invocation).apply(values);
        return values;
    }

    @Nested
    @DisplayName("Given a supported native predicate")
    class Supported {

        @Test
        void shouldSendSelectWithBoundParameters() {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of("location", "Lisbon")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")));

            manager.select(select(condition, false));

            var binds = capturedBinds(0);
            assertSoftly(soft -> {
                soft.assertThat(capturedSql(0)).isEqualTo(
                        "SELECT * FROM \"sensor_reading\" WHERE (\"location\" = $1 AND (upper(\"name\") = $2))");
                verify(binds).setVarchar(0, "Lisbon");
                verify(binds).setVarchar(1, "OTAVIO");
            });
        }

        @Test
        @DisplayName("LEFT with an evaluated length binds the ordinary numeric value")
        void shouldBindEvaluatedLength() {
            manager.select(select(eq(fn("LEFT", attr("name"), new FunctionArgument.ValueArgument(Value.of(3))),
                    constant("Ota")), false));

            var binds = capturedBinds(0);
            assertSoftly(soft -> {
                soft.assertThat(capturedSql(0)).contains("left(\"name\", $1) = $2");
                verify(binds).setInt(0, 3);
                verify(binds).setVarchar(1, "Ota");
            });
        }

        @Test
        @DisplayName("Each execution binds its own values")
        void shouldNotReuseValuesBetweenExecutions() {
            manager.select(select(eq(fn("UPPER", attr("name")), constant("ONE")), false));
            manager.select(select(eq(fn("UPPER", attr("name")), constant("TWO")), false));

            verify(capturedBinds(0)).setVarchar(0, "ONE");
            verify(capturedBinds(1)).setVarchar(0, "TWO");
        }

        @Test
        @DisplayName("COUNT sends the native predicate")
        void shouldSendCount() {
            manager.count(select(eq(fn("LENGTH", attr("name")), constant(6)), true));

            assertSoftly(soft -> soft.assertThat(capturedSql(0)).startsWith("SELECT COUNT(*) FROM \"sensor_reading\"")
                    .contains("nullif(length(\"name\"), -1) = $1"));
        }

        @Test
        @DisplayName("UPDATE WHERE carries the native predicate")
        void shouldSendUpdateWithNativePredicate() {
            manager.update(new DefaultUpdateQuery("sensor_reading", List.of(Element.of("value", 1)),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO"))));

            assertSoftly(soft -> soft.assertThat(capturedSql(0)).contains("WHERE (upper(\"name\") = $").contains("UPDATE"));
        }
    }

    @Nested
    @DisplayName("Given an unsupported native predicate")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.questdb.communication.QuestDBNativeFunctionCommunicationTest#unsupported")
        void shouldRejectWithoutQuery(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select(condition, false)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(select(condition, true)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.update(new DefaultUpdateQuery("sensor_reading",
                        List.of(Element.of("value", 1)), condition))).isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(questDB);
        }

        @Test
        @DisplayName("DELETE stays unsupported and never reaches the client")
        void shouldRejectDelete() {
            var condition = eq(fn("UPPER", attr("name")), constant("OTAVIO"));

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.delete(
                    new DefaultDeleteQuery("sensor_reading", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class));
            verifyNoInteractions(questDB);
        }
    }
}
