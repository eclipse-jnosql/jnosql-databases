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
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.function.ResultType;
import org.eclipse.jnosql.communication.function.StandardFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

import java.util.List;

import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.ID_FIELD;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.RID_FIELD;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into an OrientDB SQL boolean
 * expression.
 * <p>
 * Constants are always bound as positional parameters and attributes are validated identifiers. OrientDB
 * implements {@code ABS} as the {@code abs()} function and {@code LOWER} and {@code UPPER} as the
 * {@code toLowerCase()} and {@code toUpperCase()} methods of the attribute.
 * <p>
 * {@code LENGTH}, {@code LEFT} and {@code RIGHT} are rejected. Their OrientDB counterparts ({@code length()},
 * {@code substring()} and {@code right()}) work with UTF-16 code units instead of Unicode code points, so they
 * split surrogate pairs, and {@code length()} of a missing value is {@code 0} instead of null, which would make
 * {@code LENGTH(name) = 0} match a null attribute. {@code LEFT} is also not a SQL method of OrientDB.
 * <p>
 * The values below are provided by OrientDB and require integration verification: a stored value that is not
 * a string is converted with {@code toString()} by the case methods, a stored value that is not a number makes
 * {@code abs()} fail on the server, {@code abs()} does not report the overflow of the minimum integer, and
 * {@code toLowerCase()} uses the default locale of the server.
 */
final class OrientNativeFunction {

    private static final String DATABASE = "OrientDB";

    private final List<Object> params;

    private OrientNativeFunction(List<Object> params) {
        this.params = params;
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @param params    the positional parameters that receive the constants, in order of appearance
     * @return the SQL boolean expression, wrapped in parentheses
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    static String convert(CriteriaCondition condition, List<Object> params) {
        return new OrientNativeFunction(params).predicate(condition);
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
        return leftExpression + " LIKE " + parameter(ValueUtil.convert(constant(right, "LIKE").value()));
    }

    private String in(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        return leftExpression + " IN " + parameter(ValueUtil.convertToList(constant(right, "IN").value()));
    }

    private static CriteriaOperand.Constant constant(CriteriaOperand operand, String predicate) {
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
        return switch (operand) {
            case CriteriaOperand.Attribute attribute -> attribute(attribute.name());
            case CriteriaOperand.Constant constant -> parameter(ValueUtil.convert(constant.value()));
            case CriteriaOperand.Function function -> function(function.function());
        };
    }

    private String function(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        return switch (standard) {
            case ABS -> "abs(" + argument(standard, function.arguments().getFirst()) + ")";
            case LOWER -> argument(standard, function.arguments().getFirst()) + ".toLowerCase()";
            case UPPER -> argument(standard, function.arguments().getFirst()) + ".toUpperCase()";
            case LENGTH, LEFT, RIGHT -> throw new UnsupportedOperationException("The " + DATABASE
                    + " driver does not support the native function " + standard.name()
                    + ": OrientDB counts UTF-16 code units and returns 0 for a null length, which differs from "
                    + "the Unicode code point and null semantics of the function contract");
        };
    }

    private String argument(StandardFunction function, FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.AttributePath path -> attribute(path.path());
            case FunctionArgument.NativeArgument nested -> function(nested.function());
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver supports only an "
                    + "attribute or a native function as the argument of " + function.name());
        };
    }

    private static String attribute(String name) {
        if (ID_FIELD.equals(name) || RID_FIELD.equals(name)) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + name + " attribute in a native function predicate");
        }
        return QueryOSQLConverter.identifier(name);
    }

    private String parameter(Object value) {
        params.add(value);
        return "?";
    }
}
