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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.QdrantClient;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import java.util.List;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("Qdrant rejects native functions in metadata filters")
class QdrantNativeFunctionRejectionTest {

    private static FunctionArgument attr(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument number(int value) {
        return new FunctionArgument.ValueArgument(Value.of(value));
    }

    private static CriteriaOperand function(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition condition(CriteriaOperand left, Object value) {
        return CriteriaCondition.of(left, Condition.EQUALS, constant(value));
    }

    private static void convert(CriteriaCondition condition) {
        QdrantFilterConverter.convert(Optional.of(condition));
    }

    static Stream<Arguments> unsupported() {
        var upper = condition(function("UPPER", attr("title")), "OTAVIO");
        var plain = CriteriaCondition.eq(Element.of("year", 1965));
        return Stream.of(
                Arguments.of("ABS", condition(function("ABS", attr("year")), 1)),
                Arguments.of("LENGTH", condition(function("LENGTH", attr("title")), 5)),
                Arguments.of("LOWER", condition(function("LOWER", attr("title")), "dune")),
                Arguments.of("UPPER", upper),
                Arguments.of("LEFT", condition(function("LEFT", attr("title"), number(3)), "Dun")),
                Arguments.of("RIGHT", condition(function("RIGHT", attr("title"), number(3)), "une")),
                Arguments.of("nested", condition(function("UPPER", FunctionArgument.class.cast(
                        new FunctionArgument.NativeArgument(NativeFunction.of("LEFT", attr("title"), number(3))))), "DUN")),
                Arguments.of("on the right", CriteriaCondition.of(new CriteriaOperand.Attribute("title"),
                        Condition.EQUALS, function("UPPER", attr("other")))),
                Arguments.of("unknown function", condition(function("SOUNDEX", attr("title")), "D500")),
                Arguments.of("inside AND", CriteriaCondition.and(plain, upper)),
                Arguments.of("inside OR", CriteriaCondition.or(plain, upper)),
                Arguments.of("inside NOT", CriteriaCondition.not(upper)));
    }

    @Nested
    @DisplayName("Given a native function predicate")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.qdrant.communication.QdrantNativeFunctionRejectionTest#unsupported")
        void shouldReject(String name, CriteriaCondition condition) {
            assertThatThrownBy(() -> convert(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("native functions");
        }

    @Test
    @DisplayName("Select, count and delete issue no request to the client")
    void shouldNotReachTheClient() {
        var client = mock(QdrantClient.class);
        var manager = new QdrantVectorManager(client, "library", new QdrantCollectionSettings(QdrantDistance.COSINE, null));
        var condition = condition(function("UPPER", attr("title")), "OTAVIO");

        assertSoftly(soft -> {
            soft.assertThatThrownBy(() -> manager.select(new DefaultSelectQuery(0, 0, "books", List.of(), List.of(),
                    condition, false))).isInstanceOf(UnsupportedOperationException.class);
            soft.assertThatThrownBy(() -> manager.count(new DefaultSelectQuery(0, 0, "books", List.of(), List.of(),
                    condition, true))).isInstanceOf(UnsupportedOperationException.class);
            soft.assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("books", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
        org.mockito.Mockito.verifyNoInteractions(client);
    }
    }

    @Nested
    @DisplayName("Given ordinary metadata predicates")
    class Preserved {

        @Test
        @DisplayName("Plain predicates keep converting, including evaluated values")
        void shouldKeepPlainFilters() {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of("year", 1965)),
                    CriteriaCondition.eq(Element.of("title", "DUN")));

            assertSoftly(soft -> soft.assertThat(QdrantFilterConverter.convert(Optional.of(condition))).isPresent());
        }
    }
}
