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
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("MongoDB native function translation")
class NativeFunctionFilterTest {

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static CriteriaOperand function(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition compare(CriteriaOperand left, Condition condition, CriteriaOperand right) {
        return CriteriaCondition.of(left, condition, right);
    }

    private static String json(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()).toJson();
    }

    private static String convert(CriteriaCondition condition) {
        return json(DocumentQueryConversor.convert(condition));
    }

    @Nested
    @DisplayName("Given a native function on the left side")
    class LeftSide {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"ABS,$abs", "LENGTH,$strLenCP", "LOWER,$toLower", "UPPER,$toUpper"})
        void shouldTranslateUnaryFunctions(String function, String operator) {
            var condition = compare(function(function, attribute("name")), Condition.EQUALS, constant("x"));

            var json = convert(condition);

            assertSoftly(soft -> {
                soft.assertThat(json).startsWith("{\"$expr\": {\"$and\"");
                soft.assertThat(json).contains(operator);
                soft.assertThat(json).contains("{\"$ifNull\": [\"$name\", null]}");
                soft.assertThat(json).contains("{\"$literal\": \"x\"}");
            });
        }

        @Test
        @DisplayName("LEFT keeps the argument order and counts code points")
        void shouldTranslateLeft() {
            var condition = compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("Ota"));

            assertThat(convert(condition))
                    .contains("\"$substrCP\": [\"$$f0a0\", 0, \"$$f0a1\"]")
                    .contains("\"f0a1\": {\"$literal\": 3}");
        }

        @Test
        @DisplayName("RIGHT starts at the length minus the requested size, never below zero")
        void shouldTranslateRight() {
            var condition = compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("vio"));

            assertThat(convert(condition))
                    .contains("\"$max\": [0, {\"$subtract\": [{\"$strLenCP\": \"$$f0a0\"}, \"$$f0a1\"]}]");
        }

        @Test
        @DisplayName("A null attribute propagates null instead of an empty string")
        void shouldPropagateNull() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("X"));

            assertThat(convert(condition))
                    .contains("{\"$cond\": [{\"$or\": [{\"$eq\": [\"$$f0a0\", null]}]}, null,");
        }

        @Test
        @DisplayName("A string attribute is guarded so a non-string value fails instead of being coerced")
        void shouldGuardStringAttribute() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("X"));

            assertThat(convert(condition)).contains("$type").contains("$strLenCP");
        }

        @Test
        @DisplayName("A nested native function keeps its structure")
        void shouldTranslateNested() {
            var inner = NativeFunction.of("LEFT", attribute("name"), FunctionArgument.literal(3));
            var condition = compare(function("UPPER", new FunctionArgument.NativeArgument(inner)),
                    Condition.EQUALS, constant("OTA"));

            var json = convert(condition);

            assertSoftly(soft -> {
                soft.assertThat(json).contains("\"f0a0\": {\"$let\": {\"vars\": {\"f1a0\"");
                soft.assertThat(json).contains("$toUpper").contains("$substrCP");
            });
        }

        @Test
        @DisplayName("An evaluated nested function is an ordinary literal argument")
        void shouldKeepEvaluatedArgumentAsLiteral() {
            var condition = compare(function("LEFT", attribute("name"),
                            new FunctionArgument.ValueArgument(Value.of(3))),
                    Condition.EQUALS, constant("Ota"));

            assertThat(convert(condition)).contains("\"f0a1\": {\"$literal\": 3}");
        }

        @Test
        @DisplayName("LENGTH may be used as the length of LEFT")
        void shouldAllowLengthAsLength() {
            var length = NativeFunction.of("LENGTH", attribute("other"));
            var condition = compare(function("LEFT", attribute("name"), new FunctionArgument.NativeArgument(length)),
                    Condition.EQUALS, constant("Ota"));

            assertThat(convert(condition)).contains("$strLenCP");
        }
    }

    @Nested
    @DisplayName("Given a native function on the right or on both sides")
    class OtherSides {

        @Test
        @DisplayName("name = UPPER(otherName) requires both sides to be non-null")
        void shouldTranslateRightSide() {
            var condition = compare(new CriteriaOperand.Attribute("name"), Condition.EQUALS,
                    function("UPPER", attribute("otherName")));

            var json = convert(condition);

            assertSoftly(soft -> {
                soft.assertThat(json).contains("{\"$ne\": [{\"$ifNull\": [\"$name\", null]}, null]}");
                soft.assertThat(json).contains("$toUpper");
                soft.assertThat(json).contains("\"$otherName\"");
            });
        }

        @Test
        @DisplayName("UPPER(firstName) = UPPER(lastName) translates both functions")
        void shouldTranslateBothSides() {
            var condition = compare(function("UPPER", attribute("firstName")), Condition.EQUALS,
                    function("UPPER", attribute("lastName")));

            assertThat(convert(condition)).contains("$firstName").contains("$lastName");
        }

        @Test
        @DisplayName("Comparing with a null constant tests for null")
        void shouldCompareWithNull() {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(null));

            assertThat(convert(condition)).contains("\"$eq\": [{\"$let\"").endsWith("{\"$literal\": null}]}}");
        }
    }

    @Nested
    @DisplayName("Given other predicate types")
    class Predicates {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"GREATER_THAN,$gt", "GREATER_EQUALS_THAN,$gte", "LESSER_THAN,$lt", "LESSER_EQUALS_THAN,$lte"})
        void shouldTranslateOrdering(Condition condition, String operator) {
            var predicate = compare(function("ABS", attribute("balance")), condition, constant(100));

            assertThat(convert(predicate)).contains(operator).contains("{\"$literal\": 100}");
        }

        @Test
        void shouldTranslateLike() {
            var predicate = compare(function("UPPER", attribute("name")), Condition.LIKE, constant("OT%"));

            assertThat(convert(predicate)).contains("$regexMatch").contains("^\\\\QO\\\\E\\\\QT\\\\E.*$");
        }

        @Test
        void shouldTranslateIn() {
            var predicate = compare(function("UPPER", attribute("name")), Condition.IN,
                    constant(List.of("A", "B")));

            assertThat(convert(predicate)).contains("\"$in\"").contains("{\"$literal\": [\"A\", \"B\"]}");
        }

        @Test
        void shouldTranslateBetween() {
            var predicate = compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                    constant(List.of(1, 5)));

            assertThat(convert(predicate)).contains("$gte").contains("$lte");
        }

        @Test
        @DisplayName("AND, OR and NOT keep the structure of the condition tree")
        void shouldKeepBooleanGrouping() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("A"));
            var plain = CriteriaCondition.eq("id", 1);
            var other = CriteriaCondition.eq("id", 2);

            var json = json(DocumentQueryConversor.convert(upper.and(plain.or(other.negate()))));

            assertSoftly(soft -> {
                soft.assertThat(json).startsWith("{\"$and\": [{\"$expr\"");
                soft.assertThat(json).contains("{\"$or\": [{\"id\": 1}, {\"$nor\": [{\"id\": 2}]}]}");
            });
        }

        @Test
        @DisplayName("A negated native predicate is wrapped in $nor")
        void shouldNegate() {
            var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("A"));

            assertThat(convert(upper.negate())).startsWith("{\"$nor\": [{\"$expr\"");
        }
    }

    @Nested
    @DisplayName("Given constants that look like MongoDB syntax")
    class Injection {

        @ParameterizedTest
        @ValueSource(strings = {"$name", "$$ROOT", "' || 1 == 1", "\\", "\"quote\"", "{\"$where\": \"1\"}", "ôÆ😀"})
        void shouldKeepConstantsAsLiterals(String payload) {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload));

            var expected = new BsonDocument("$literal", new BsonString(payload)).toJson();

            assertThat(convert(condition)).contains(expected);
        }

        @Test
        void shouldRejectOperatorAsAttributeName() {
            var condition = compare(function("UPPER", attribute("$where")), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid MongoDB field name");
        }

        @Test
        void shouldRejectOperatorInNestedAttributeName() {
            var condition = compare(function("UPPER", attribute("a.$b")), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given unsupported or invalid native functions")
    class Rejections {

        @Test
        void shouldRejectUnknownFunction() {
            var condition = compare(function("SOUNDEX", attribute("name")), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("MongoDB").hasMessageContaining("SOUNDEX");
        }

        @Test
        void shouldRejectLengthFromAttribute() {
            var condition = compare(function("LEFT", attribute("name"), attribute("size")), Condition.EQUALS,
                    constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("LEFT");
        }

        @Test
        void shouldRejectLikeOnNumericResult() {
            var condition = compare(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("LENGTH");
        }

        @Test
        void shouldRejectNegativeLength() {
            var condition = compare(function("LEFT", attribute("name"),
                    new FunctionArgument.ValueArgument(Value.of(-1))), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        void shouldRejectWrongValueType() {
            var condition = compare(function("LEFT", attribute("name"),
                    new FunctionArgument.ValueArgument(Value.of("three"))), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> convert(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("position 2");
        }
    }
}
