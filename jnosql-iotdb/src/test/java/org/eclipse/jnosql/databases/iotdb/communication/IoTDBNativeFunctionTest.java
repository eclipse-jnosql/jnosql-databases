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
package org.eclipse.jnosql.databases.iotdb.communication;

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

@DisplayName("IoTDB native function translation to IoTDB table model SQL")
class IoTDBNativeFunctionTest {

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
        return new Rendered(IoTDBQueryConverter.convert(SelectQuery.builder().select().from(TABLE)
                .where(condition).build()), List.of());
    }

    private static Rendered count(CriteriaCondition condition) {
        return new Rendered(IoTDBQueryConverter.count(SelectQuery.builder().select().from(TABLE)
                .where(condition).build()), List.of());
    }

    static Stream<Arguments> supported() {
        return Stream.of(
                Arguments.of("UPPER on the attribute", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = 'OTAVIO')",
                        List.of()),
                Arguments.of("LOWER on the attribute", eq(fn("LOWER", attr("name")), constant("otavio")),
                        "(lower(\"name\") = 'otavio')",
                        List.of()),
                Arguments.of("LENGTH on the attribute", eq(fn("LENGTH", attr("name")), constant(6)),
                        "(length(\"name\") = 6)",
                        List.of()),
                Arguments.of("ABS on the attribute", CriteriaCondition.of(fn("ABS", attr("value")), Condition.GREATER_THAN, constant(2)),
                        "(abs(\"value\") > 2)",
                        List.of()),
                Arguments.of("native function on the right", eq(attrOperand("name"), fn("UPPER", attr("other"))),
                        "(\"name\" = upper(\"other\"))",
                        List.of()),
                Arguments.of("native functions on both sides", eq(fn("UPPER", attr("name")), fn("LOWER", attr("other"))),
                        "(upper(\"name\") = lower(\"other\"))",
                        List.of()),
                Arguments.of("nested functions", eq(fn("UPPER", nested("LOWER", attr("name"))), constant("OTAVIO")),
                        "(upper(lower(\"name\")) = 'OTAVIO')",
                        List.of()),
                Arguments.of("evaluable function result as a plain value", eq(fn("UPPER", attr("name")), constant("OTAVIO")),
                        "(upper(\"name\") = 'OTAVIO')",
                        List.of()),
                Arguments.of("IN with a native function", CriteriaCondition.of(fn("UPPER", attr("name")), Condition.IN, constant(List.of("A", "B"))),
                        "(upper(\"name\") IN ('A', 'B'))",
                        List.of()),
                Arguments.of("BETWEEN with a native function", CriteriaCondition.of(fn("ABS", attr("value")), Condition.BETWEEN, constant(List.of(1, 5))),
                        "(abs(\"value\") BETWEEN 1 AND 5)",
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
                Arguments.of("LEFT", eq(fn("LEFT", attr("name"), value(3)), constant("Ota"))),
                Arguments.of("RIGHT", eq(fn("RIGHT", attr("name"), value(3)), constant("vio"))),
                Arguments.of("nested LEFT", eq(fn("UPPER", nested("LEFT", attr("name"), value(3))), constant("OTA"))),
                Arguments.of("LENGTH of a nested LEFT", eq(fn("LENGTH", nested("RIGHT", attr("name"), value(2))), constant(2))));
    }

    @Nested
    @DisplayName("Given supported native functions")
    class Supported {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.iotdb.communication.IoTDBNativeFunctionTest#supported")
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
                soft.assertThat(rendered.statement()).isEqualTo("SELECT COUNT(*) FROM \"sensor_reading\" WHERE (upper(\"name\") = 'OTAVIO')");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of());
            });
        }

        @Test
        @DisplayName("A native predicate keeps the temporal restriction next to it")
        void shouldKeepTemporalRestriction() {
            var ts = Instant.parse("2026-09-08T17:00:00Z");
            var rendered = select(CriteriaCondition.and(CriteriaCondition.eq(Element.of("_id", ts)),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO"))));

            assertSoftly(soft -> {
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE (\"time\" = 1788886800000 AND (upper(\"name\") = 'OTAVIO'))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of());
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
                soft.assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE ((\"value\" > 20.5 OR (upper(\"name\") = 'OTAVIO')) AND NOT ((lower(\"other\") = 'x')))");
                soft.assertThat(rendered.parameters()).containsExactlyElementsOf(List.of());
            });
        }

        @Test
        @DisplayName("NOT wraps a native predicate")
        void shouldNegate() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("OTAVIO")).negate());

            assertThat(rendered.statement()).isEqualTo("SELECT * FROM \"" + TABLE + "\" WHERE NOT ((upper(\"name\") = 'OTAVIO'))");
        }

        @Test
        @DisplayName("Ordering and limits are appended after the native predicate")
        void shouldKeepOrderAndLimit() {
            var query = new DefaultSelectQuery(5, 2, TABLE, List.of(), List.of(Sort.desc("_id")),
                    eq(fn("UPPER", attr("name")), constant("OTAVIO")), false);

            assertThat(IoTDBQueryConverter.convert(query)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE (upper(\"name\") = 'OTAVIO') ORDER BY \"time\" DESC LIMIT 5 OFFSET 2");
        }

        @Test
        @DisplayName("A hostile value is quoted as a literal and never ends the statement")
        void shouldQuoteHostileValue() {
            var rendered = select(eq(fn("UPPER", attr("name")), constant("x') OR 1=1 --")));

            assertThat(rendered.statement()).isEqualTo(
                    "SELECT * FROM \"sensor_reading\" WHERE (upper(\"name\") = 'x'') OR 1=1 --')");
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
        @MethodSource("org.eclipse.jnosql.databases.iotdb.communication.IoTDBNativeFunctionTest#unsupported")
        void shouldRejectWithUnsupportedOperation(String name, CriteriaCondition condition) {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> select(condition)).isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> count(condition)).isInstanceOf(UnsupportedOperationException.class);
            });
        }
    }
}
