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
package org.eclipse.jnosql.databases.neo4j.communication;

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.function.ArgumentType;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.function.ResultType;
import org.eclipse.jnosql.communication.function.StandardFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

import java.util.List;
import java.util.function.Function;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into a Cypher boolean expression.
 * <p>
 * Constants are always bound as Cypher parameters and attributes go through the builder's validated property
 * references. Supported functions: {@code ABS} as {@code abs}, {@code LOWER} as {@code toLower}, {@code UPPER} as
 * {@code toUpper} and {@code LENGTH} as {@code char_length}, which counts the characters of a string. Cypher
 * {@code length()} is a path length and {@code size()} is a collection size, so neither is used.
 * {@code LEFT} and {@code RIGHT} are rejected because the unit the Cypher {@code left()} and {@code right()}
 * functions count (UTF-16 units or code points) is not documented, so the contract cannot be guaranteed.
 * A null function argument is rejected because Cypher null propagation differs from the contract's null checks.
 */
final class Neo4JNativeFunction {

    private static final String DATABASE = "Neo4j";

    private final Function<String, String> property;
    private final Function<Object, String> binder;

    private Neo4JNativeFunction(Function<String, String> property, Function<Object, String> binder) {
        this.property = property;
        this.binder = binder;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @param property  translates an attribute name to a Cypher property reference
     * @param binder    registers a value and returns its parameter reference, for example {@code $name}
     * @return the Cypher boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, Function<String, String> property,
                          Function<Object, String> binder) {
        return new Neo4JNativeFunction(property, binder).predicate(condition);
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
            return expression(left) + " IS NULL";
        }
        if (isNull(left)) {
            return expression(right) + " IS NULL";
        }
        return comparison(" = ", left, right);
    }

    private String comparison(String operator, CriteriaOperand left, CriteriaOperand right) {
        return expression(left) + operator + expression(right);
    }

    private String like(CriteriaOperand left, CriteriaOperand right) {
        if (left instanceof CriteriaOperand.Function function
                && StandardFunction.find(function.function().name()).map(StandardFunction::result)
                .filter(result -> result != ResultType.STRING).isPresent()) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support LIKE on the "
                    + "numeric result of native " + function.function().name());
        }
        var pattern = LikeToCypherRegex.INSTANCE.toCypherRegex(constant(right, "LIKE").value().get().toString());
        return expression(left) + " =~ " + binder.apply(pattern);
    }

    private String in(CriteriaOperand left, CriteriaOperand right) {
        var expression = expression(left);
        return expression + " IN " + binder.apply(ValueUtil.convertToList(constant(right, "IN").value()));
    }

    private String between(CriteriaOperand left, CriteriaOperand right) {
        var expression = expression(left);
        List<Object> values = ValueUtil.convertToList(constant(right, "BETWEEN").value());
        return expression + " >= " + binder.apply(values.get(0)) + " AND " + expression + " <= "
                + binder.apply(values.get(1));
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
            return property.apply(attribute.name());
        }
        if (operand instanceof CriteriaOperand.Constant constant) {
            return binder.apply(ValueUtil.convert(constant.value()));
        }
        if (operand instanceof CriteriaOperand.Function function) {
            return function(function.function());
        }
        throw new IllegalStateException("Unexpected " + operand);
    }

    private String function(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var name = switch (standard) {
            case ABS -> "abs";
            case LOWER -> "toLower";
            case UPPER -> "toUpper";
            case LENGTH -> "char_length";
            case LEFT, RIGHT -> throw new UnsupportedOperationException("The " + DATABASE
                    + " driver does not support native " + standard.name() + ": the Cypher "
                    + standard.name().toLowerCase(java.util.Locale.ROOT)
                    + "() function does not document code point semantics");
        };
        var arguments = function.arguments();
        var types = standard.parameters();
        return name + "(" + argument(standard, 0, types.get(0), arguments.get(0)) + ")";
    }

    private String argument(StandardFunction function, int index, ArgumentType type, FunctionArgument argument) {
        if (argument instanceof FunctionArgument.AttributePath path) {
            return property.apply(path.path());
        }
        if (argument instanceof FunctionArgument.NativeArgument nested) {
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

    private String constant(StandardFunction function, int index, ArgumentType type, Object value) {
        if (value == null) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support a null argument "
                    + "in native " + function.name());
        }
        if (!type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        return binder.apply(value);
    }
}
