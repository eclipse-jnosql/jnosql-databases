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
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.function.ArgumentType;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.function.ResultType;
import org.eclipse.jnosql.communication.function.StandardFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.eclipse.jnosql.databases.oracle.communication.TableCreationConfiguration.JSON_FIELD;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into an Oracle NoSQL SQL boolean
 * expression.
 * <p>
 * Constants are always bound as positional variables, attribute paths are validated identifiers and the function
 * structure is kept as nested SQL expressions. The semantics follow the shared function contract with these
 * exceptions, which come from Oracle NoSQL itself:
 * <ul>
 *     <li>a stored attribute that is not a string makes the string functions return null instead of failing,
 *     and a stored attribute that is not a number makes {@code ABS} return null;</li>
 *     <li>{@code ABS} does not report an overflow ({@code ABS(Long.MIN_VALUE)} is not rejected);</li>
 *     <li>the {@code _id} attribute is stored with the entity name prefix and is not supported as an operand.</li>
 * </ul>
 * {@code LENGTH}, {@code LEFT} and {@code RIGHT} count Unicode code points and {@code LOWER} and {@code UPPER}
 * use the full Unicode case mapping, as the shared contract requires.
 */
final class OracleNativeFunction {

    private static final String DATABASE = "Oracle NoSQL";

    private final String table;

    private OracleNativeFunction(String table) {
        this.table = table;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @param table     the table that qualifies the attributes
     * @param params    the positional variables that receive the constants
     * @return the SQL boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, String table, List<FieldValue> params) {
        var predicate = new OracleNativeFunction(table).predicate(condition);
        params.addAll(predicate.params());
        return predicate.text();
    }

    /**
     * A SQL fragment with the variables it binds, in order of appearance.
     * A fragment that is repeated in an expression repeats its variables as well.
     */
    private record Sql(String text, boolean string, List<FieldValue> params) {

        static Sql of(String text, boolean string) {
            return new Sql(text, string, List.of());
        }

        static Sql compose(boolean string, Object... parts) {
            var text = new StringBuilder();
            var params = new ArrayList<FieldValue>();
            for (Object part : parts) {
                if (part instanceof Sql sql) {
                    text.append(sql.text());
                    params.addAll(sql.params());
                } else if (part instanceof FieldValue value) {
                    params.add(value);
                } else {
                    text.append(part);
                }
            }
            return new Sql(text.toString(), string, params);
        }
    }

