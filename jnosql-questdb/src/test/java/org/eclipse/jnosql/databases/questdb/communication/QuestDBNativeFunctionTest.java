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
package org.eclipse.jnosql.databases.questdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("QuestDB native function translation to QuestDB SQL")
class QuestDBNativeFunctionTest {

    private static final String TABLE = "sensor_reading";

    private static FunctionArgument attr(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument value(Object value) {
        return new FunctionArgument.ValueArgument(Value.of(value));
    }

    private static FunctionArgument nested(String name, FunctionArgument... arguments) {
        return new FunctionArgument.NativeArgument(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand fn(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand attrOperand(String name) {
        return new CriteriaOperand.Attribute(name);
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition eq(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private record Rendered(String statement, List<Object> parameters) {
    }

    private static Rendered select(CriteriaCondition condition) {
        var query = QuestDBQueryConverter.convert(SelectQuery.builder().select().from(TABLE).where(condition).build());
        return new Rendered(query.statement(), query.parameters());
    }

    private static Rendered count(CriteriaCondition condition) {
        var query = QuestDBQueryConverter.count(SelectQuery.builder().select().from(TABLE).where(condition).build());
        return new Rendered(query.statement(), query.parameters());
    }

    static Stream<Arguments> supported() {
        return Stream.of(
                Arguments.of("UPPER on the attribute", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = $1)",
                        List.of("OTAVIO")),
                Arguments.of("LOWER on the attribute", eq(fn("LOWER", attr("name")), constant("otavio")),
                        "(lower(\"name\") = $1)",
                        List.of("otavio")),
                Arguments.of("LENGTH on the attribute", eq(fn("LENGTH", attr("name")), constant(6)),
                        "(nullif(length(\"name\"), -1) = $1)",
                        List.of(6)),
                Arguments.of("ABS on the attribute", CriteriaCondition.of(fn("ABS", attr("value")), Condition.GREATER_THAN, constant(2)),
                        "(abs(\"value\") > $1)",
                        List.of(2)),
                Arguments.of("native function on the right", eq(attrOperand("name"), fn("UPPER", attr("other"))),
                        "(\"name\" = upper(\"other\"))",
                        List.of()),
                Arguments.of("native functions on both sides", eq(fn("UPPER", attr("name")), fn("LOWER", attr("other"))),
                        "(upper(\"name\") = lower(\"other\"))",
                        List.of()),
                Arguments.of("nested functions", eq(fn("UPPER", nested("LOWER", attr("name"))), constant("OTAVIO")),
                        "(upper(lower(\"name\")) = $1)",
                        List.of("OTAVIO")),
                Arguments.of("evaluable function result as a plain value", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = $1)",
                        List.of("OTAVIO")),
                Arguments.of("IN with a native function", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.IN, constant(List.of("A", "B"))),
                        "(upper(\"name\") IN ($1, $2))",
                        List.of("A", "B")),
                Arguments.of("BETWEEN with a native function", CriteriaCondition.of(fn("ABS", attr("value")), Condition.BETWEEN, constant(List.of(1, 5))),
                        "(abs(\"value\") BETWEEN $1 AND $2)",
                        List.of(1, 5)),
                Arguments.of("LEFT on the attribute", eq(fn("LEFT", attr("name"), value(3)), constant("Ota")),
                        "(left(\"name\", $1) = $2)",
                        List.of(3, "Ota")),
                Arguments.of("RIGHT on the attribute", eq(fn("RIGHT", attr("name"), value(3)), constant("vio")),
                        "(right(\"name\", $1) = $2)",
                        List.of(3, "vio")),
                Arguments.of("nested LEFT in UPPER", eq(fn("UPPER", nested("LEFT", attr("name"), value(3))), constant("OTA")),
                        "(upper(left(\"name\", $1)) = $2)",
                        List.of(3, "OTA")),
                Arguments.of("partial evaluation binds the length as a number", eq(fn("LEFT", attr("name"), value(3)), constant("Ota")),
                        "(left(\"name\", $1) = $2)",
                        List.of(3, "Ota")),
                Arguments.of("LENGTH nested as the length", eq(fn("LEFT", attr("name"), nested("LENGTH", attr("other"))), attrOperand("prefix")),
                        "(left(\"name\", nullif(length(\"other\"), -1)) = \"prefix\")",
                        List.of()));
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
                Arguments.of("LIKE", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.LIKE, constant("O%"))),
                Arguments.of("null comparison", eq(fn("UPPER", attr("name")), constant(null))),
                Arguments.of("temporal identifier as the function argument", eq(fn("UPPER", attr("_id")), constant("A"))),
                Arguments.of("temporal identifier on the other side", eq(attrOperand("_id"), fn("UPPER", attr("name")))),
                Arguments.of("unknown function", eq(new CriteriaOperand.Function(NativeFunction.of("SOUNDEX", attr("name"))), constant("A"))),
                Arguments.of("unsupported function inside an AND group", CriteriaCondition.and(CriteriaCondition.eq(Element.of("value", 1)), eq(new CriteriaOperand.Function(NativeFunction.of("SOUNDEX", attr("name"))), constant("A")))),
                Arguments.of("unsupported function under NOT", eq(new CriteriaOperand.Function(NativeFunction.of("SOUNDEX", attr("name"))), constant("A")).negate()),
                Arguments.of("length from a stored attribute", eq(fn("LEFT", attr("name"), attr("value")), constant("A"))),
                Arguments.of("length from a non LENGTH function", eq(fn("LEFT", attr("name"), nested("ABS", attr("value"))), constant("A"))));
    }

    @Nested
    @DisplayName("Given supported native functions")
    class Supported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.questdb.communication.QuestDBNativeFunctionTest#supported")
        void shouldTranslate(String name, CriteriaCondition condition, String where, List<Object> parameters) {
            var rendered = select(condition);

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE " + where);
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(parameters);
            });
        }

        @Test
        @DisplayName("COUNT uses the same native predicate")
        void shouldTranslateCount() {
            var rendered = count(eq(fn("UPPER", attr("name")), constant("OTAVIO")));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo("SELECT COUNT(*) FROM \"sensor_reading\" WHERE (upper(\"name\") = $1)");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of("OTAVIO"));
            });
        }

        @Test
        @DisplayName("A native predicate keeps the temporal restriction next to it")
        void shouldKeepTemporalRestriction() {
            var ts = Instant.parse("2026-09-08T17:00:00Z");
            var rendered = select(CriteriaCondition.and(CriteriaCondition.eq(Element.of("_id", ts)),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO"))));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE (\"timestamp\" = $1 AND (upper(\"name\") = $2))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of(QuestDBEntityConverter.queryValue("_id", ts), "OTAVIO"));
            });
        }

        @Test
        @DisplayName("OR, AND and NOT keep their grouping around native predicates")
        void shouldKeepBooleanGrouping() {
            var native1 = eq(fn("UPPER", attr("name")), constant("OTAVIO"));
            var native2 = eq(fn("LOWER", attr("other")), constant("x"));
            var value = CriteriaCondition.of(attrOperand("value"), Condition.GREATER_THAN, constant(20.5D));

            var rendered = select(CriteriaCondition.and(CriteriaCondition.or(value, native1), native2.negate()));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE ((\"value\" > $1 OR (upper(\"name\") = $2)) AND NOT ((lower(\"other\") = $3)))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of(20.5D, "OTAVIO", "x"));
            });
        }

        @Test
        @DisplayName("NOT wraps a native predicate")
        void shouldNegate() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("OTAVIO")).negate());

            assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE NOT ((upper(\"name\") = $1))");
        }

        @Test
        @DisplayName("Ordering and limits are appended after the native predicate")
        void shouldKeepOrderAndLimit() {
            var query = new DefaultSelectQuery(5, 2, TABLE, List.of(), List.of(Sort.desc("_id")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")), false);

            assertThat(QuestDBQueryConverter.convert(query).statement()).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE (upper(\"name\") = $1) ORDER BY \"timestamp\" DESC LIMIT $2, $3");
        }

        @Test
        @DisplayName("A hostile value stays a bound parameter")
        void shouldBindHostileValue() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("x') OR 1=1 --")));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo(
                        "SELECT * FROM \"sensor_reading\" WHERE (upper(\"name\") = $1)");
                soft.assertThat(rendered.parameters()).containsExactly("x') OR 1=1 --");
            });
        }

        @Test
        @DisplayName("A hostile attribute name is rejected before a statement exists")
        void shouldRejectHostileAttribute() {
            assertThatThrownBy(() -> select(eq(fn("UPPER", attr("name\") OR 1=1 --")), constant("A"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Given unsupported native functions")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.questdb.communication.QuestDBNativeFunctionTest#unsupported")
        void shouldRejectWithUnsupportedOperation(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
            });
        }

        @Test
        @DisplayName("A negative length keeps the core validation contract and is never sent to the database")
        void shouldRejectNegativeLength() {
            assertThatThrownBy(() -> select(eq(fn("LEFT", attr("name"), value(-1)), constant("A"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("A length beyond the numeric range is clamped because it returns the whole string")
        void shouldClampHugeLength() {
            var rendered = select(eq(fn("RIGHT", attr("name"), value(Long.MAX_VALUE)), constant("A")));

            assertThat(rendered.parameters().get(0)).isEqualTo(Integer.MAX_VALUE);
        }
    }
}
