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

import jakarta.json.bind.Jsonb;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.GetRequest;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.QueryResult;
import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Oracle NoSQL native functions reaching the mocked client")
class OracleNativeFunctionManagerTest {

    private static final String TABLE = "database";
    private static final String NAME = "database.content.name";

    private NoSQLHandle handle;
    private PreparedStatement statement;
    private DefaultOracleNoSQLDocumentManager manager;

    @BeforeEach
    void setUp() {
        handle = mock(NoSQLHandle.class);
        statement = mock(PreparedStatement.class);
        when(handle.prepare(any(PrepareRequest.class)))
                .thenReturn(new PrepareResult().setPreparedStatement(statement));
        when(handle.query(any(QueryRequest.class))).thenAnswer(invocation -> {
            QueryRequest request = invocation.getArgument(0);
            request.setContKey(null);
            return new QueryResult(request).setResults(List.of());
        });
        manager = new DefaultOracleNoSQLDocumentManager(TABLE, handle, mock(Jsonb.class));
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
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

    private static CriteriaCondition compare(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private static DefaultSelectQuery select(CriteriaCondition condition, boolean count) {
        return new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, count);
    }

    private String preparedStatement() {
        var request = ArgumentCaptor.forClass(PrepareRequest.class);
        verify(handle).prepare(request.capture());
        return request.getValue().getStatement().replaceAll("\\s+", " ");
    }

    private List<Object> bound() {
        var values = ArgumentCaptor.forClass(FieldValue.class);
        verify(statement, org.mockito.Mockito.atLeastOnce()).setVariable(any(Integer.class), values.capture());
        return values.getAllValues().stream().map(value -> value.isString() ? (Object) value.getString()
                : value.getLong()).toList();
    }

    static Stream<Arguments> functions() {
        return Stream.of(
                Arguments.of(compare(function("ABS", attribute("balance")), constant(7)),
                        "abs(database.content.balance) = ?", List.of("person", 7L)),
                Arguments.of(compare(function("LENGTH", attribute("name")), constant(3)),
                        "length(" + NAME + ") ELSE null END = ?", List.of("person", 3L)),
                Arguments.of(compare(function("LOWER", attribute("name")), constant("ota")),
                        "lower(" + NAME + ") ELSE null END = ?", List.of("person", "ota")),
                Arguments.of(compare(function("UPPER", attribute("name")), constant("OTA")),
                        "upper(" + NAME + ") ELSE null END = ?", List.of("person", "OTA")),
                Arguments.of(compare(function("LEFT", attribute("name"), value(3)), constant("Ota")),
                        "substring(" + NAME + ", 0, 3)", List.of("person", "Ota")),
                Arguments.of(compare(function("RIGHT", attribute("name"), value(3)), constant("vio")),
                        "substring(" + NAME + ", length(" + NAME + ") - 3)", List.of("person", "vio")),
                Arguments.of(compare(new CriteriaOperand.Attribute("name"), function("UPPER", attribute("other"))),
                        NAME + " = CASE WHEN database.content.other", List.of("person")));
    }

    @Nested
    @DisplayName("Given native function predicates")
    class Supported {

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.communication.OracleNativeFunctionManagerTest#functions")
        @DisplayName("SELECT prepares the native SQL and binds the constants without reading by id")
        void shouldSelect(CriteriaCondition condition, String fragment, List<Object> variables) {
            manager.select(select(condition, false)).toList();

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).contains(fragment);
                soft.assertThat(bound()).containsExactlyElementsOf(variables);
            });
            org.mockito.Mockito.verify(handle, org.mockito.Mockito.never()).get(any(GetRequest.class));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.communication.OracleNativeFunctionManagerTest#functions")
        void shouldCount(CriteriaCondition condition, String fragment, List<Object> variables) {
            manager.count(select(condition, true));

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).startsWith("select count(*)").contains(fragment);
                soft.assertThat(bound()).containsExactlyElementsOf(variables);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.oracle.communication.OracleNativeFunctionManagerTest#functions")
        void shouldDelete(CriteriaCondition condition, String fragment, List<Object> variables) {
            manager.delete(NativeFunctionQueries.delete("person", condition, List.of()));

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).startsWith("DELETE").contains(fragment);
                soft.assertThat(bound()).containsExactlyElementsOf(variables);
            });
            org.mockito.Mockito.verify(handle, org.mockito.Mockito.never()).delete(any(DeleteRequest.class));
        }

        @Test
        @DisplayName("A nested function with partial evaluation receives the evaluated length as a number")
        void shouldNestWithEvaluatedLength() {
            var left = function("UPPER", new FunctionArgument.NativeArgument(
                    NativeFunction.of("LEFT", attribute("name"), value(3))));

            manager.select(select(compare(left, constant("OTA")), false)).toList();

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).contains("upper(CASE WHEN").contains("substring(" + NAME + ", 0, 3)");
                soft.assertThat(bound()).containsExactly("person", "OTA");
            });
        }

        @Test
        @DisplayName("A native function on both sides binds only the entity name")
        void shouldTranslateBothSides() {
            manager.select(select(compare(function("UPPER", attribute("firstName")),
                    function("LOWER", attribute("lastName"))), false)).toList();

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).contains("database.content.firstName")
                        .contains("database.content.lastName");
                soft.assertThat(bound()).containsExactly("person");
            });
        }

        @Test
        @DisplayName("Repeated executions bind the values of each query")
        void shouldNotReuseValues() {
            manager.select(select(compare(function("UPPER", attribute("name")), constant("A")), false)).toList();
            manager.select(select(compare(function("UPPER", attribute("name")), constant("B")), false)).toList();

            var values = ArgumentCaptor.forClass(FieldValue.class);
            verify(statement, org.mockito.Mockito.times(4)).setVariable(any(Integer.class), values.capture());
            assertThat(values.getAllValues()).extracting(FieldValue::toString)
                    .contains("\"A\"", "\"B\"");
        }

        @Test
        @DisplayName("An injection payload stays a bound value")
        void shouldBindInjectionPayload() {
            var payload = "x' OR 1=1 --\\";
            manager.select(select(compare(function("UPPER", attribute("name")), constant(payload)), false)).toList();

            assertSoftly(soft -> {
                soft.assertThat(preparedStatement()).doesNotContain("OR 1=1");
                soft.assertThat(bound()).containsExactly("person", payload);
            });
        }
    }

    @Nested
    @DisplayName("Given unsupported native function predicates")
    class Unsupported {

        static Stream<CriteriaCondition> unsupported() {
            return Stream.of(
                    compare(function("REVERSE", attribute("name")), constant("x")),
                    compare(function("LEFT", attribute("name"), attribute("size")), constant("x")),
                    compare(function("UPPER", attribute("_id")), constant("A")),
                    CriteriaCondition.of(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%")));
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectSelectWithoutRequest(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.select(select(condition, false)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(handle);
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectCountWithoutRequest(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.count(select(condition, true)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(handle);
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported delete is rejected and never becomes an unrestricted delete")
        void shouldRejectDeleteWithoutRequest(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> manager.deleteAndCount(NativeFunctionQueries.delete("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(handle);
        }
    }
}
