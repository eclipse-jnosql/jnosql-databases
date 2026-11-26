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
package org.eclipse.jnosql.databases.elasticsearch.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("Elasticsearch rejects native functions without a request")
class ElasticsearchNativeFunctionRejectionTest {

    private DefaultElasticsearchDocumentManager manager;

    @BeforeEach
    void setUp() {
        MockedElasticsearchProducer.reset();
        manager = MockedElasticsearchProducer.manager();
        org.mockito.Mockito.clearInvocations(MockedElasticsearchProducer.CLIENT);
    }

    private static CriteriaCondition function(String name, Object... extra) {
        var arguments = new ArrayList<FunctionArgument>();
        arguments.add(new FunctionArgument.AttributePath("name"));
        for (Object value : extra) {
            arguments.add(new FunctionArgument.ValueArgument(Value.of(value)));
        }
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of(name,
                arguments.toArray(FunctionArgument[]::new))), Condition.EQUALS,
                new CriteriaOperand.Constant(Value.of("x")));
    }

    static Stream<Arguments> conditions() {
        var plain = CriteriaCondition.eq(Element.of("age", 1));
        var onTheRight = CriteriaCondition.of(new CriteriaOperand.Attribute("name"), Condition.EQUALS,
                new CriteriaOperand.Function(NativeFunction.of("UPPER", new FunctionArgument.AttributePath("other"))));
        var nested = CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of("UPPER",
                new FunctionArgument.NativeArgument(NativeFunction.of("LEFT", new FunctionArgument.AttributePath("name"),
                        new FunctionArgument.ValueArgument(Value.of(3)))))), Condition.EQUALS,
                new CriteriaOperand.Constant(Value.of("OTA")));
        return Stream.of(
                Arguments.of("ABS", function("ABS")),
                Arguments.of("LENGTH", function("LENGTH")),
                Arguments.of("LOWER", function("LOWER")),
                Arguments.of("UPPER", function("UPPER")),
                Arguments.of("LEFT", function("LEFT", 3)),
                Arguments.of("RIGHT", function("RIGHT", 3)),
                Arguments.of("native on the right", onTheRight),
                Arguments.of("nested", nested),
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
        @MethodSource("org.eclipse.jnosql.databases.elasticsearch.communication."
                + "ElasticsearchNativeFunctionRejectionTest#conditions")
        void shouldRejectBeforeAnyRequest(String name, CriteriaCondition condition) {
            var select = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false);
            var delete = NativeFunctionQueries.delete("person", condition, List.of());

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("Elasticsearch");
                soft.assertThatThrownBy(() -> manager.delete(delete)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.deleteAndCount(delete))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> QueryConverter.requireNoNativeFunction(Optional.of(condition)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThat(MockedElasticsearchProducer.calls()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("Given ordinary predicates")
    class Ordinary {

        @Test
        @DisplayName("A hostile value stays a typed value of the query and is not a script")
        void shouldKeepValueInStructuredQuery() {
            var payload = "x\" } }, \"script\": { \"source\": \"1\"";
            var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    CriteriaCondition.eq(Element.of("name", payload)), false);

            assertThatThrownBy(() -> manager.select(query)).isNotInstanceOf(UnsupportedOperationException.class);

            assertSoftly(soft -> soft.assertThat(MockedElasticsearchProducer.searches()).singleElement()
                    .satisfies(request -> {
                        soft.assertThat(request.query().isBool()).isTrue();
                        soft.assertThat(request.toString()).doesNotContain("\"script\":{\"source\"");
                        soft.assertThat(request.toString()).contains("person");
                    }));
        }
    }
}
