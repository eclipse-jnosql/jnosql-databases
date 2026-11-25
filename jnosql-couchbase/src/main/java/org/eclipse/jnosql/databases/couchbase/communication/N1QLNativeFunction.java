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

import com.couchbase.client.java.json.JsonObject;
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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into a SQL++ boolean expression.
 * <p>
 * Constants are always bound as named parameters, attribute paths are quoted identifiers and the function structure
 * is kept as nested SQL++ expressions. The semantics follow the shared function contract with these exceptions,
 * which come from SQL++ itself:
 * <ul>
 *     <li>{@code LENGTH}, {@code LEFT} and {@code RIGHT} count Unicode code points. SQL++ {@code LENGTH} and
 *     {@code SUBSTR} work on UTF-8 bytes, so the code points are obtained with {@code SPLIT(value, '')};</li>
 *     <li>a stored attribute that is not a string makes the string functions return null instead of failing;</li>
 *     <li>{@code ABS} does not report an overflow ({@code ABS(Long.MIN_VALUE)} is not rejected);</li>
 *     <li>{@code LOWER} and {@code UPPER} use the SQL++ case mapping, which is simple (one character to one
 *     character): {@code UPPER('ß')} stays {@code 'ß'}.</li>
 * </ul>
 */
final class N1QLNativeFunction {

    private static final String DATABASE = "Couchbase";

    private final String alias;
    private final JsonObject params;

    private N1QLNativeFunction(String alias, JsonObject params) {
        this.alias = alias;
        this.params = params;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @param alias     the alias used to qualify attributes, or {@code null} when attributes are not qualified
     * @param params    the named parameters that receive the constants
     * @return the SQL++ boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, String alias, JsonObject params) {
        return new N1QLNativeFunction(alias, params).predicate(condition);
    }

    private String predicate(CriteriaCondition condition) {
        var left = condition.leftOperand();
        var right = condition.rightOperand();
        Condition type = condition.condition();
        return "(" + switch (type) {
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
        } + ")";
    }

    private String equality(CriteriaOperand left, CriteriaOperand right) {
        if (isNull(right)) {
            return expression(left) + " IS NOT VALUED";
        }
        if (isNull(left)) {
            return expression(right) + " IS NOT VALUED";
        }
        return comparison(" = ", left, right);
    }

    private String comparison(String operator, CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        return leftExpression + operator + expression(right);
    }

