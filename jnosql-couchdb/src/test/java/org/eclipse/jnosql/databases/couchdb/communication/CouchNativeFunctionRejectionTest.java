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
package org.eclipse.jnosql.databases.couchdb.communication;

import jakarta.json.Json;
import jakarta.json.JsonObject;
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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("CouchDB rejects native functions without a request")
class CouchNativeFunctionRejectionTest {

    private DefaultCouchDBDocumentManager manager;

    @BeforeEach
    void setUp() {
        MockedCouchProducer.CLIENT.reset();
        manager = MockedCouchProducer.manager();
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
        @MethodSource("org.eclipse.jnosql.databases.couchdb.communication.CouchNativeFunctionRejectionTest#conditions")
        void shouldRejectSelectBeforeAnyRequest(String name, CriteriaCondition condition) {
            var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false);

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(query)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("CouchDB");
                soft.assertThatThrownBy(() -> new MangoQueryConverter().apply(query))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThat(MockedCouchProducer.CLIENT.calls()).isEmpty();
            });
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.couchdb.communication.CouchNativeFunctionRejectionTest#conditions")
        @DisplayName("A rejected delete never searches or removes a document")
        void shouldRejectDeleteBeforeAnyRequest(String name, CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(MockedCouchProducer.CLIENT.calls()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"_id", "id"})
        @DisplayName("An id predicate next to a native function is still rejected")
        void shouldRejectNextToId(String id) {
            var condition = CriteriaCondition.and(CriteriaCondition.eq(Element.of(id, "1")), function("UPPER"));

            assertThatThrownBy(() -> manager.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    condition, false))).isInstanceOf(UnsupportedOperationException.class);
            assertThat(MockedCouchProducer.CLIENT.calls()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given ordinary predicates")
    class Ordinary {

        @Test
        @DisplayName("An evaluated value and a hostile value stay JSON values in the selector")
        void shouldKeepValuesInSelector() {
            var payload = "\"}, \"$or\": [{}] //";
            var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(),
                    CriteriaCondition.eq(Element.of("name", payload)), false);

            assertThatThrownBy(() -> manager.select(query)).isInstanceOf(CouchDBHttpClientException.class);

            var calls = MockedCouchProducer.CLIENT.calls();
            assertThat(calls).singleElement().satisfies(call -> {
                JsonObject body = Json.createReader(new StringReader(call.body())).readObject();
                assertSoftly(soft -> {
                    soft.assertThat(call.method()).isEqualTo("POST");
                    soft.assertThat(call.uri()).endsWith("/library/_find");
                    soft.assertThat(body.getJsonObject("selector").getString("name")).isEqualTo(payload);
                    soft.assertThat(body.getJsonObject("selector").keySet()).doesNotContain("$or");
                });
            });
        }
    }
}
