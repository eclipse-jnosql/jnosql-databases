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

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.function.ArgumentType;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.function.StandardFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static org.eclipse.jnosql.databases.questdb.communication.QuestDBEntityConverter.ID_FIELD;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into a QuestDB SQL boolean expression.
 * <p>
 * Supported functions: {@code ABS} as {@code abs}, {@code LOWER} as {@code lower}, {@code UPPER} as {@code upper},
 * {@code LENGTH} as {@code nullif(length(x), -1)}, {@code LEFT} as {@code left} and {@code RIGHT} as {@code right}.
 * Values are bound as positional {@code $n} parameters; a length is bound as an int. Aggregate and window functions
 * are never produced. A length argument that is a stored attribute is rejected because a negative count cannot be
 * rejected by the database.
 * Attribute names go through the converter's identifier quoting and values are never concatenated into the
 * statement as text. The temporal identifier ({@code _id}) is not accepted as a function operand because it is not
 * a string or a number attribute. A null function argument or a null comparison constant is rejected.
 * The predicate is only a fragment: temporal restrictions, boolean grouping, ordering and limits stay in the
 * existing converter.
 */
final class QuestDBNativeFunction {

    private static final String DATABASE = "QuestDB";

    private final Function<String, String> identifier;
    private final Function<Object, String> binder;

    private QuestDBNativeFunction(Function<String, String> identifier, Function<Object, String> binder) {
        this.identifier = identifier;
        this.binder = binder;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition  the native condition
     * @param identifier quotes an attribute name as a column reference
     * @param binder     registers a value and returns the text that references it
     * @return the boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, Function<String, String> identifier,
                          Function<Object, String> binder) {
        return new QuestDBNativeFunction(identifier, binder).predicate(condition);
    }

    private String predicate(CriteriaCondition condition) {
        var left = condition.leftOperand();
        var right = condition.rightOperand();
        Condition type = condition.condition();
        return "(" + switch (type) {
            case EQUALS -> comparison(" = ", left, right);
            case GREATER_THAN -> comparison(" > ", left, right);
            case GREATER_EQUALS_THAN -> comparison(" >= ", left, right);
            case LESSER_THAN -> comparison(" < ", left, right);
            case LESSER_EQUALS_THAN -> comparison(" <= ", left, right);
            case IN -> in(left, right);
            case BETWEEN -> between(left, right);
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + type + " condition with a native function operand");
        } + ")";
    }

    private String comparison(String operator, CriteriaOperand left, CriteriaOperand right) {
        return expression(left) + operator + expression(right);
    }

    private String in(CriteriaOperand left, CriteriaOperand right) {
        var expression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "IN").value());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        return expression + " IN (" + String.join(", ", values.stream().map(this::bindComparison).toList()) + ")";
    }

    private String between(CriteriaOperand left, CriteriaOperand right) {
        var expression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "BETWEEN").value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return expression + " BETWEEN " + bindComparison(values.get(0)) + " AND " + bindComparison(values.get(1));
    }

    private CriteriaOperand.Constant constant(CriteriaOperand operand, String predicate) {
        if (operand instanceof CriteriaOperand.Constant constant) {
            return constant;
        }
        throw new UnsupportedOperationException("The " + DATABASE + " driver does not support " + predicate
                + " with a native function as the right operand");
    }

    private String bindComparison(Object value) {
        if (value == null) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support comparing a "
                    + "native function with null");
        }
        return binder.apply(value);
    }

    private String expression(CriteriaOperand operand) {
        if (operand instanceof CriteriaOperand.Attribute attribute) {
            return attribute(attribute.name());
        }
        if (operand instanceof CriteriaOperand.Constant constant) {
            return bindComparison(ValueUtil.convert(constant.value()));
        }
        if (operand instanceof CriteriaOperand.Function function) {
            return function(function.function());
        }
        throw new IllegalStateException("Unexpected " + operand);
    }

    private String attribute(String name) {
        if (ID_FIELD.equals(name)) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + name + " attribute in a native function predicate");
        }
        return identifier.apply(name);
    }

    private String function(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var arguments = function.arguments();
        var types = standard.parameters();
        var text = argument(standard, 0, types.get(0), arguments.get(0));
        return switch (standard) {
            case ABS -> "abs(" + text + ")";
            case LOWER -> "lower(" + text + ")";
            case UPPER -> "upper(" + text + ")";
            // QuestDB length() returns -1 for null, so it is converted back to null
            case LENGTH -> "nullif(length(" + text + "), -1)";
            case LEFT -> "left(" + text + ", " + argument(standard, 1, types.get(1), arguments.get(1)) + ")";
            case RIGHT -> "right(" + text + ", " + argument(standard, 1, types.get(1), arguments.get(1)) + ")";
        };
    }

    private String argument(StandardFunction function, int index, ArgumentType type, FunctionArgument argument) {
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
            return function(nested.function());
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
        if (value == null) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support a null argument "
                    + "in native " + function.name());
        }
        if (!type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        if (type == ArgumentType.INTEGER) {
            return binder.apply(length(function, value));
        }
        return binder.apply(value);
    }

    // a length beyond the numeric range of the function returns the whole string, so it is clamped
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
}
