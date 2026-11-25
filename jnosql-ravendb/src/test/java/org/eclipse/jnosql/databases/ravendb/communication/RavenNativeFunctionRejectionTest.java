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
package org.eclipse.jnosql.databases.ravendb.communication;

import net.ravendb.client.documents.DocumentStore;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("RavenDB rejects native functions without reaching the server")
class RavenNativeFunctionRejectionTest {

    private RavenDBDocumentManager manager;

    @BeforeEach
    void setUp() {
        MockedRavenProducer.reset();
        manager = new RavenDBDocumentManager(MockedRavenProducer.STORE, "library");
    }

    private static CriteriaCondition function(String name, Object... extra) {
        var arguments = new java.util.ArrayList<FunctionArgument>();
        arguments.add(new FunctionArgument.AttributePath("name"));
        for (Object value : extra) {
            arguments.add(new FunctionArgument.ValueArgument(Value.of(value)));
        }
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of(name,
                arguments.toArray(FunctionArgument[]::new))), Condition.EQUALS,
                new CriteriaOperand.Constant(Value.of("x")));
    }

    private static CriteriaCondition nativeOnTheRight() {
        return CriteriaCondition.of(new CriteriaOperand.Attribute("name"), Condition.EQUALS,
                new CriteriaOperand.Function(NativeFunction.of("UPPER",
                        new FunctionArgument.AttributePath("other"))));
    }

    private static CriteriaCondition nested() {
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of("UPPER",
                new FunctionArgument.NativeArgument(NativeFunction.of("LEFT",
                        new FunctionArgument.AttributePath("name"), new FunctionArgument.ValueArgument(Value.of(3)))))),
                Condition.EQUALS, new CriteriaOperand.Constant(Value.of("OTA")));
    }

    static Stream<Arguments> conditions() {
        var plain = CriteriaCondition.eq(Element.of("age", 1));
        return Stream.of(
                Arguments.of("ABS", function("ABS")),
                Arguments.of("LENGTH", function("LENGTH")),
                Arguments.of("LOWER", function("LOWER")),
                Arguments.of("UPPER", function("UPPER")),
                Arguments.of("LEFT", function("LEFT", 3)),
                Arguments.of("RIGHT", function("RIGHT", 3)),
                Arguments.of("native on the right", nativeOnTheRight()),
                Arguments.of("nested", nested()),
                Arguments.of("AND group", CriteriaCondition.and(plain, function("UPPER"))),
                Arguments.of("OR group", CriteriaCondition.or(function("UPPER"), plain)),
                Arguments.of("NOT", function("UPPER").negate()),
                Arguments.of("deep group", CriteriaCondition.and(plain,
                        CriteriaCondition.or(plain, function("LOWER").negate()))));
    }

    @Nested
    @DisplayName("Given a native function predicate")
    class Rejected {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.ravendb.communication.RavenNativeFunctionRejectionTest#conditions")
        void shouldRejectSelect(String name, CriteriaCondition condition) {
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(), condition, false);

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(query)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("RavenDB");
                soft.assertThatThrownBy(() -> DocumentQueryConverter.requireNoNativeFunction(Optional.of(condition)))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(MockedRavenProducer.SESSION, MockedRavenProducer.QUERY);
            verify(MockedRavenProducer.STORE, never()).openSession();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.ravendb.communication.RavenNativeFunctionRejectionTest#conditions")
        @DisplayName("A rejected delete never loads or removes a document")
        void shouldRejectDelete(String name, CriteriaCondition condition) {
            var query = new DefaultDeleteQuery("people", condition, List.of());

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.delete(query))
                    .isInstanceOf(UnsupportedOperationException.class));
            verify(MockedRavenProducer.STORE, never()).openSession();
            verifyNoInteractions(MockedRavenProducer.SESSION);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.ravendb.communication.RavenNativeFunctionRejectionTest#conditions")
        @DisplayName("The query converter also rejects when it is called with a session")
        void shouldRejectInConverter(String name, CriteriaCondition condition) {
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(), condition, false);

            assertSoftly(soft -> soft.assertThatThrownBy(() ->
                            DocumentQueryConverter.createQuery(MockedRavenProducer.SESSION, query))
                    .isInstanceOf(UnsupportedOperationException.class));
            verifyNoInteractions(MockedRavenProducer.SESSION);
        }

        @ParameterizedTest
        @ValueSource(strings = {"_id", "id"})
        @DisplayName("An id predicate next to a native function does not take the id lookup shortcut")
        void shouldNotBypassWithIdLookup(String id) {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of(id, "people/1")),
                    function("UPPER"));
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(), condition, false);

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.select(query))
                    .isInstanceOf(UnsupportedOperationException.class));
            verify(MockedRavenProducer.STORE, never()).openSession();
        }
    }

    @Nested
    @DisplayName("Given ordinary predicates")
    class Ordinary {

        @Test
        @DisplayName("Ordinary conditions still reach the query builder")
        void shouldKeepOrdinaryQueries() {
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(),
                    CriteriaCondition.eq(Element.of("name", "OTAVIO")), false);

            manager.select(query).toList();

            verify(MockedRavenProducer.QUERY).whereEquals("name", (Object) "OTAVIO");
        }
    }
}
