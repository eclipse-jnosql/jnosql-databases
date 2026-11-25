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
package org.eclipse.jnosql.databases.solr.communication;

import org.apache.solr.common.params.SolrParams;
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
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("Solr rejects native functions without a request")
class SolrNativeFunctionRejectionTest {

    private DefaultSolrDocumentManager manager;

    @BeforeEach
    void setUp() {
        MockedSolrProducer.reset();
        manager = MockedSolrProducer.manager();
        org.mockito.Mockito.clearInvocations(MockedSolrProducer.CLIENT);
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
        @MethodSource("org.eclipse.jnosql.databases.solr.communication.SolrNativeFunctionRejectionTest#conditions")
        void shouldRejectBeforeAnyRequest(String name, CriteriaCondition condition) {
            var select = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false);
            var count = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true);
            var delete = new DefaultDeleteQuery("person", condition, List.of());

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("Solr");
                soft.assertThatThrownBy(() -> manager.count(count)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.delete(delete)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> DocumentQueryConverter.convert(select))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
            verifyNoInteractions(MockedSolrProducer.CLIENT);
        }
    }

    @Nested
    @DisplayName("Given ordinary predicates and hostile values")
    class Ordinary {

        @ParameterizedTest
        @ValueSource(strings = {"x OR *:*", "\" ; {!func}abs(1)", "{!frange l=0}abs(age)", "a\\b", "name:(1 2)"})
        @DisplayName("A hostile value is escaped and never becomes a function or a local param")
        void shouldEscapeValue(String payload) throws Exception {
            var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    CriteriaCondition.eq(Element.of("name", payload)), false);

            manager.select(query);

            var captor = ArgumentCaptor.forClass(SolrParams.class);
            verify(MockedSolrProducer.CLIENT).query(anyString(), captor.capture());
            var q = captor.getValue().get("q");
            assertSoftly(soft -> {
                soft.assertThat(q).isEqualTo("_entity:person AND name:" + DocumentQueryConverter.escape(payload));
                soft.assertThat(q).doesNotContain("{!");
            });
        }

        @Test
        void shouldRejectInvalidFieldInsideNativeFunction() {
            var condition = CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of("UPPER",
                    new FunctionArgument.AttributePath("name) OR (1"))), Condition.EQUALS,
                    new CriteriaOperand.Constant(Value.of("x")));

            assertThatThrownBy(() -> manager.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    condition, false))).isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(MockedSolrProducer.CLIENT);
        }
    }
}