    private Sql predicate(CriteriaCondition condition) {
        var left = condition.leftOperand();
        var right = condition.rightOperand();
        Condition type = condition.condition();
        return Sql.compose(false, "(", switch (type) {
            case EQUALS -> equality(left, right);
            case GREATER_THAN -> comparison(" > ", left, right);
            case GREATER_EQUALS_THAN -> comparison(" >= ", left, right);
            case LESSER_THAN -> comparison(" < ", left, right);
            case LESSER_EQUALS_THAN -> comparison(" <= ", left, right);
            case LIKE -> like(left, right);
            case IN -> in(left, right);
            case BETWEEN -> between(left, right);
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + type + " condition with a native function operand");
        }, ")");
    }

    private Sql equality(CriteriaOperand left, CriteriaOperand right) {
        if (isNull(right)) {
            return nullEquality(expression(left));
        }
        if (isNull(left)) {
            return nullEquality(expression(right));
        }
        return comparison(" = ", left, right);
    }

    private static Sql nullEquality(Sql expression) {
        return Sql.compose(false, "(", expression, " = null OR ", expression, " IS NULL OR NOT EXISTS ",
                expression, ")");
    }

    private Sql comparison(String operator, CriteriaOperand left, CriteriaOperand right) {
        return Sql.compose(false, expression(left), operator, expression(right));
    }

    private Sql like(CriteriaOperand left, CriteriaOperand right) {
        if (left instanceof CriteriaOperand.Function function
                && StandardFunction.find(function.function().name()).map(StandardFunction::result)
                .filter(result -> result != ResultType.STRING).isPresent()) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support LIKE on the "
                    + "numeric result of native " + function.function().name());
        }
        var leftExpression = expression(left);
        var pattern = OracleNoSqlLikeConverter.INSTANCE.convert(constant(right, "LIKE").value().get());
        return Sql.compose(false, "regex_like(", leftExpression, ", ", parameter(pattern), ")");
    }

    private Sql in(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "IN").value());
        return Sql.compose(false, leftExpression, " IN ?[]", FieldValueConverter.of(values));
    }

    private Sql between(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "BETWEEN").value());
        return Sql.compose(false, leftExpression, " BETWEEN ", parameter(values.get(0)), " AND ",
                parameter(values.get(1)));
    }

    private CriteriaOperand.Constant constant(CriteriaOperand operand, String predicate) {
        if (operand instanceof CriteriaOperand.Constant constant) {
            return constant;
        }
        throw new UnsupportedOperationException("The " + DATABASE + " driver does not support " + predicate
                + " with a native function as the right operand");
    }

    private static boolean isNull(CriteriaOperand operand) {
        return operand instanceof CriteriaOperand.Constant constant && constant.value().isNull();
    }

    private Sql expression(CriteriaOperand operand) {
        return switch (operand) {
            case CriteriaOperand.Attribute attribute -> Sql.of(attribute(attribute.name()), false);
            case CriteriaOperand.Constant constant -> parameter(ValueUtil.convert(constant.value()));
            case CriteriaOperand.Function function -> function(function.function());
        };
    }

    private Sql function(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var arguments = function.arguments();
        var types = standard.parameters();
        var rendered = new Sql[arguments.size()];
        boolean nullResult = false;
        for (int index = 0; index < rendered.length; index++) {
            var argument = arguments.get(index);
            if (isNullConstant(argument)) {
                nullResult = true;
            }
            rendered[index] = argument(standard, index, types.get(index), argument);
        }
        if (nullResult) {
            return Sql.of("null", true);
        }
        var text = rendered[0];
        return switch (standard) {
            case ABS -> Sql.compose(false, "abs(", text, ")");
            case LOWER -> guarded(text, "lower(", text, ")");
            case UPPER -> guarded(text, "upper(", text, ")");
            case LENGTH -> guarded(text, "length(", text, ")");
            case LEFT -> guarded(text, left(text, rendered[1]));
            case RIGHT -> guarded(text, right(text, rendered[1]));
        };
    }

    // substring returns null for an empty string or a start at the end, so those cases are explicit.
    // Oracle NoSQL function names are case sensitive and must be lowercase.
    private static Sql left(Sql text, Sql length) {
        return Sql.compose(true, "CASE WHEN length(", text, ") = 0 OR ", length, " = 0 THEN '' ELSE substring(",
                text, ", 0, ", length, ") END");
    }

    private static Sql right(Sql text, Sql length) {
        return Sql.compose(true, "CASE WHEN length(", text, ") = 0 OR ", length, " = 0 THEN '' WHEN ", length,
                " >= length(", text, ") THEN ", text, " ELSE substring(", text, ", length(", text, ") - ", length,
                ") END");
    }

    // Oracle NoSQL coerces numbers to strings, so a stored attribute must be checked before a string function
    private static Sql guarded(Sql argument, Object... expression) {
        var body = Sql.compose(true, expression);
        if (argument.string()) {
            return body;
        }
        return Sql.compose(true, "CASE WHEN ", argument, " IS OF TYPE (ONLY STRING) THEN ", body, " ELSE null END");
    }

    private static boolean isNullConstant(FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.Literal literal -> literal.value() == null;
            case FunctionArgument.ValueArgument value -> value.value().isNull();
            case FunctionArgument.EvaluableArgument nested -> nested.function().get() == null;
            default -> false;
        };
    }

    private Sql argument(StandardFunction function, int index, ArgumentType type, FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.AttributePath path -> {
                if (type == ArgumentType.INTEGER) {
                    throw unsupportedLength(function);
                }
                yield Sql.of(attribute(path.path()), false);
            }
            case FunctionArgument.NativeArgument nested -> {
                if (type == ArgumentType.INTEGER
                        && !"LENGTH".equals(nested.function().name().toUpperCase(Locale.ROOT))) {
                    throw unsupportedLength(function);
                }
                yield function(nested.function());
            }
            case FunctionArgument.Literal literal -> constant(function, index, type, literal.value());
            case FunctionArgument.ValueArgument value -> constant(function, index, type, value.value().get());
            case FunctionArgument.EvaluableArgument nested -> constant(function, index, type,
                    nested.function().get());
            case FunctionArgument.NamedParameter parameter -> throw new IllegalStateException(
                    "Unbound parameter :" + parameter.name() + " in " + function.name());
            case FunctionArgument.PositionalParameter parameter -> throw new IllegalStateException(
                    "Unbound parameter ?" + parameter.position() + " in " + function.name());
        };
    }

    private static UnsupportedOperationException unsupportedLength(StandardFunction function) {
        return new UnsupportedOperationException("The " + DATABASE + " driver does not support native "
                + function.name() + " with a length that depends on a stored attribute: only a value or LENGTH is "
                + "accepted because a negative length cannot be rejected by the database");
    }

    private Sql constant(StandardFunction function, int index, ArgumentType type, Object value) {
        if (value != null && !type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        if (type == ArgumentType.INTEGER && value != null) {
            return Sql.of(Integer.toString(length(function, value)), true);
        }
        return parameter(value);
    }

    private static int length(StandardFunction function, Object value) {
        if (value instanceof BigInteger big) {
            if (big.signum() < 0) {
                throw negative(function, value);
            }
            return big.bitLength() > 31 ? Integer.MAX_VALUE : big.intValue();
        }
        long number = ((Number) value).longValue();
        if (number < 0) {
            throw negative(function, value);
        }
        return number > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) number;
    }

    private static IllegalArgumentException negative(StandardFunction function, Object value) {
        return new IllegalArgumentException("Invalid argument value for " + function.name()
                + " at position 2: the length must not be negative but was " + value);
    }

    private String attribute(String name) {
        if (DefaultOracleNoSQLDocumentManager.ID.equals(name)) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + name + " attribute in a native function predicate");
        }
        return table + "." + JSON_FIELD + "." + AbstractQueryBuilder.validateIdentifierPath(name);
    }

    private static Sql parameter(Object value) {
        return new Sql("?", true, List.of(FieldValueConverter.of(value)));
    }
}
