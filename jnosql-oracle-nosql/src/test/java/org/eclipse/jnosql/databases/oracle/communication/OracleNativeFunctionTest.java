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
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.QueryException;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("Oracle NoSQL native function translation")
class OracleNativeFunctionTest {

    private static final String TABLE = "entities";
    private static final String NAME = "entities.content.name";
    private static final String STRING_GUARD = " IS OF TYPE (ONLY STRING)";

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

    private static CriteriaCondition upperEquals(String value) {
        return compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(value));
    }

    private static String sql(CriteriaCondition condition, List<FieldValue> params) {
        return normalize(OracleNativeFunction.convert(condition, TABLE, params));
    }

    // Oracle NoSQL function names are lowercase, so assertions on the structure ignore the case of the names by lowercasing them
    private static String normalize(String text) {
        return Pattern.compile("\\b(upper|lower|length|substring|abs)\\(", Pattern.CASE_INSENSITIVE)
                .matcher(text).replaceAll(match -> match.group(1).toLowerCase(Locale.ROOT) + "(");
    }

    private static String text(OracleQuery query) {
        return normalize(query.query()).replaceAll("\\s+", " ");
    }

    private static String sql(CriteriaCondition condition) {
        return sql(condition, new ArrayList<>());
    }

    private static OracleQuery select(CriteriaCondition condition) {
        var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false);
        return new SelectBuilder(query, "database").get();
    }

    private static OracleQuery count(CriteriaCondition condition) {
        var query = new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true);
        return new SelectCountBuilder(query, "database").get();
    }

    private static OracleQuery delete(CriteriaCondition condition) {
        return new DeleteBuilder(new DefaultDeleteQuery("person", condition, List.of()), "database").get();
    }

    private static String where(OracleQuery query) {
        var text = text(query);
        return text.substring(text.indexOf(" AND ") + 5);
    }

    private static List<Object> values(List<FieldValue> params) {
        return params.stream().map(FieldValue::getString).map(Object.class::cast).toList();
    }

    private static String guarded(String attribute, String body) {
        return "CASE WHEN " + attribute + STRING_GUARD + " THEN " + body + " ELSE null END";
    }

    @Nested
    @DisplayName("Given a native function on the left side")
    class LeftSide {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"LOWER,lower", "UPPER,upper"})
        void shouldTranslateStringFunctions(String name, String expected) {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function(name, attribute("name")), Condition.EQUALS, constant("x")), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(" + guarded(NAME, expected + "(" + NAME + ")") + " = ?)");
                soft.assertThat(values(params)).containsExactly("x");
            });
        }

        @ParameterizedTest(name = "{0} is emitted in lowercase")
        @CsvSource({"UPPER,upper(", "LOWER,lower(", "LENGTH,length(", "ABS,abs("})
        @DisplayName("Oracle NoSQL function names are case sensitive and must be lowercase in the SQL text")
        void shouldEmitLowercaseFunctionNames(String name, String expected) {
            var raw = OracleNativeFunction.convert(compare(function(name, attribute("name")), Condition.EQUALS,
                    constant("x")), TABLE, new ArrayList<>());

            assertThat(raw).contains(expected).doesNotContain(expected.toUpperCase(Locale.ROOT));
        }

        @Test
        @DisplayName("LEFT and RIGHT are emitted with the lowercase substring function")
        void shouldEmitLowercaseSubstring() {
            var raw = OracleNativeFunction.convert(compare(function("LEFT", attribute("name"),
                    FunctionArgument.literal(2)), Condition.EQUALS, constant("x")), TABLE, new ArrayList<>());

            assertThat(raw).contains("substring(").contains("length(").doesNotContain("SUBSTRING(", "LENGTH(");
        }

        @Test
        void shouldTranslateAbsWithoutGuard() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("ABS", attribute("balance")), Condition.EQUALS, constant(100)), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(abs(entities.content.balance) = ?)");
                soft.assertThat(params).hasSize(1);
                soft.assertThat(params.getFirst().getLong()).isEqualTo(100L);
            });
        }

        @Test
        void shouldTranslateLength() {
            var text = sql(compare(function("LENGTH", attribute("name")), Condition.EQUALS, constant(6)));

            assertThat(text).isEqualTo("(" + guarded(NAME, "length(" + NAME + ")") + " = ?)");
        }

        @Test
        @DisplayName("LEFT inlines the validated length and binds only the constant")
        void shouldTranslateLeft() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("Ota")), params);

            var body = "CASE WHEN length(" + NAME + ") = 0 OR 3 = 0 THEN '' ELSE substring(" + NAME + ", 0, 3) END";
            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(" + guarded(NAME, body) + " = ?)");
                soft.assertThat(values(params)).containsExactly("Ota");
            });
        }

        @Test
        void shouldTranslateRight() {
            var text = sql(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                    Condition.EQUALS, constant("vio")));

            var body = "CASE WHEN length(" + NAME + ") = 0 OR 3 = 0 THEN '' WHEN 3 >= length(" + NAME + ") THEN "
                    + NAME + " ELSE substring(" + NAME + ", length(" + NAME + ") - 3) END";
            assertThat(text).isEqualTo("(" + guarded(NAME, body) + " = ?)");
        }

        @Test
        @DisplayName("A dotted attribute path is kept")
        void shouldKeepDottedAttribute() {
            var text = sql(compare(function("UPPER", attribute("a.b")), Condition.EQUALS, constant("x")));

            assertThat(text).contains("entities.content.a.b");
        }

        @Test
        @DisplayName("The table qualifies the attribute")
        void shouldQualifyWithTable() {
            var text = OracleNativeFunction.convert(upperEquals("x"), "other_table", new ArrayList<>());

            assertThat(text).contains("other_table.content.name").doesNotContain("entities.");
        }
    }

    @Nested
    @DisplayName("Given native functions on the right side and on both sides")
    class OtherSides {

        @Test
        void shouldTranslateRightSide() {
            var text = sql(compare(attributeOperand("name"), Condition.EQUALS, function("UPPER", attribute("other"))));

            assertThat(text).isEqualTo("(" + NAME + " = " + guarded("entities.content.other",
                    "upper(entities.content.other)") + ")");
        }

        @Test
        void shouldTranslateBothSides() {
            var text = sql(compare(function("UPPER", attribute("first")), Condition.EQUALS,
                    function("LOWER", attribute("last"))));

            assertThat(text).isEqualTo("(" + guarded("entities.content.first", "upper(entities.content.first)")
                    + " = " + guarded("entities.content.last", "lower(entities.content.last)") + ")");
        }

        @Test
        @DisplayName("A nested function is a nested expression and the inner result needs no guard")
        void shouldTranslateNested() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("UPPER", nested("LEFT", attribute("name"), FunctionArgument.literal(3))),
                    Condition.EQUALS, constant("OTA")), params);

            assertSoftly(soft -> {
                soft.assertThat(text).startsWith("(upper(CASE WHEN " + NAME + STRING_GUARD + " THEN CASE WHEN length(")
                        .contains("substring(" + NAME + ", 0, 3)");
                soft.assertThat(text).doesNotContain("upper(" + NAME + ")");
                soft.assertThat(values(params)).containsExactly("OTA");
            });
        }

        @Test
        @DisplayName("A nested LENGTH can be the length of LEFT")
        void shouldAcceptLengthAsLength() {
            var text = sql(compare(function("LEFT", attribute("name"), nested("LENGTH", attribute("prefix"))),
                    Condition.EQUALS, constant("Ota")));

            assertThat(text).contains("length(entities.content.prefix)")
                    .contains("substring(" + NAME + ", 0, CASE WHEN entities.content.prefix" + STRING_GUARD
                            + " THEN length(entities.content.prefix) ELSE null END)");
        }

        @Test
        @DisplayName("Fragments that repeat a bound text repeat its params, in order of appearance")
        void shouldRepeatParamsOfRepeatedFragments() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LEFT", value("ota"), nested("LENGTH", attribute("size"))),
                    Condition.EQUALS, attributeOperand("name")), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(CASE WHEN length(?) = 0 OR "
                        + guarded("entities.content.size", "length(entities.content.size)") + " = 0 THEN '' ELSE "
                        + "substring(?, 0, " + guarded("entities.content.size", "length(entities.content.size)")
                        + ") END = " + NAME + ")");
                soft.assertThat(values(params)).containsExactly("ota", "ota");
            });
        }

        @Test
        @DisplayName("RIGHT repeats its text param in every use and the constant comes last")
        void shouldRepeatParamsOfRight() {
            var params = new ArrayList<FieldValue>();

            sql(compare(function("RIGHT", value("ota"), nested("LENGTH", attribute("size"))), Condition.EQUALS,
                    constant("end")), params);

            assertThat(values(params)).containsExactly("ota", "ota", "ota", "ota", "ota", "end");
        }

        @Test
        @DisplayName("Params keep the order of the left function, the right function and the constants")
        void shouldKeepParamOrderAcrossOperands() {
            var params = new ArrayList<FieldValue>();
            var condition = compare(function("LEFT", value("abc"), nested("LENGTH", attribute("size"))),
                    Condition.EQUALS, function("RIGHT", value("xyz"), nested("LENGTH", attribute("size"))));

            sql(condition, params);

            assertThat(values(params)).containsExactly("abc", "abc", "xyz", "xyz", "xyz", "xyz", "xyz");
        }
    }

    @Nested
    @DisplayName("Given the supported predicates")
    class Predicates {

        @ParameterizedTest(name = "{0} is translated to {1}")
        @CsvSource({"GREATER_THAN,>", "GREATER_EQUALS_THAN,>=", "LESSER_THAN,<", "LESSER_EQUALS_THAN,<="})
        void shouldTranslateOrdering(Condition condition, String operator) {
            var text = sql(compare(function("ABS", attribute("balance")), condition, constant(1)));

            assertThat(text).isEqualTo("(abs(entities.content.balance) " + operator + " ?)");
        }

        @Test
        void shouldTranslateLike() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LOWER", attribute("name")), Condition.LIKE, constant("ja%")), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(regex_like(" + guarded(NAME, "lower(" + NAME + ")") + ", ?))");
                soft.assertThat(values(params)).containsExactly(OracleNoSqlLikeConverter.INSTANCE.convert("ja%"));
            });
        }

        @Test
        void shouldTranslateIn() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LOWER", attribute("name")), Condition.IN,
                    constant(List.of("a", "b"))), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(" + guarded(NAME, "lower(" + NAME + ")") + " IN ?[])");
                soft.assertThat(params).hasSize(1);
                soft.assertThat(params.getFirst().isArray()).isTrue();
                soft.assertThat(params.getFirst().asArray().size()).isEqualTo(2);
            });
        }

        @Test
        void shouldTranslateBetween() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("ABS", attribute("balance")), Condition.BETWEEN,
                    constant(List.of(1, 5))), params);

            assertSoftly(soft -> {
                soft.assertThat(text).isEqualTo("(abs(entities.content.balance) BETWEEN ? AND ?)");
                soft.assertThat(params).extracting(FieldValue::getLong).containsExactly(1L, 5L);
            });
        }

        @Test
        @DisplayName("Equality with null matches null, JSON null and missing values")
        void shouldTranslateNullEquality() {
            var text = sql(compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(null)));

            var expression = guarded(NAME, "upper(" + NAME + ")");
            assertThat(text).isEqualTo("((" + expression + " = null OR " + expression + " IS NULL OR NOT EXISTS "
                    + expression + "))");
        }

        @Test
        @DisplayName("A function that receives a null literal results in null")
        void shouldPropagateNull() {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LEFT", attribute("name"), FunctionArgument.literal(null)),
                    Condition.EQUALS, constant("x")), params);

            assertThat(text).startsWith("(null = ?)");
        }

        @Test
        @DisplayName("A native predicate is parenthesized")
        void shouldParenthesize() {
            assertThat(sql(upperEquals("x"))).startsWith("(").endsWith(")");
        }
    }

    @Nested
    @DisplayName("Given query builders")
    class Builders {

        private final CriteriaCondition upper = upperEquals("A");
        private final CriteriaCondition plain = CriteriaCondition.eq(Element.of("code", "1"));
        private final String upperSql = "(" + guarded("database.content.name", "upper(database.content.name)")
                + " = ?)";

        @Test
        @DisplayName("SELECT appends the native predicate after the entity condition and binds in order")
        void shouldTranslateSelect() {
            var query = select(upper);

            assertSoftly(soft -> {
                soft.assertThat(text(query)).startsWith("select * from database WHERE "
                        + "database.entity= ? AND " + upperSql);
                soft.assertThat(values(query.bindings())).containsExactly("person", "A");
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.hasIds()).isFalse();
            });
        }

        @Test
        void shouldTranslateCount() {
            var query = count(upper);

            assertSoftly(soft -> {
                soft.assertThat(text(query)).startsWith("select count(*) as count from "
                        + "database WHERE database.entity= ? AND " + upperSql);
                soft.assertThat(values(query.bindings())).containsExactly("person", "A");
            });
        }

        @Test
        void shouldTranslateDelete() {
            var query = delete(upper);

            assertSoftly(soft -> {
                soft.assertThat(text(query)).startsWith("DELETE from database WHERE "
                        + "database.entity= ? AND " + upperSql);
                soft.assertThat(values(query.bindings())).containsExactly("person", "A");
                soft.assertThat(query.hasIds()).isFalse();
            });
        }

        @Test
        void shouldKeepGroupingAroundNativePredicates() {
            var query = select(CriteriaCondition.and(CriteriaCondition.or(upper, plain), plain));

            assertThat(where(query)).startsWith("((" + upperSql + " OR");
            assertThat(values(query.bindings())).containsExactly("person", "A", "1", "1");
        }

        @Test
        void shouldNegateNativePredicate() {
            var query = select(upper.negate());

            assertThat(where(query)).isEqualTo("NOT " + upperSql);
        }

        @Test
        @DisplayName("Two native predicates keep the order of their params")
        void shouldKeepParamOrderBetweenPredicates() {
            var other = compare(function("LOWER", attribute("name")), Condition.EQUALS, constant("b"));

            var query = select(upper.or(other));

            assertThat(values(query.bindings())).containsExactly("person", "A", "b");
        }

        @Test
        @DisplayName("A native predicate is combined with an ordinary one")
        void shouldCombineWithOrdinary() {
            var query = select(upper.and(CriteriaCondition.gt("balance", 0)));

            assertSoftly(soft -> {
                soft.assertThat(where(query)).contains("database.content.balance >");
                soft.assertThat(query.params()).hasSize(2);
                soft.assertThat(query.params().getFirst().getString()).isEqualTo("A");
            });
        }

        @Test
        @DisplayName("An id predicate in an AND with a native predicate is not an id lookup")
        void shouldNotBypassWithIdLookupInAnd() {
            var query = select(upper.and(CriteriaCondition.eq("_id", "1")));

            assertSoftly(soft -> {
                soft.assertThat(query.hasIds()).isFalse();
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.hasOnlyIds()).isFalse();
                soft.assertThat(text(query)).contains(" upper(");
            });
        }

        @Test
        @DisplayName("An id predicate in an OR with a native predicate is not an id lookup")
        void shouldNotBypassWithIdLookupInOr() {
            var query = select(upper.or(CriteriaCondition.in("_id", List.of("1", "2"))));

            assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.hasIds()).isFalse();
            });
        }

        @Test
        @DisplayName("A native predicate on the id attribute is rejected and does not become an id lookup")
        void shouldRejectIdAttribute() {
            var condition = compare(function("UPPER", attribute("_id")), Condition.EQUALS, constant("A"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("_id");
        }

        @Test
        void shouldRejectIdAttributeOnTheRightSide() {
            var condition = compare(attributeOperand("_id"), Condition.EQUALS, function("UPPER", attribute("name")));

            assertThatThrownBy(() -> delete(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("_id");
        }

        @Test
        void shouldRejectIdAttributeInCount() {
            var condition = compare(function("LENGTH", attribute("_id")), Condition.EQUALS, constant(1));

            assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("A plain id lookup still uses the id fast path")
        void shouldKeepPlainIdLookup() {
            var query = select(CriteriaCondition.eq("_id", "1"));

            assertThat(query.ids()).containsExactly("1");
        }
    }

    @Nested
    @DisplayName("Given unsupported or invalid native functions")
    class Unsupported {

        @Test
        void shouldRejectUnknownFunction() {
            var condition = compare(function("REVERSE", attribute("name")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Oracle NoSQL").hasMessageContaining("REVERSE");
        }

        @Test
        void shouldRejectUnknownFunctionInDelete() {
            var condition = compare(function("REVERSE", attribute("name")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> delete(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldRejectUnknownFunctionInCount() {
            var condition = compare(function("REVERSE", attribute("name")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LEFT", "RIGHT"})
        void shouldRejectLengthFromAttribute(String name) {
            var condition = compare(function(name, attribute("name"), attribute("size")), Condition.EQUALS,
                    constant("x"));

            assertThatThrownBy(() -> select(condition))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Oracle NoSQL").hasMessageContaining(name);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LEFT", "RIGHT"})
        void shouldRejectLengthFromNativeFunctionOtherThanLength(String name) {
            var condition = compare(function(name, attribute("name"), nested("ABS", attribute("balance"))),
                    Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining(name);
        }

        @Test
        void shouldRejectLikeOnNumericResult() {
            var condition = compare(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> select(condition))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("LENGTH");
        }

        @Test
        void shouldRejectLikeOnAbs() {
            var condition = compare(function("ABS", attribute("balance")), Condition.LIKE, constant("1%"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"CONTAINS", "STARTS_WITH", "ENDS_WITH"})
        @DisplayName("A condition that is not supported with a native function is rejected by the core API")
        void shouldRejectUnsupportedCondition(String condition) {
            var type = Condition.valueOf(condition);

            assertThatThrownBy(() -> compare(function("UPPER", attribute("name")), type, constant("x")))
                    .isInstanceOf(QueryException.class).hasMessageContaining(condition);
        }

        @Test
        @DisplayName("A native function inside IGNORE_CASE is rejected")
        void shouldRejectInsideIgnoreCase() {
            var condition = CriteriaCondition.ignoreCase(upperEquals("a"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ignore case");
        }

        @Test
        @DisplayName("IN accepts only a value as the right operand, which the core API enforces")
        void shouldRejectFunctionAsRightOperandOfIn() {
            assertThatThrownBy(() -> compare(function("UPPER", attribute("name")), Condition.IN,
                    function("LOWER", attribute("other")))).isInstanceOf(QueryException.class);
        }

        @Test
        void shouldRejectNegativeLength() {
            var condition = compare(function("LEFT", attribute("name"), FunctionArgument.literal(-1)),
                    Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        void shouldRejectNegativeValueLength() {
            var condition = compare(function("RIGHT", attribute("name"), value(-5)), Condition.EQUALS,
                    constant("x"));

            assertThatThrownBy(() -> delete(condition)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("A text where a length is expected is rejected")
        void shouldRejectWrongArgumentType() {
            var condition = compare(function("LEFT", attribute("name"), value("x")), Condition.EQUALS, constant(1));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LEFT");
        }

        @Test
        @DisplayName("A huge length is capped instead of overflowing")
        void shouldCapHugeLength() {
            var text = sql(compare(function("LEFT", attribute("name"), FunctionArgument.literal(Long.MAX_VALUE)),
                    Condition.EQUALS, constant("x")));

            assertThat(text).contains(Integer.toString(Integer.MAX_VALUE)).doesNotContain("-");
        }

        @ParameterizedTest
        @ValueSource(strings = {"a b", "a-b", "a.", ".a", "a..b", "1a", "a'", "a) OR (1=1", "a; DROP TABLE x"})
        void shouldRejectInvalidAttributeIdentifier(String name) {
            var condition = compare(function("UPPER", attribute(name)), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Oracle NoSQL identifier");
        }

        @Test
        void shouldRejectInvalidIdentifierOnPlainAttributeOperand() {
            var condition = compare(attributeOperand("a b"), Condition.EQUALS, function("UPPER", attribute("name")));

            assertThatThrownBy(() -> select(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given hostile values")
    class Injection {

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1=1 --", "\\' OR '1'='1", "'; DROP TABLE x; --", "é😀\"`", "?", ") OR (1=1"})
        void shouldBindConstantsAsParameters(String payload) {
            var params = new ArrayList<FieldValue>();

            var text = sql(upperEquals(payload), params);

            assertSoftly(soft -> {
                soft.assertThat(text).doesNotContain(payload.replace("?", "ä"));
                soft.assertThat(values(params)).containsExactly(payload);
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"' OR 1=1 --", "\\' OR '1'='1", "é😀\""})
        void shouldBindFunctionArgumentsAsParameters(String payload) {
            var params = new ArrayList<FieldValue>();

            var text = sql(compare(function("LEFT", value(payload), nested("LENGTH", attribute("size"))),
                    Condition.EQUALS, constant("x")), params);

            assertSoftly(soft -> {
                soft.assertThat(text).doesNotContain(payload);
                soft.assertThat(values(params)).containsExactly(payload, payload, "x");
            });
        }

        @Test
        void shouldBindLikePatternAndInValues() {
            var like = new ArrayList<FieldValue>();
            var in = new ArrayList<FieldValue>();

            var likeText = sql(compare(function("UPPER", attribute("name")), Condition.LIKE, constant("' OR 1=1 --")),
                    like);
            var inText = sql(compare(function("UPPER", attribute("name")), Condition.IN,
                    constant(List.of("' OR 1=1 --", "x"))), in);

            assertSoftly(soft -> {
                soft.assertThat(likeText).doesNotContain("OR 1=1");
                soft.assertThat(inText).doesNotContain("OR 1=1");
                soft.assertThat(like).hasSize(1);
                soft.assertThat(in).hasSize(1);
            });
        }

        @Test
        @DisplayName("Hostile attribute names never reach the SQL text")
        void shouldRejectHostileAttributeNames() {
            var condition = compare(function("UPPER", attribute("a` OR 1=1 --")), Condition.EQUALS, constant("x"));

            assertThatThrownBy(() -> sql(condition)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
