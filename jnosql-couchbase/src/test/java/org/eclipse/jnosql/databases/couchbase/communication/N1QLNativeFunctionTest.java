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
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Couchbase native function translation")
class N1QLNativeFunctionTest {

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

    private static N1QLQuery select(CriteriaCondition condition, boolean count) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("person");
        when(query.condition()).thenReturn(Optional.of(condition));
        when(query.sorts()).thenReturn(List.of());
        when(query.columns()).thenReturn(List.of());
        return new N1QLSelectQueryBuilder(query, "bucket", "scope", count).get();
    }

    private static N1QLQuery update(CriteriaCondition condition) {
        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("person");
        when(query.set()).thenReturn(List.of(Element.of("status", "x")));
        when(query.condition()).thenReturn(Optional.of(condition));
        return new N1QLUpdateQueryBuilder(query, "bucket", "scope").get();
    }

    private static String where(N1QLQuery query) {
        var n1ql = query.query().replaceAll("\\s+", " ").replace("( ", "(");
        return n1ql.substring(n1ql.indexOf("WHERE ") + 6);
    }

    @Nested
    @DisplayName("Given a native function on the left side")
    class LeftSide {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"ABS,ABS(`name`)", "LOWER,LOWER(`name`)", "UPPER,UPPER(`name`)"})
        void shouldTranslateDirectFunctions(String name, String expected) {
            var query = select(compare(function(name, attribute("name")), Condition.EQUALS, constant("x")), false);

            assertSoftly(soft -> {
                soft.assertThat(where(query)).isEqualTo("(" + expected + " = $p0)");
                soft.assertThat(query.params().get("$p0")).isEqualTo("x");
            });
        }

        @Test
        @DisplayName("LENGTH counts code points instead of the bytes returned by SQL++ LENGTH")
        void shouldTranslateLength() {
            var query = select(compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(6)), false);

            assertThat(where(query)).isEqualTo(
                    "(CASE WHEN ISSTRING(`name`) THEN ARRAY_LENGTH(SPLIT(`name`, '')) END = $p0)");
        }

        @Test
        @DisplayName("LEFT keeps the argument order and binds the length")
        void shouldTranslateLeft() {
            var query = select(compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("Ota")), false);

            assertSoftly(soft -> {
                soft.assertThat(where(query)).contains("SUBSTR(`name`, 0, IFNULL(ARRAY_SUM(ARRAY LENGTH(c0) FOR "
                        + "i0:c0 IN SPLIT(`name`, '') WHEN i0 < $p0 END), 0))");
                soft.assertThat(query.params().getInt("$p0")).isEqualTo(3);
                soft.assertThat(query.params().getString("$p1")).isEqualTo("Ota");
            });
        }

        @Test
        @DisplayName("RIGHT cuts the bytes of the last code points")
        void shouldTranslateRight() {
            var query = select(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("vio")), false);

            assertThat(where(query)).contains("ARRAY_REVERSE(SPLIT(`name`, ''))")
                    .contains("SUBSTR(`name`, LENGTH(`name`) - IFNULL(ARRAY_SUM");
        }

        @Test
        @DisplayName("A dotted attribute is quoted segment by segment")
        void shouldQuoteDottedAttribute() {
            var query = select(compare(function("UPPER", attribute("a.b`c")), Condition.EQUALS, constant("x")),
                    false);

            assertThat(where(query)).isEqualTo("(UPPER(`a`.`b``c`) = $p0)");
        }

        @Test
        @DisplayName("The attribute is qualified with the alias in an UPDATE")
        void shouldQualifyInUpdate() {
            var query = update(compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("X")));

            assertThat(where(query)).startsWith("(UPPER(d.`name`) = $p1)");
        }
    }

    @Nested
    @DisplayName("Given native functions on the right side and on both sides")
    class OtherSides {

        @Test
        @DisplayName("should translate a function on the right side")
        void shouldTranslateRight() {
            var query = select(compare(new CriteriaOperand.Attribute("name"), Condition.EQUALS,
                    function("UPPER", attribute("otherName"))), false);

            assertThat(where(query)).isEqualTo("(`name` = UPPER(`otherName`))");
        }

        @Test
        @DisplayName("should translate functions on both sides")
        void shouldTranslateBothSides() {
            var query = select(compare(function("UPPER", attribute("firstName")), Condition.EQUALS,
                    function("UPPER", attribute("lastName"))), false);

            assertThat(where(query)).isEqualTo("(UPPER(`firstName`) = UPPER(`lastName`))");
        }

        @Test
        @DisplayName("should keep a nested function as a nested expression")
        void shouldTranslateNested() {
            var inner = NativeFunction.of("LEFT", attribute("name"), FunctionArgument.literal(3));
            var query = select(compare(function("UPPER", new FunctionArgument.NativeArgument(inner)),
                    Condition.EQUALS, constant("OTA")), false);

            assertThat(where(query)).startsWith("(UPPER(CASE WHEN ISSTRING(`name`) THEN IFNULL(SUBSTR(`name`");
        }

        @Test
        @DisplayName("should use a nested LENGTH as the length of LEFT")
        void shouldAcceptLengthAsLength() {
            var length = NativeFunction.of("LENGTH", attribute("prefix"));
            var query = select(compare(function("LEFT", attribute("name"), new FunctionArgument.NativeArgument(
                    length)), Condition.EQUALS, constant("Ota")), false);

            assertThat(where(query)).contains("WHEN i0 < CASE WHEN ISSTRING(`prefix`)");
        }

        @Test
        @DisplayName("should use an evaluated value as an argument")
        void shouldUseValueArgument() {
            var length = NativeFunction.of("LENGTH", attribute("size"));
            var query = select(compare(function("LEFT", new FunctionArgument.ValueArgument(Value.of("ota")),
                    new FunctionArgument.NativeArgument(length)), Condition.EQUALS,
                    new CriteriaOperand.Attribute("name")), false);

            assertSoftly(soft -> {
                soft.assertThat(where(query)).contains("SPLIT($p0, '')").contains("WHEN i0 < CASE WHEN ISSTRING(`size`)");
                soft.assertThat(query.params().getString("$p0")).isEqualTo("ota");
            });
        }
    }

    @Nested
    @DisplayName("Given the supported predicates")
    class Predicates {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"GREATER_THAN,>", "GREATER_EQUALS_THAN,>=", "LESSER_THAN,<", "LESSER_EQUALS_THAN,<="})
        void shouldTranslateOrdering(Condition condition, String operator) {
            var query = select(compare(function("ABS", attribute("balance")), condition, constant(1)), false);

            assertThat(where(query)).isEqualTo("(ABS(`balance`) " + operator + " $p0)");
        }

        @Test
        void shouldTranslateLike() {
            var query = select(compare(function("LOWER", attribute("name")), Condition.LIKE, constant("ja%")), false);

            assertThat(where(query)).isEqualTo("(LOWER(`name`) LIKE $p0)");
        }

        @Test
        void shouldTranslateIn() {
            var query = select(compare(function("LOWER", attribute("name")), Condition.IN,
                    constant(List.of("a", "b"))), false);

            assertSoftly(soft -> {
                soft.assertThat(where(query)).isEqualTo("(LOWER(`name`) IN $p0)");
                soft.assertThat(query.params().getArray("$p0").toList()).containsExactly("a", "b");
            });
        }

        @Test
        void shouldTranslateBetween() {
            var query = select(compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                    constant(List.of(1, 5))), false);

            assertThat(where(query)).isEqualTo("(ABS(`balance`) BETWEEN $p0 AND $p1)");
        }

        @Test
        @DisplayName("Equality with null tests for a missing or null value")
        void shouldTranslateNullEquality() {
            var query = select(compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(null)),
                    false);

            assertThat(where(query)).isEqualTo("(UPPER(`name`) IS NOT VALUED)");
        }

        @Test
        @DisplayName("A function that receives a null value results in NULL")
        void shouldPropagateNull() {
            var query = select(compare(function("LEFT", attribute("name"), FunctionArgument.literal(null)),
                    Condition.EQUALS, constant("x")), false);

            assertThat(where(query)).startsWith("(NULL = ");
        }
    }

    @Nested
    @DisplayName("Given Boolean grouping and query operations")
    class Grouping {

        private final CriteriaCondition upper = compare(function("UPPER", attribute("name")), Condition.EQUALS,
                constant("A"));
        private final CriteriaCondition plain = CriteriaCondition.eq(Element.of("id", "1"));

        @Test
        void shouldKeepGroupingAroundNativePredicates() {
            var query = select(CriteriaCondition.and(CriteriaCondition.or(upper, plain), plain), false);

            assertThat(where(query)).startsWith("((UPPER(`name`) = $p0) OR `id` = $p1) AND `id` = $p2");
        }

        @Test
        void shouldNegateNativePredicate() {
            var query = select(upper.negate(), false);

            assertThat(where(query)).isEqualTo("NOT ((UPPER(`name`) = $p0))");
        }

        @Test
        @DisplayName("An id predicate is never used as an id lookup when it is a native function")
        void shouldNotBypassWithIdLookup() {
            var condition = compare(function("UPPER", attribute("_id")), Condition.EQUALS, constant("A"));

            var query = select(condition, false);

            assertSoftly(soft -> {
                soft.assertThat(query.hasIds()).isFalse();
                soft.assertThat(where(query)).isEqualTo("(UPPER(`_id`) = $p0)");
            });
        }

        @Test
        void shouldTranslateInCount() {
            var query = select(upper, true);

            assertThat(query.query()).contains("COUNT(*)").contains("UPPER(`name`)");
        }
    }

    @Nested
    @DisplayName("Given unsupported or invalid native functions")
    class Unsupported {

        @Test
        void shouldRejectUnknownFunction() {
            var condition = compare(function("REVERSE", attribute("name")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition, false))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Couchbase").hasMessageContaining("REVERSE");
        }

        @Test
        void shouldRejectUnknownFunctionInUpdate() {
            var condition = compare(function("REVERSE", attribute("name")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> update(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldRejectLengthFromAttribute() {
            var condition = compare(function("LEFT", attribute("name"), attribute("size")), Condition.EQUALS,
                    constant("x"));

            assertThatThrownBy(() -> select(condition, false))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Couchbase").hasMessageContaining("LEFT");
        }

        @Test
        void shouldRejectLengthFromNativeFunctionOtherThanLength() {
            var abs = NativeFunction.of("ABS", attribute("balance"));
            var condition = compare(function("RIGHT", attribute("name"), new FunctionArgument.NativeArgument(abs)),
                    Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition, false))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("RIGHT");
        }

        @Test
        void shouldRejectLikeOnNumericResult() {
            var condition = compare(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> select(condition, false))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("LENGTH");
        }

        @Test
        void shouldRejectNegativeLength() {
            assertThatThrownBy(() -> select(compare(function("LEFT", attribute("name"),
                    FunctionArgument.literal(-1)), Condition.EQUALS, constant("x")), false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectWrongArgumentType() {
            assertThatThrownBy(() -> select(compare(function("LEFT", attribute("name"),
                    FunctionArgument.literal("x")), Condition.EQUALS, constant(1)), false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given hostile values")
    class Injection {

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1=1 --", "\\' OR '1'='1", "'; DROP COLLECTION x; --", "é😀\"`", "$p0"})
        void shouldBindConstantsAsParameters(String payload) {
            var query = select(compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload)),
                    false);

            assertSoftly(soft -> {
                soft.assertThat(query.query()).doesNotContain(payload.replace("$p0", "ä"));
                soft.assertThat(query.params().getString("$p0")).isEqualTo(payload);
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1=1 --", "\\' OR '1'='1", "é😀\""})
        void shouldBindFunctionArgumentsAsParameters(String payload) {
            var length = NativeFunction.of("LENGTH", attribute("size"));
            var query = select(compare(function("LEFT", new FunctionArgument.ValueArgument(Value.of(payload)),
                    new FunctionArgument.NativeArgument(length)), Condition.EQUALS, constant("x")), false);

            assertSoftly(soft -> {
                soft.assertThat(query.query()).doesNotContain(payload);
                soft.assertThat(query.params().getString("$p0")).isEqualTo(payload);
            });
        }

        @Test
        void shouldQuoteHostileAttributeNames() {
            var query = select(compare(function("UPPER", attribute("a` OR 1=1 --")), Condition.EQUALS,
                    constant("x")), false);

            assertThat(where(query)).isEqualTo("(UPPER(`a`` OR 1=1 --`) = $p0)");
        }
    }
}
