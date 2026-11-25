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
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("OrientDB manager with native functions and a mocked connection pool")
class OrientNativeFunctionManagerTest {

    private final DefaultOrientDBDocumentManager manager = new DefaultOrientDBDocumentManager(
            MockedOrientProducer.POOL, "library");

    @BeforeEach
    void setUp() {
        MockedOrientProducer.reset();
    }

    private static List<Object> params(org.mockito.invocation.Invocation call) {
        return java.util.Arrays.asList(call.getArguments()).subList(1, call.getArguments().length);
    }

    private static CriteriaCondition condition(String function, String attribute, Object value) {
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of(function,
                new FunctionArgument.AttributePath(attribute))), Condition.EQUALS,
                new CriteriaOperand.Constant(Value.of(value)));
    }

    private static DefaultSelectQuery select(CriteriaCondition condition, boolean count) {
        return new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, count);
    }

    @Nested
    @DisplayName("Given a supported native function")
    class Supported {

        @Test
        void shouldSelect() {
            manager.select(select(condition("UPPER", "name", "OTAVIO"), false)).toList();

            assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).singleElement().satisfies(call -> {
                soft.assertThat(call.getArgument(0, String.class))
                        .isEqualTo("SELECT FROM person WHERE (name.toUpperCase() = ?)");
                soft.assertThat(params(call)).containsExactly("OTAVIO");
            }));
        }

        @Test
        void shouldCount() {
            manager.count(select(condition("ABS", "balance", 3), true));

            assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).singleElement().satisfies(call -> {
                soft.assertThat(call.getArgument(0, String.class))
                        .isEqualTo("SELECT COUNT(*) FROM person WHERE (abs(balance) = ?)");
                soft.assertThat(params(call)).containsExactly(3);
            }));
        }

        @Test
        @DisplayName("Delete selects with the native predicate and never an unrestricted statement")
        void shouldDeleteWithNativePredicate() {
            manager.delete(NativeFunctionQueries.delete("person", condition("LOWER", "name", "x"), List.of()));

            assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).singleElement().satisfies(call ->
                    soft.assertThat(call.getArgument(0, String.class)).contains("WHERE (name.toLowerCase() = ?)")));
        }
    }

    @Nested
    @DisplayName("Given an unsupported native function")
    class Unsupported {

        @ParameterizedTest
        @ValueSource(strings = {"LENGTH", "LEFT", "RIGHT", "REVERSE"})
        void shouldRejectBeforeAnyRequest(String function) {
            var arguments = function.equals("LENGTH") || function.equals("REVERSE")
                    ? new FunctionArgument[]{new FunctionArgument.AttributePath("name")}
                    : new FunctionArgument[]{new FunctionArgument.AttributePath("name"),
                    new FunctionArgument.ValueArgument(Value.of(2))};
            var condition = CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of(function, arguments)),
                    Condition.EQUALS, new CriteriaOperand.Constant(Value.of("x")));

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select(condition, false)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.count(select(condition, true)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("person", condition, List.of())))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThat(MockedOrientProducer.acquisitions()).isZero();
                soft.assertThat(MockedOrientProducer.commands()).isEmpty();
            });
        }

        @Test
        @DisplayName("A rejected predicate inside a group never turns into an unrestricted delete")
        void shouldRejectGroupedDelete() {
            var condition = CriteriaCondition.or(condition("UPPER", "name", "A"), condition("LENGTH", "name", 3));

            assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).isEmpty());
        }
    }
}
