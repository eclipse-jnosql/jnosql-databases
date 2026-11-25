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
package org.eclipse.jnosql.databases.influxdb.communication;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("InfluxDB native function translation to InfluxDB 3 SQL")
class InfluxDBNativeFunctionTest {

    private static final String TABLE = "temperature";

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
        var query = InfluxDBQueryConverter.convert(SelectQuery.builder().select().from(TABLE).where(condition).build());
        return new Rendered(query.statement(), new ArrayList<>(query.parameters().values()));
    }

    private static Rendered count(CriteriaCondition condition) {
        var query = InfluxDBQueryConverter.count(SelectQuery.builder().select().from(TABLE).where(condition).build());
        return new Rendered(query.statement(), new ArrayList<>(query.parameters().values()));
    }

    static Stream<Arguments> supported() {
        return Stream.of(
                Arguments.of("UPPER on the attribute", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = $p0)",
                        List.of("OTAVIO")),
                Arguments.of("LOWER on the attribute", eq(fn("LOWER", attr("name")), constant("otavio")),
                        "(lower(\"name\") = $p0)",
                        List.of("otavio")),
                Arguments.of("LENGTH on the attribute", eq(fn("LENGTH", attr("name")), constant(6)),
                        "(char_length(\"name\") = $p0)",
                        List.of(6)),
                Arguments.of("ABS on the attribute", CriteriaCondition.of(fn("ABS", attr("value")), Condition.GREATER_THAN, constant(2)),
                        "(abs(\"value\") > $p0)",
                        List.of(2)),
                Arguments.of("native function on the right", eq(attrOperand("name"), fn("UPPER", attr("other"))),
                        "(\"name\" = upper(\"other\"))",
                        List.of()),
                Arguments.of("native functions on both sides", eq(fn("UPPER", attr("name")), fn("LOWER", attr("other"))),
                        "(upper(\"name\") = lower(\"other\"))",
                        List.of()),
                Arguments.of("nested functions", eq(fn("UPPER", nested("LOWER", attr("name"))), constant("OTAVIO")),
                        "(upper(lower(\"name\")) = $p0)",
                        List.of("OTAVIO")),
                Arguments.of("evaluable function result as a plain value", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = $p0)",
                        List.of("OTAVIO")),
                Arguments.of("IN with a native function", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.IN, constant(List.of("A", "B"))),
                        "(upper(\"name\") IN ($p0, $p1))",
                        List.of("A", "B")),
                Arguments.of("BETWEEN with a native function", CriteriaCondition.of(fn("ABS", attr("value")), Condition.BETWEEN, constant(List.of(1, 5))),
                        "(abs(\"value\") BETWEEN $p0 AND $p1)",
                        List.of(1, 5)),
                Arguments.of("LEFT on the attribute", eq(fn("LEFT", attr("name"), value(3)), constant("Ota")),
                        "(left(\"name\", $p0) = $p1)",
                        List.of(3L, "Ota")),
                Arguments.of("RIGHT on the attribute", eq(fn("RIGHT", attr("name"), value(3)), constant("vio")),
                        "(right(\"name\", $p0) = $p1)",
                        List.of(3L, "vio")),
                Arguments.of("nested LEFT in UPPER", eq(fn("UPPER", nested("LEFT", attr("name"), value(3))), constant("OTA")),
                        "(upper(left(\"name\", $p0)) = $p1)",
                        List.of(3L, "OTA")),
                Arguments.of("partial evaluation binds the length as a number", eq(fn("LEFT", attr("name"), value(3)), constant("Ota")),
                        "(left(\"name\", $p0) = $p1)",
                        List.of(3L, "Ota")),
                Arguments.of("LENGTH nested as the length", eq(fn("LEFT", attr("name"), nested("LENGTH", attr("other"))), attrOperand("prefix")),
                        "(left(\"name\", char_length(\"other\")) = \"prefix\")",
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
        @MethodSource("org.eclipse.jnosql.databases.influxdb.communication.InfluxDBNativeFunctionTest#supported")
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
                soft.assertThat(rendered.statement()).isEqualTo("SELECT COUNT(*) AS \"jnosql_count\" FROM \"temperature\" WHERE (upper(\"name\") = $p0)");
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
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE (\"time\" = $p0 AND (upper(\"name\") = $p1))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of("2026-09-08T17:00:00Z", "OTAVIO"));
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
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE ((\"value\" > $p0 OR (upper(\"name\") = $p1)) AND NOT ((lower(\"other\") = $p2)))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of(20.5D, "OTAVIO", "x"));
            });
        }

        @Test
        @DisplayName("NOT wraps a native predicate")
        void shouldNegate() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("OTAVIO")).negate());

            assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE NOT ((upper(\"name\") = $p0))");
        }

        @Test
        @DisplayName("Ordering and limits are appended after the native predicate")
        void shouldKeepOrderAndLimit() {
            var query = new DefaultSelectQuery(5, 2, TABLE, List.of(), List.of(Sort.desc("_id")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")), false);

            assertThat(InfluxDBQueryConverter.convert(query).statement()).isEqualTo("SELECT * FROM \"temperature\" WHERE (upper(\"name\") = $p0) ORDER BY \"time\" DESC LIMIT 5 OFFSET 2");
        }

        @Test
        @DisplayName("A hostile value stays a bound parameter")
        void shouldBindHostileValue() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("x') OR 1=1 --")));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo(
                        "SELECT * FROM \"temperature\" WHERE (upper(\"name\") = $p0)");
                soft.assertThat(rendered.parameters()).containsExactly("x') OR 1=1 --");
            });
        }

        @Test
        @DisplayName("A quote in an attribute name is doubled inside the identifier")
        void shouldEscapeIdentifier() {
            var rendered = select(eq(fn("UPPER", attr("na\"me")), constant("A")));

            assertThat(rendered.statement()).isEqualTo(
                    "SELECT * FROM \"temperature\" WHERE (upper(\"na\"\"me\") = $p0)");
        }
    }

    @Nested
    @DisplayName("Given unsupported native functions")
    class Unsupported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.influxdb.communication.InfluxDBNativeFunctionTest#unsupported")
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

            assertThat(rendered.parameters().getFirst()).isEqualTo(Long.MAX_VALUE);
        }
    }
}
