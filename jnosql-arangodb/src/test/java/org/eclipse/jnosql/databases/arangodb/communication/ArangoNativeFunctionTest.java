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

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.QueryException;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultUpdateQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("ArangoDB native function translation")
class ArangoNativeFunctionTest {

    private static final String UPPER_NAME = "(IS_STRING(p.name) ? UPPER(p.name) : null)";

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument nested(String name, FunctionArgument... arguments) {
        return new FunctionArgument.NativeArgument(NativeFunction.of(name, arguments));
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

    private static CriteriaOperand attributeOperand(String name) {
        return new CriteriaOperand.Attribute(name);
    }

    private static CriteriaCondition compare(CriteriaOperand left, Condition condition, CriteriaOperand right) {
        return CriteriaCondition.of(left, condition, right);
    }

    private static CriteriaCondition equal(CriteriaOperand left, CriteriaOperand right) {
        return compare(left, Condition.EQUALS, right);
    }

    private static AQLQueryResult select(CriteriaCondition condition) {
        return QueryAQLConverter.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition,
                false));
    }

    private static AQLQueryResult count(CriteriaCondition condition) {
        return QueryAQLConverter.count(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition,
                true));
    }

    private static AQLQueryResult delete(CriteriaCondition condition) {
        return QueryAQLConverter.delete(new DefaultDeleteQuery("person", condition, List.of()));
    }

    private static AQLQueryResult update(CriteriaCondition condition) {
        return QueryAQLConverter.update(new DefaultUpdateQuery("person", List.of(Element.of("status", "x")),
                condition));
    }

    private static String filter(AQLQueryResult result) {
        var query = result.query();
        return query.substring(query.indexOf("FILTER ") + 7, query.lastIndexOf(" RETURN"));
    }

    @Nested
    @DisplayName("Given each standard function on the left side")
    class StandardFunctions {

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "UPPER|(IS_STRING(p.name) ? UPPER(p.name) : null)",
                "LOWER|(IS_STRING(p.name) ? LOWER(p.name) : null)",
                "LENGTH|(IS_STRING(p.name) ? CHAR_LENGTH(p.name) : null)"})
        void shouldTranslateStringFunctions(String name, String expression) {
            var result = select(equal(function(name, attribute("name")), constant("x")));

            assertSoftly(soft -> {
                soft.assertThat(result.query()).isEqualTo("FOR p IN person FILTER (" + expression
                        + " == @nf) RETURN p");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", "x"));
            });
        }

        @Test
        void shouldTranslateAbs() {
            var result = select(equal(function("ABS", attribute("balance")), constant(7)));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("((IS_NUMBER(p.balance) ? ABS(p.balance) : null) == @nf)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", 7));
            });
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"LEFT", "RIGHT"})
        @DisplayName("LEFT and RIGHT bind the length first and the compared constant second")
        void shouldTranslateSubstrings(String name) {
            var result = select(equal(function(name, attribute("name"), value(3)), constant("Ota")));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("((IS_STRING(p.name) ? " + name
                        + "(p.name, @nf) : null) == @nf_1)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", 3, "nf_1", "Ota"));
            });
        }

        @Test
        @DisplayName("A dotted attribute path is kept")
        void shouldKeepDottedPath() {
            var result = select(equal(function("UPPER", attribute("address.city")), constant("X")));

            assertThat(filter(result)).contains("IS_STRING(p.address.city)");
        }
    }

    @Nested
    @DisplayName("Given the scenarios of the function contract")
    class Scenarios {

        @Test
        @DisplayName("An evaluated value is an ordinary bind variable")
        void shouldBindEvaluatedValue() {
            var result = select(equal(attributeOperand("name"), constant("OTAVIO")));

            assertThat(result.query()).isEqualTo("FOR p IN person FILTER  p.name == @name RETURN p");
        }

        @Test
        @DisplayName("A native function on the right side of the comparison")
        void shouldTranslateRightSide() {
            var result = select(equal(attributeOperand("name"), function("UPPER", attribute("otherName"))));

            assertThat(filter(result)).isEqualTo("(p.name == (IS_STRING(p.otherName) ? UPPER(p.otherName) : null))");
            assertThat(result.values()).isEmpty();
        }

        @Test
        @DisplayName("Native functions on both sides keep the operand order")
        void shouldTranslateBothSides() {
            var result = select(equal(function("UPPER", attribute("firstName")),
                    function("UPPER", attribute("lastName"))));

            assertThat(filter(result)).isEqualTo("((IS_STRING(p.firstName) ? UPPER(p.firstName) : null) == "
                    + "(IS_STRING(p.lastName) ? UPPER(p.lastName) : null))");
        }

        @Test
        @DisplayName("A native function and an evaluated value are mixed")
        void shouldMixNativeAndEvaluated() {
            var result = select(equal(function("UPPER", attribute("name")), constant("OTAVIO")));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("(" + UPPER_NAME + " == @nf)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", "OTAVIO"));
            });
        }

        @Test
        @DisplayName("Nested functions are guarded at every level because the inner result may be null")
        void shouldTranslateNested() {
            var inner = "(IS_STRING(p.name) ? LEFT(p.name, @nf) : null)";
            var result = select(equal(function("UPPER", nested("LEFT", attribute("name"), value(3))),
                    constant("OTA")));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("((IS_STRING(" + inner + ") ? UPPER(" + inner
                        + ") : null) == @nf_1)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", 3, "nf_1", "OTA"));
            });
        }

        @Test
        @DisplayName("Partial evaluation leaves the numeric length as an ordinary bind variable")
        void shouldPartiallyEvaluate() {
            var result = select(equal(function("LEFT", attribute("name"), value(3)), constant("Ota")));

            assertThat(result.values().get("nf")).isEqualTo(3);
        }

        @Test
        @DisplayName("LENGTH of another attribute can be the length of LEFT and null disables the result")
        void shouldAcceptNestedLengthAsLength() {
            var length = "(IS_STRING(p.prefix) ? CHAR_LENGTH(p.prefix) : null)";
            var result = select(equal(function("LEFT", attribute("name"), nested("LENGTH", attribute("prefix"))),
                    attributeOperand("prefix")));

            assertThat(filter(result)).isEqualTo("((IS_STRING(p.name) AND " + length + " != null ? LEFT(p.name, "
                    + length + ") : null) == p.prefix)");
        }

        @Test
        @DisplayName("A null literal argument results in null")
        void shouldPropagateNull() {
            var result = select(equal(function("LEFT", attribute("name"),
                    new FunctionArgument.Literal(null)), constant("x")));

            assertThat(filter(result)).isEqualTo("(null == @nf)");
        }
    }

    @Nested
    @DisplayName("Given the supported predicates")
    class Predicates {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"GREATER_THAN,>", "GREATER_EQUALS_THAN,>=", "LESSER_THAN,<", "LESSER_EQUALS_THAN,<="})
        void shouldTranslateOrdering(Condition condition, String operator) {
            var result = select(compare(function("LENGTH", attribute("name")), condition, constant(3)));

            var length = "(IS_STRING(p.name) ? CHAR_LENGTH(p.name) : null)";
            assertThat(filter(result)).isEqualTo("(" + length + " " + operator + " @nf AND " + length + " != null)");
        }

        @Test
        void shouldTranslateLike() {
            var result = select(compare(function("UPPER", attribute("name")), Condition.LIKE, constant("OT%")));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("(" + UPPER_NAME + " LIKE @nf)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", "OT%"));
            });
        }

        @Test
        void shouldTranslateIn() {
            var result = select(compare(function("UPPER", attribute("name")), Condition.IN,
                    constant(List.of("A", "B"))));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("(" + UPPER_NAME + " IN @nf)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", List.of("A", "B")));
            });
        }

        @Test
        void shouldTranslateBetween() {
            var result = select(compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                    constant(List.of(1, 9))));

            var abs = "(IS_NUMBER(p.balance) ? ABS(p.balance) : null)";
            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("(" + abs + " >= @nf AND " + abs + " <= @nf_1)");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", 1, "nf_1", 9));
            });
        }
    }

    @Nested
    @DisplayName("Given Boolean grouping and the query operations")
    class Operations {

        private final CriteriaCondition upper = equal(function("UPPER", attribute("name")), constant("A"));
        private final CriteriaCondition plain = CriteriaCondition.eq(Element.of("age", 1));

        @Test
        void shouldKeepGroupingAroundNativePredicates() {
            var result = select(CriteriaCondition.and(CriteriaCondition.or(upper, plain), plain));

            assertSoftly(soft -> {
                soft.assertThat(filter(result)).isEqualTo("((" + UPPER_NAME + " == @nf) OR  p.age == @age)"
                        + " AND  p.age == @age_1");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", "A", "age", 1, "age_1", 1));
            });
        }

        @Test
        void shouldNegateNativePredicate() {
            var result = select(upper.negate());

            assertThat(filter(result)).isEqualTo(" NOT ((" + UPPER_NAME + " == @nf))");
        }

        @Test
        @DisplayName("Parameters of two native predicates do not collide")
        void shouldNotCollide() {
            var other = equal(function("LOWER", attribute("name")), constant("b"));

            var result = select(upper.or(other));

            assertThat(result.values()).isEqualTo(Map.of("nf", "A", "nf_1", "b"));
        }

        @Test
        @DisplayName("An ordinary attribute named like the native bind variable does not collide")
        void shouldNotCollideWithAttributeNamedLikeParameter() {
            var result = select(CriteriaCondition.eq(Element.of("nf", "attr")).and(upper));

            assertThat(result.values()).isEqualTo(Map.of("nf", "attr", "nf_1", "A"));
        }

        @Test
        void shouldTranslateCount() {
            var result = count(upper);

            assertThat(result.query()).isEqualTo("RETURN LENGTH(FOR p IN person FILTER (" + UPPER_NAME
                    + " == @nf) RETURN p)");
        }

        @Test
        void shouldTranslateDelete() {
            var result = delete(upper);

            assertThat(result.query()).isEqualTo("FOR p IN person FILTER (" + UPPER_NAME + " == @nf) REMOVE p IN person");
        }

        @Test
        @DisplayName("UPDATE keeps the native filter and binds the new values separately")
        void shouldTranslateUpdate() {
            var result = update(upper);

            assertSoftly(soft -> {
                soft.assertThat(result.query()).isEqualTo("FOR p IN person FILTER (" + UPPER_NAME
                        + " == @nf) UPDATE p WITH { status: @status } IN person RETURN  NEW");
                soft.assertThat(result.values()).isEqualTo(Map.of("nf", "A", "status", "x"));
            });
        }
    }

    @Nested
    @DisplayName("Given query security")
    class Security {

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1==1 //", "\" RETURN 1 //", "\\' ; REMOVE p IN person", "@x} RETURN p"})
        @DisplayName("A hostile value is only bound and never part of the AQL")
        void shouldBindHostileValues(String payload) {
            var result = select(equal(function("UPPER", attribute("name")), constant(payload)));

            assertSoftly(soft -> {
                soft.assertThat(result.query()).doesNotContain(payload);
                soft.assertThat(result.values()).containsValue(payload);
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"name) == 1 //", "na me", "name'", "1name", "a.b c"})
        void shouldRejectInvalidAttributeIdentifier(String name) {
            var condition = equal(function("UPPER", attribute(name)), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid AQL identifier");
        }

        @Test
        void shouldRejectInvalidIdentifierOnPlainAttributeOperand() {
            var condition = equal(attributeOperand("name) == 1 //"), function("UPPER", attribute("name")));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("A huge length is capped instead of overflowing")
        void shouldCapHugeLength() {
            var result = select(equal(function("LEFT", attribute("name"), value(Long.MAX_VALUE)), constant("x")));

            assertThat(result.values().get("nf")).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("Given unsupported or invalid native functions")
    class Unsupported {

        @Test
        void shouldRejectUnknownFunction() {
            var condition = equal(function("REVERSE", attribute("name")), constant("x"));

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("ArangoDB").hasMessageContaining("REVERSE");
                soft.assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> delete(condition)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> update(condition)).isInstanceOf(UnsupportedOperationException.class);
            });
        }

        @Test
        @DisplayName("An unsupported function nested in a group rejects the whole query")
        void shouldRejectInsideGroup() {
            var condition = CriteriaCondition.eq(Element.of("age", 1))
                    .or(equal(function("REVERSE", attribute("name")), constant("x")));

            assertThatThrownBy(() -> delete(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LEFT", "RIGHT"})
        void shouldRejectLengthFromAttribute(String name) {
            var condition = equal(function(name, attribute("name"), attribute("size")), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(name);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LEFT", "RIGHT"})
        void shouldRejectLengthFromNativeFunctionOtherThanLength(String name) {
            var condition = equal(function(name, attribute("name"), nested("ABS", attribute("balance"))),
                    constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LENGTH", "ABS"})
        void shouldRejectLikeOnNumericResult(String name) {
            var condition = compare(function(name, attribute("name")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(name);
        }

        @ParameterizedTest
        @ValueSource(strings = {"CONTAINS", "STARTS_WITH", "ENDS_WITH"})
        @DisplayName("A condition that is not supported with a native function is rejected by the core API")
        void shouldRejectUnsupportedCondition(String condition) {
            var type = Condition.valueOf(condition);

            assertThatThrownBy(() -> compare(function("UPPER", attribute("name")), type, constant("x")))
                    .isInstanceOf(QueryException.class);
        }

        @Test
        @DisplayName("A native function inside IGNORE_CASE keeps the existing rejection of that condition")
        void shouldRejectInsideIgnoreCase() {
            var condition = CriteriaCondition.ignoreCase(equal(function("UPPER", attribute("name")), constant("a")));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectNegativeLength() {
            var condition = equal(function("LEFT", attribute("name"), value(-1)), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("A text where a length is expected is rejected")
        void shouldRejectWrongArgumentType() {
            var condition = equal(function("LEFT", attribute("name"), value("3")), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