    private String like(CriteriaOperand left, CriteriaOperand right) {
        if (left instanceof CriteriaOperand.Function function
                && StandardFunction.find(function.function().name()).map(StandardFunction::result)
                .filter(result -> result != ResultType.STRING).isPresent()) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support LIKE on the "
                    + "numeric result of native " + function.function().name());
        }
        var leftExpression = expression(left);
        return leftExpression + " LIKE " + parameter(constant(right, "LIKE").value().get());
    }

    private String in(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "IN").value());
        return leftExpression + " IN " + parameter(values);
    }

    private String between(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "BETWEEN").value());
        var lower = parameter(values.get(0));
        var upper = parameter(values.get(1));
        return leftExpression + " BETWEEN " + lower + " AND " + upper;
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

    private String expression(CriteriaOperand operand) {
        if (operand instanceof CriteriaOperand.Attribute attribute) {
            return attribute(attribute.name());
        }
        if (operand instanceof CriteriaOperand.Constant constant) {
            return parameter(ValueUtil.convert(constant.value()));
        }
        if (operand instanceof CriteriaOperand.Function function) {
            return function(function.function(), 0);
        }
        throw new IllegalStateException("Unexpected " + operand);
    }

    private String function(NativeFunction function, int depth) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var arguments = function.arguments();
        var types = standard.parameters();
        var rendered = new String[arguments.size()];
        boolean nullResult = false;
        for (int index = 0; index < rendered.length; index++) {
            var argument = arguments.get(index);
            if (isNullConstant(argument)) {
                nullResult = true;
            }
            rendered[index] = argument(standard, index, types.get(index), argument, depth);
        }
        if (nullResult) {
            return "NULL";
        }
        var text = rendered[0];
        return switch (standard) {
            case ABS -> "ABS(" + text + ")";
            case LOWER -> "LOWER(" + text + ")";
            case UPPER -> "UPPER(" + text + ")";
            case LENGTH -> "CASE WHEN ISSTRING(" + text + ") THEN ARRAY_LENGTH(SPLIT(" + text + ", '')) END";
            case LEFT -> {
                var bytes = bytes("SPLIT(" + text + ", '')", rendered[1], depth);
                yield "CASE WHEN ISSTRING(" + text + ") THEN IFNULL(SUBSTR(" + text + ", 0, " + bytes + "), '') END";
            }
            case RIGHT -> {
                var bytes = bytes("ARRAY_REVERSE(SPLIT(" + text + ", ''))", rendered[1], depth);
                yield "CASE WHEN ISSTRING(" + text + ") THEN IFNULL(SUBSTR(" + text + ", LENGTH(" + text
                        + ") - " + bytes + "), '') END";
            }
        };
    }

    // SUBSTR works on UTF-8 bytes: the number of bytes used by the first code points is what is cut
    private static String bytes(String codePoints, String length, int depth) {
        var point = "c" + depth;
        var position = "i" + depth;
        return "IFNULL(ARRAY_SUM(ARRAY LENGTH(" + point + ") FOR " + position + ":" + point + " IN " + codePoints
                + " WHEN " + position + " < " + length + " END), 0)";
    }

    private static boolean isNullConstant(FunctionArgument argument) {
        if (argument instanceof FunctionArgument.Literal literal) {
            return literal.value() == null;
        }
        if (argument instanceof FunctionArgument.ValueArgument value) {
            return value.value().isNull();
        }
        if (argument instanceof FunctionArgument.EvaluableArgument nested) {
            return nested.function().get() == null;
        }
        return false;
    }

    private String argument(StandardFunction function, int index, ArgumentType type,
                            FunctionArgument argument, int depth) {
        if (argument instanceof FunctionArgument.AttributePath path) {
            if (type == ArgumentType.INTEGER) {
                throw unsupportedLength(function);
            }
            return attribute(path.path());
        }
        if (argument instanceof FunctionArgument.NativeArgument nested) {
            if (type == ArgumentType.INTEGER
                    && !"LENGTH".equals(nested.function().name().toUpperCase(Locale.ROOT))) {
                throw unsupportedLength(function);
            }
            return function(nested.function(), depth + 1);
        }
        if (argument instanceof FunctionArgument.Literal literal) {
            return constant(function, index, type, literal.value());
        }
        if (argument instanceof FunctionArgument.ValueArgument value) {
            return constant(function, index, type, value.value().get());
        }
        if (argument instanceof FunctionArgument.EvaluableArgument nested) {
            return constant(function, index, type,
                    nested.function().get());
        }
        if (argument instanceof FunctionArgument.NamedParameter parameter) {
            throw new IllegalStateException(
                    "Unbound parameter :" + parameter.name() + " in " + function.name());
        }
        if (argument instanceof FunctionArgument.PositionalParameter parameter) {
            throw new IllegalStateException(
                    "Unbound parameter ?" + parameter.position() + " in " + function.name());
        }
        throw new IllegalStateException("Unexpected " + argument);
    }

    private static UnsupportedOperationException unsupportedLength(StandardFunction function) {
        return new UnsupportedOperationException("The " + DATABASE + " driver does not support native "
                + function.name() + " with a length that depends on a stored attribute: only a value or LENGTH is "
                + "accepted because a negative length cannot be rejected by the database");
    }

    private String constant(StandardFunction function, int index, ArgumentType type, Object value) {
        if (value != null && !type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        if (type == ArgumentType.INTEGER && value != null) {
            return parameter(length(function, value));
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
        var path = Arrays.stream(name.split("\\.", -1))
                .map(part -> "`" + part.replace("`", "``") + "`")
                .collect(Collectors.joining("."));
        return alias == null ? path : alias + "." + path;
    }

    private String parameter(Object value) {
        var name = "$p" + params.getNames().size();
        params.put(name, value);
        return name;
    }
}
