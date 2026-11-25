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
import java.util.Map;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into an AQL boolean expression.
 * <p>
 * Constants are always bound as AQL bind variables, attributes are validated identifiers and the function
 * structure is kept as nested AQL function calls. AQL casts {@code null} and non-matching values silently
 * ({@code UPPER(null)} is {@code ""} and {@code ABS(null)} is {@code 0}), so every function applied to a stored
 * attribute or to another function result is guarded with {@code IS_STRING} or {@code IS_NUMBER} and yields
 * {@code null} otherwise, which matches the shared null propagation contract.
 * <p>
 * Unicode character counting of {@code CHAR_LENGTH}, {@code LEFT} and {@code RIGHT}, the Unicode case mapping of
 * {@code LOWER} and {@code UPPER}, the behaviour of {@code LEFT} and {@code RIGHT} with a length greater than the
 * string and the lack of an overflow error in {@code ABS} are provided by ArangoDB and require integration
 * verification.
 */
final class ArangoNativeFunction {

    private static final String DATABASE = "ArangoDB";
    private static final String PARAMETER = "nf";

    private final char entity;
    private final Map<String, Object> params;

    private ArangoNativeFunction(char entity, Map<String, Object> params) {
        this.entity = entity;
        this.params = params;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @param entity    the alias of the iterated document
     * @param params    the bind variables that receive the constants
     * @return the AQL boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, char entity, Map<String, Object> params) {
        return new ArangoNativeFunction(entity, params).predicate(condition);
    }

    /**
     * An AQL expression. A constant is a bind variable that is neither null nor of an unexpected type,
     * every other expression may be null or of another type and must be guarded.
     */
    private record Expression(String text, boolean constant) {
    }

    private String predicate(CriteriaCondition condition) {
        var left = condition.leftOperand();
        var right = condition.rightOperand();
        Condition type = condition.condition();
        return "(" + switch (type) {
            case EQUALS -> comparison(" == ", left, right);
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

    private String comparison(String operator, CriteriaOperand left, CriteriaOperand right) {
        return expression(left).text() + operator + expression(right).text();
    }

    private String like(CriteriaOperand left, CriteriaOperand right) {
        if (left instanceof CriteriaOperand.Function function
                && StandardFunction.find(function.function().name()).map(StandardFunction::result)
                .filter(result -> result != ResultType.STRING).isPresent()) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support LIKE on the "
                    + "numeric result of native " + function.function().name());
        }
        var leftExpression = expression(left);
        return leftExpression.text() + " LIKE " + bind(ValueUtil.convert(constant(right, "LIKE").value(),
                ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER));
    }

    private String in(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        return leftExpression.text() + " IN " + bind(ValueUtil.convertToList(constant(right, "IN").value(),
                ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER));
    }

    private String between(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left).text();
        List<Object> values = ValueUtil.convertToList(constant(right, "BETWEEN").value(),
                ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER);
        return leftExpression + " >= " + bind(values.get(0)) + " AND " + leftExpression + " <= "
                + bind(values.get(1));
    }

    private static CriteriaOperand.Constant constant(CriteriaOperand operand, String predicate) {
        if (operand instanceof CriteriaOperand.Constant constant) {
            return constant;
        }
        throw new UnsupportedOperationException("The " + DATABASE + " driver does not support " + predicate
                + " with a native function as the right operand");
    }

    private Expression expression(CriteriaOperand operand) {
        return switch (operand) {
            case CriteriaOperand.Attribute attribute -> new Expression(attribute(attribute.name()), false);
            case CriteriaOperand.Constant constant -> new Expression(bind(ValueUtil.convert(constant.value(),
                    ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER)), true);
            case CriteriaOperand.Function function -> function(function.function());
        };
    }

    private Expression function(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var arguments = function.arguments();
        var types = standard.parameters();
        var rendered = new Expression[arguments.size()];
        boolean nullResult = false;
        for (int index = 0; index < rendered.length; index++) {
            var argument = arguments.get(index);
            nullResult |= isNullConstant(argument);
            rendered[index] = nullResult ? null : argument(standard, index, types.get(index), argument);
        }
        if (nullResult) {
            return new Expression("null", false);
        }
        var text = rendered[0];
        return switch (standard) {
            case ABS -> guarded("IS_NUMBER", text, "ABS(" + text.text() + ")");
            case LENGTH -> guarded("IS_STRING", text, "CHAR_LENGTH(" + text.text() + ")");
            case LOWER -> guarded("IS_STRING", text, "LOWER(" + text.text() + ")");
            case UPPER -> guarded("IS_STRING", text, "UPPER(" + text.text() + ")");
            case LEFT -> guarded("IS_STRING", text, rendered[1], "LEFT(" + text.text() + ", " + rendered[1].text() + ")");
            case RIGHT -> guarded("IS_STRING", text, rendered[1], "RIGHT(" + text.text() + ", " + rendered[1].text() + ")");
        };
    }

    private static Expression guarded(String check, Expression argument, String call) {
        return guarded(check, argument, null, call);
    }

    private static Expression guarded(String check, Expression argument, Expression length, String call) {
        var conditions = new java.util.ArrayList<String>();
        if (!argument.constant()) {
            conditions.add(check + "(" + argument.text() + ")");
        }
        if (length != null && !length.constant()) {
            conditions.add(length.text() + " != null");
        }
        if (conditions.isEmpty()) {
            return new Expression(call, false);
        }
        return new Expression("(" + String.join(" AND ", conditions) + " ? " + call + " : null)", false);
    }

    private static boolean isNullConstant(FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.Literal literal -> literal.value() == null;
            case FunctionArgument.ValueArgument value -> value.value().isNull();
            case FunctionArgument.EvaluableArgument nested -> nested.function().get() == null;
            default -> false;
        };
    }

    private Expression argument(StandardFunction function, int index, ArgumentType type, FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.AttributePath path -> {
                if (type == ArgumentType.INTEGER) {
                    throw unsupportedLength(function);
                }
                yield new Expression(attribute(path.path()), false);
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

    private Expression constant(StandardFunction function, int index, ArgumentType type, Object value) {
        if (!type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        if (type == ArgumentType.INTEGER) {
            return new Expression(bind(length(function, value)), true);
        }
        return new Expression(bind(value), true);
    }

    private static Object length(StandardFunction function, Object value) {
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
        QueryAQLConverter.identifier(name);
        return entity + "." + name;
    }

    private String bind(Object value) {
        var name = PARAMETER;
        int counter = 1;
        while (params.containsKey(name)) {
            name = PARAMETER + '_' + counter++;
        }
        params.put(name, value);
        return "@" + name;
    }
}
