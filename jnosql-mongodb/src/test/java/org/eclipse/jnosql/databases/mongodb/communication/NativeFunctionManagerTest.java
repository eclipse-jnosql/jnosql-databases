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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("MongoDB native functions reaching the mocked client")
class NativeFunctionManagerTest {

    private MongoCollection<Document> collection;
    private MongoDBDocumentManager manager;

    @BeforeEach
    void setUp() {
        MockedMongoProducer.reset();
        collection = MockedMongoProducer.COLLECTION;
        manager = new MockedMongoProducer().manager();
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
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

    private static String json(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()).toJson();
    }

    private static DefaultSelectQuery select(CriteriaCondition condition) {
        return new DefaultSelectQuery(0, 0, "accounts", List.of(), List.of(), condition, false);
    }

    private String captured(String operation) {
        var filter = ArgumentCaptor.forClass(Bson.class);
        switch (operation) {
            case "find" -> verify(collection).find(filter.capture());
            case "count" -> verify(collection).countDocuments(filter.capture());
            case "delete" -> verify(collection).deleteMany(filter.capture());
            default -> verify(collection).updateMany(filter.capture(), any(Bson.class));
        }
        return json(filter.getValue());
    }

    static Stream<Arguments> functions() {
        return Stream.of(
                Arguments.of(compare(function("ABS", attribute("balance")), constant(7)), "$abs", "\"$balance\""),
                Arguments.of(compare(function("LENGTH", attribute("name")), constant(3)), "$strLenCP", "\"$name\""),
                Arguments.of(compare(function("LOWER", attribute("name")), constant("ota")), "$toLower", "\"$name\""),
                Arguments.of(compare(function("UPPER", attribute("name")), constant("OTA")), "$toUpper", "\"$name\""),
                Arguments.of(compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                        constant("Ota")), "$substrCP", "\"f0a1\": {\"$literal\": 3}"),
                Arguments.of(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                        constant("vio")), "$substrCP", "\"f0a1\": {\"$literal\": 3}"),
                Arguments.of(compare(new CriteriaOperand.Attribute("name"), function("UPPER", attribute("other"))),
                        "$toUpper", "\"$other\""));
    }

    @Nested
    @DisplayName("Given native function predicates")
    class Supported {

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.mongodb.communication.NativeFunctionManagerTest#functions")
        void shouldSelect(CriteriaCondition condition, String operator, String argument) {
            manager.select(select(condition)).toList();

            assertSoftly(soft -> {
                soft.assertThat(captured("find")).startsWith("{\"$expr\"").contains(operator).contains(argument);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.mongodb.communication.NativeFunctionManagerTest#functions")
        void shouldCount(CriteriaCondition condition, String operator, String argument) {
            manager.count(select(condition));

            assertSoftly(soft -> soft.assertThat(captured("count")).startsWith("{\"$expr\"")
                    .contains(operator).contains(argument));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.mongodb.communication.NativeFunctionManagerTest#functions")
        void shouldDelete(CriteriaCondition condition, String operator, String argument) {
            manager.delete(NativeFunctionQueries.delete("accounts", condition, List.of()));

            assertSoftly(soft -> soft.assertThat(captured("delete")).startsWith("{\"$expr\"")
                    .contains(operator).contains(argument));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.mongodb.communication.NativeFunctionManagerTest#functions")
        void shouldUpdate(CriteriaCondition condition, String operator, String argument) {
            manager.update(NativeFunctionQueries.update("accounts", List.of(Element.of("flag", true)), condition));

            assertSoftly(soft -> soft.assertThat(captured("update")).startsWith("{\"$expr\"")
                    .contains(operator).contains(argument));
        }

        @Test
        @DisplayName("A nested function is kept as nested $let expressions, not evaluated in Java")
        void shouldKeepNestedStructure() {
            var inner = NativeFunction.of("LEFT", attribute("name"), FunctionArgument.literal(3));
            var condition = compare(function("UPPER", new FunctionArgument.NativeArgument(inner)), constant("OTA"));

            manager.select(select(condition)).toList();

            assertSoftly(soft -> soft.assertThat(captured("find"))
                    .contains("$toUpper").contains("$substrCP").contains("{\"$let\""));
        }

        @Test
        @DisplayName("An evaluated length reaches the database as an ordinary number")
        void shouldPassEvaluatedLength() {
            var condition = compare(function("LEFT", attribute("name"),
                    new FunctionArgument.ValueArgument(Value.of(3))), constant("Ota"));

            manager.select(select(condition)).toList();

            assertSoftly(soft -> soft.assertThat(captured("find")).contains("\"f0a1\": {\"$literal\": 3}"));
        }

        @Test
        @DisplayName("Hostile values stay BSON literals")
        void shouldKeepPayloadAsLiteral() {
            var condition = compare(function("UPPER", attribute("name")), constant("{\"$ne\": null}\\'"));

            manager.select(select(condition)).toList();

            assertSoftly(soft -> soft.assertThat(captured("find")).contains("{\"$literal\": \"{\\\"$ne\\\": null}\\\\'\"}"));
        }
    }

    @Nested
    @DisplayName("Given unsupported native function predicates")
    class Unsupported {

        static Stream<CriteriaCondition> unsupported() {
            return Stream.of(
                    compare(function("SOUNDEX", attribute("name")), constant("A")),
                    compare(function("LEFT", attribute("name"), attribute("size")), constant("A")),
                    CriteriaCondition.of(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%")));
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectSelectWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.select(select(condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectCountWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.count(select(condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported delete never becomes an unrestricted delete")
        void shouldRejectDeleteWithoutMutation(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.delete(NativeFunctionQueries.delete("accounts", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported update never becomes an unrestricted update")
        void shouldRejectUpdateWithoutMutation(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.update(NativeFunctionQueries.update("accounts",
                    List.of(Element.of("flag", true)), condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoInteractions(collection);
        }
    }
}
