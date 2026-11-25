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
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
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
import org.mockito.invocation.Invocation;

import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@DisplayName("ArangoDB native functions reaching the mocked driver")
class ArangoNativeFunctionManagerTest {

    private ArangoDB arangoDB;
    private ArangoDatabase database;
    private DefaultArangoDBDocumentManager manager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        arangoDB = mock(ArangoDB.class);
        database = mock(ArangoDatabase.class);
        var collection = mock(ArangoCollection.class);
        ArangoCursor<Object> cursor = mock(ArangoCursor.class, RETURNS_DEEP_STUBS);
        when(arangoDB.db("library")).thenReturn(database);
        when(arangoDB.getAccessibleDatabases()).thenReturn(List.of("library"));
        when(database.name()).thenReturn("library");
        when(database.arango()).thenReturn(arangoDB);
        when(database.collection("person")).thenReturn(collection);
        when(collection.exists()).thenReturn(true);
        when(cursor.spliterator()).thenAnswer(invocation -> Spliterators.emptySpliterator());
        when(database.query(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.any())).thenReturn((ArangoCursor) cursor);
        manager = new DefaultArangoDBDocumentManager("library", arangoDB);
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

    private List<Invocation> requests() {
        return mockingDetails(database).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("query")).toList();
    }

    private Invocation request() {
        assertThat(requests()).hasSize(1);
        return requests().getFirst();
    }

    private String aql() {
        return request().getArgument(0, String.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> variables() {
        return (Map<String, Object>) request().getArgument(2, Map.class);
    }

    private boolean touchedTheServer() {
        return !requests().isEmpty()
                || mockingDetails(arangoDB).getInvocations().stream()
                .anyMatch(call -> call.getMethod().getName().startsWith("create"))
                || mockingDetails(database).getInvocations().stream()
                .anyMatch(call -> call.getMethod().getName().equals("collection"));
    }

    static Stream<Arguments> functions() {
        return Stream.of(
                Arguments.of(compare(function("ABS", attribute("balance")), constant(7)),
                        "ABS(p.balance)", Map.of("nf", 7)),
                Arguments.of(compare(function("LENGTH", attribute("name")), constant(3)),
                        "CHAR_LENGTH(p.name)", Map.of("nf", 3)),
                Arguments.of(compare(function("LOWER", attribute("name")), constant("ota")),
                        "LOWER(p.name)", Map.of("nf", "ota")),
                Arguments.of(compare(function("UPPER", attribute("name")), constant("OTA")),
                        "UPPER(p.name)", Map.of("nf", "OTA")),
                Arguments.of(compare(function("LEFT", attribute("name"), value(3)), constant("Ota")),
                        "LEFT(p.name, @nf)", Map.of("nf", 3, "nf_1", "Ota")),
                Arguments.of(compare(function("RIGHT", attribute("name"), value(3)), constant("vio")),
                        "RIGHT(p.name, @nf)", Map.of("nf", 3, "nf_1", "vio")));
    }

    @Nested
    @DisplayName("Given native function predicates")
    class Supported {

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.arangodb.communication.ArangoNativeFunctionManagerTest#functions")
        void shouldSelect(CriteriaCondition condition, String fragment, Map<String, Object> variables) {
            manager.select(select(condition, false)).toList();

            assertSoftly(soft -> {
                soft.assertThat(aql()).startsWith("FOR p IN person FILTER").contains(fragment).endsWith("RETURN p");
                soft.assertThat(variables()).isEqualTo(variables);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.arangodb.communication.ArangoNativeFunctionManagerTest#functions")
        void shouldCount(CriteriaCondition condition, String fragment, Map<String, Object> variables) {
            manager.count(select(condition, true));

            assertSoftly(soft -> {
                soft.assertThat(aql()).startsWith("RETURN LENGTH(FOR p IN person FILTER").contains(fragment);
                soft.assertThat(variables()).isEqualTo(variables);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.arangodb.communication.ArangoNativeFunctionManagerTest#functions")
        void shouldDelete(CriteriaCondition condition, String fragment, Map<String, Object> variables) {
            manager.delete(new DefaultDeleteQuery("person", condition, List.of()));

            assertSoftly(soft -> {
                soft.assertThat(aql()).startsWith("FOR p IN person FILTER").contains(fragment)
                        .endsWith("REMOVE p IN person");
                soft.assertThat(variables()).isEqualTo(variables);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.arangodb.communication.ArangoNativeFunctionManagerTest#functions")
        void shouldUpdate(CriteriaCondition condition, String fragment, Map<String, Object> variables) {
            manager.update(new DefaultUpdateQuery("person", List.of(Element.of("status", "x")), condition));

            assertSoftly(soft -> {
                soft.assertThat(aql()).startsWith("FOR p IN person FILTER").contains(fragment)
                        .contains("UPDATE p WITH { status: @status } IN person");
                soft.assertThat(variables()).containsAllEntriesOf(variables).containsEntry("status", "x");
            });
        }

        @Test
        @DisplayName("Native functions on both sides and nested functions reach the driver as AQL")
        void shouldSelectBothSidesAndNested() {
            manager.select(select(compare(function("UPPER", attribute("firstName")),
                    function("LOWER", attribute("lastName"))), false)).toList();

            assertThat(aql()).contains("UPPER(p.firstName)").contains("LOWER(p.lastName)");
        }
    }

    @Nested
    @DisplayName("Given unsupported native function predicates")
    class Rejected {

        static Stream<CriteriaCondition> unsupported() {
            return Stream.of(
                    compare(function("REVERSE", attribute("name")), constant("x")),
                    compare(function("LEFT", attribute("name"), attribute("size")), constant("x")),
                    compare(function("RIGHT", attribute("name"), attribute("size")), constant("x")),
                    CriteriaCondition.eq(Element.of("age", 1))
                            .or(compare(function("REVERSE", attribute("name")), constant("x"))),
                    compare(function("UPPER", new FunctionArgument.NativeArgument(
                            NativeFunction.of("REVERSE", attribute("name")))), constant("x")));
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectSelect(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.select(select(condition, false)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(touchedTheServer()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectCount(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.count(select(condition, true)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(touchedTheServer()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported DELETE never runs an unrestricted removal")
        void shouldRejectDelete(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> manager.deleteAndCount(new DefaultDeleteQuery("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(touchedTheServer()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported UPDATE never runs an unrestricted update")
        void shouldRejectUpdate(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.update(
                    new DefaultUpdateQuery("person", List.of(Element.of("status", "x")), condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(touchedTheServer()).isFalse();
        }
    }
}
