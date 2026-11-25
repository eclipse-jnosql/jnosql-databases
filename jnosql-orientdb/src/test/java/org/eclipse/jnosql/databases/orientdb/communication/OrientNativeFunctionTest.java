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
import org.eclipse.jnosql.communication.QueryException;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
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

@DisplayName("OrientDB native function translation")
class OrientNativeFunctionTest {

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

    private static QueryOSQLConverter.Query select(CriteriaCondition condition) {
        return QueryOSQLConverter.select(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition,
                false));
    }

    private static QueryOSQLConverter.Query count(CriteriaCondition condition) {
        return QueryOSQLConverter.selectCount(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition,
                true));
    }

    private static String where(QueryOSQLConverter.Query query) {
        return query.query().substring(query.query().indexOf(" WHERE ") + 7);
    }

    @Nested
    @DisplayName("Given the supported native functions")
    class Supported {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"UPPER,name.toUpperCase()", "LOWER,name.toLowerCase()"})
        void shouldTranslateStringFunctions(String name, String expression) {
            var query = select(equal(function(name, attribute("name")), constant("x")));

            assertSoftly(soft -> {
                soft.assertThat(query.query()).isEqualTo("SELECT FROM person WHERE (" + expression + " = ?)");
                soft.assertThat(query.params()).containsExactly("x");
                soft.assertThat(query.ids()).isEmpty();
            });
        }

        @Test
        void shouldTranslateAbs() {
            var query = select(equal(function("ABS", attribute("balance")), constant(7)));

            assertSoftly(soft -> {
                soft.assertThat(query.query()).isEqualTo("SELECT FROM person WHERE (abs(balance) = ?)");
                soft.assertThat(query.params()).containsExactly(7);
            });
        }

        @Test
        @DisplayName("A native function on the right side of the comparison")
        void shouldTranslateRightSide() {
            var query = select(equal(attributeOperand("name"), function("UPPER", attribute("otherName"))));

            assertThat(where(query)).isEqualTo("(name = otherName.toUpperCase())");
            assertThat(query.params()).isEmpty();
        }

        @Test
        @DisplayName("Native functions on both sides keep the operand order")
        void shouldTranslateBothSides() {
            var query = select(equal(function("UPPER", attribute("firstName")),
                    function("LOWER", attribute("lastName"))));

            assertThat(where(query)).isEqualTo("(firstName.toUpperCase() = lastName.toLowerCase())");
        }

        @Test
        @DisplayName("Nested supported functions chain the methods in argument order")
        void shouldTranslateNested() {
            var query = select(equal(function("UPPER", nested("LOWER", attribute("name"))), constant("OTA")));

            assertThat(where(query)).isEqualTo("(name.toLowerCase().toUpperCase() = ?)");
        }

        @Test
        void shouldTranslateNestedAbs() {
            var query = select(equal(function("ABS", nested("ABS", attribute("balance"))), constant(1)));

            assertThat(where(query)).isEqualTo("(abs(abs(balance)) = ?)");
        }

        @Test
        @DisplayName("A dotted attribute path is kept")
        void shouldKeepDottedPath() {
            var query = select(equal(function("UPPER", attribute("address.city")), constant("X")));

            assertThat(where(query)).isEqualTo("(address.city.toUpperCase() = ?)");
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"GREATER_THAN,>", "GREATER_EQUALS_THAN,>=", "LESSER_THAN,<", "LESSER_EQUALS_THAN,<="})
        void shouldTranslateOrdering(Condition condition, String operator) {
            var query = select(compare(function("ABS", attribute("balance")), condition, constant(3)));

            assertThat(where(query)).isEqualTo("(abs(balance) " + operator + " ?)");
        }

        @Test
        void shouldTranslateLike() {
            var query = select(compare(function("UPPER", attribute("name")), Condition.LIKE, constant("OT%")));

            assertSoftly(soft -> {
                soft.assertThat(where(query)).isEqualTo("(name.toUpperCase() LIKE ?)");
                soft.assertThat(query.params()).containsExactly("OT%");
            });
        }

        @Test
        void shouldTranslateIn() {
            var query = select(compare(function("UPPER", attribute("name")), Condition.IN,
                    constant(List.of("A", "B"))));

            assertSoftly(soft -> {
                soft.assertThat(where(query)).isEqualTo("(name.toUpperCase() IN ?)");
                soft.assertThat(query.params()).containsExactly(List.of("A", "B"));
            });
        }

        @Test
        @DisplayName("Equality with null is a null check")
        void shouldTranslateNullEquality() {
            var query = select(equal(function("UPPER", attribute("name")), constant(null)));

            assertThat(where(query)).isEqualTo("(name.toUpperCase() IS NULL)");
            assertThat(query.params()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given Boolean grouping and the query operations")
    class Operations {

        private final CriteriaCondition upper = equal(function("UPPER", attribute("name")), constant("A"));
        private final CriteriaCondition plain = CriteriaCondition.eq(Element.of("age", 1));

        @Test
        void shouldKeepGroupingAroundNativePredicates() {
            var query = select(CriteriaCondition.and(CriteriaCondition.or(upper, plain), plain));

            assertSoftly(soft -> {
                soft.assertThat(where(query)).isEqualTo("((name.toUpperCase() = ?) OR age = ?) AND age = ?");
                soft.assertThat(query.params()).containsExactly("A", 1, 1);
            });
        }

        @Test
        void shouldNegateNativePredicate() {
            var query = select(upper.negate());

            assertThat(where(query)).isEqualTo("NOT ((name.toUpperCase() = ?))");
        }

        @Test
        @DisplayName("Parameters of native and ordinary predicates keep the order of appearance")
        void shouldKeepParamOrder() {
            var lower = equal(function("LOWER", attribute("name")), constant("b"));

            var query = select(plain.and(upper).and(lower));

            assertThat(query.params()).containsExactly(1, "A", "b");
        }

        @Test
        void shouldTranslateCount() {
            var query = count(upper);

            assertThat(query.query()).isEqualTo("SELECT COUNT(*) FROM person WHERE (name.toUpperCase() = ?)");
        }

        @Test
        @DisplayName("An id predicate next to a native predicate is not an id lookup")
        void shouldNotBypassWithIdLookup() {
            var query = select(CriteriaCondition.eq(Element.of("_id", "#1:1")).and(upper));

            assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(where(query)).contains("name.toUpperCase()");
            });
        }

        @Test
        @DisplayName("A native predicate is never treated as an id lookup")
        void shouldNotTreatNativeAsIdLookup() {
            var query = select(upper);

            assertThat(query.ids()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given query security")
    class Security {

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1=1 --", "\" ; DELETE VERTEX V", "\\' ; DROP CLASS person", "?"})
        @DisplayName("A hostile value is only bound and never part of the SQL")
        void shouldBindHostileValues(String payload) {
            var query = select(equal(function("UPPER", attribute("name")), constant(payload)));

            assertSoftly(soft -> {
                soft.assertThat(query.query()).doesNotContain(payload.replace("?", "!"));
                soft.assertThat(query.params()).containsExactly(payload);
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"name) = 1 --", "na me", "name'", "1name", "a.b c"})
        void shouldRejectInvalidAttributeIdentifier(String name) {
            var condition = equal(function("UPPER", attribute(name)), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid OrientDB identifier");
        }

        @Test
        void shouldRejectInvalidIdentifierOnPlainAttributeOperand() {
            var condition = equal(attributeOperand("name) = 1 --"), function("UPPER", attribute("name")));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given unsupported native functions")
    class Unsupported {

        @ParameterizedTest
        @ValueSource(strings = {"LENGTH", "LEFT", "RIGHT"})
        @DisplayName("Functions whose OrientDB semantics differ from the contract are rejected")
        void shouldRejectFunctionsWithDifferentSemantics(String name) {
            var arguments = name.equals("LENGTH") ? new FunctionArgument[]{attribute("name")}
                    : new FunctionArgument[]{attribute("name"), value(3)};
            var condition = equal(function(name, arguments), constant("x"));

            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("OrientDB").hasMessageContaining(name);
                soft.assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
            });
        }

        @Test
        @DisplayName("A rejected function nested in a supported one rejects the whole predicate")
        void shouldRejectNested() {
            var condition = equal(function("UPPER", nested("LEFT", attribute("name"), value(3))), constant("OTA"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("LEFT");
        }

        @Test
        @DisplayName("A rejected function on the right side is rejected")
        void shouldRejectRightSide() {
            var condition = equal(attributeOperand("name"), function("LENGTH", attribute("other")));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldRejectUnknownFunction() {
            var condition = equal(function("REVERSE", attribute("name")), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("REVERSE");
        }

        @Test
        @DisplayName("An unsupported function inside a group rejects the whole query")
        void shouldRejectInsideGroup() {
            var condition = CriteriaCondition.eq(Element.of("age", 1))
                    .or(equal(function("LENGTH", attribute("name")), constant(3)));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
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
        @DisplayName("BETWEEN is not supported by the driver and is not added for native functions")
        void shouldRejectBetween() {
            var condition = compare(function("ABS", attribute("balance")), Condition.BETWEEN, constant(List.of(1, 2)));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("BETWEEN");
        }

        @ParameterizedTest
        @ValueSource(strings = {"_id", "@rid"})
        void shouldRejectIdAttributes(String name) {
            var condition = equal(function("UPPER", attribute(name)), constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldRejectLikeOnAbs() {
            var condition = compare(function("ABS", attribute("balance")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ABS");
        }
    }
}
