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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.List;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into a MongoDB {@code $expr} filter.
 * <p>
 * Every function becomes a structured aggregation expression. Constants are always wrapped in {@code $literal}, so a
 * value such as {@code "$name"} is never read as a field path, and attributes are validated before being turned into
 * a field path. The semantics follow the shared function contract:
 * <ul>
 *     <li>{@code LENGTH}, {@code LEFT} and {@code RIGHT} count Unicode code points ({@code $strLenCP},
 *     {@code $substrCP});</li>
 *     <li>a null or missing attribute makes the function result null (MongoDB would otherwise return an empty
 *     string or fail);</li>
 *     <li>a stored attribute used as a string argument must be a string, otherwise MongoDB fails the query instead of
 *     coercing the value;</li>
 *     <li>a null result never satisfies an ordering comparison, and two expressions are only equal when neither is
 *     null; comparing with a null constant tests for null.</li>
 * </ul>
 * Known limitation: {@code $toLower} and {@code $toUpper} only have a defined behavior for ASCII characters, so
 * {@code LOWER} and {@code UPPER} do not convert non-ASCII letters the way {@code String.toLowerCase(Locale.ROOT)}
 * does.
 */
final class NativeFunctionFilter {

    private static final String DATABASE = "MongoDB";

    private NativeFunctionFilter() {
    }

    static Bson convert(CriteriaCondition condition) {
        var left = condition.leftOperand();
        var right = condition.rightOperand();
        Condition type = condition.condition();
        return Filters.expr(switch (type) {
            case EQUALS -> equality(left, right);
            case GREATER_THAN -> ordering("$gt", left, right);
            case GREATER_EQUALS_THAN -> ordering("$gte", left, right);
            case LESSER_THAN -> ordering("$lt", left, right);
            case LESSER_EQUALS_THAN -> ordering("$lte", left, right);
            case LIKE -> like(left, right);
            case IN -> in(left, right);
            case BETWEEN -> between(left, right);
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + type + " condition with a native function operand");
        });
    }

    private static Document equality(CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        if (right instanceof CriteriaOperand.Constant constant && constant.value().isNull()) {
            return new Document("$eq", List.of(leftExpression, literal(null)));
        }
        var rightExpression = expression(right);
        return notNull(new Document("$eq", List.of(leftExpression, rightExpression)), leftExpression, rightExpression);
    }

    private static Document ordering(String operator, CriteriaOperand left, CriteriaOperand right) {
        var leftExpression = expression(left);
        var rightExpression = expression(right);
        return notNull(new Document(operator, List.of(leftExpression, rightExpression)), leftExpression,
                rightExpression);
    }

    private static Document like(CriteriaOperand left, CriteriaOperand right) {
        requireString(left, "LIKE");
        var constant = constant(right, "LIKE");
        var pattern = constant.value().isNull() ? null : constant.value().get().toString();
        var regex = new Document("input", expression(left))
                .append("regex", DocumentQueryConversor.prepareRegexValue(pattern));
        return new Document("$regexMatch", regex);
    }

    private static Document in(CriteriaOperand left, CriteriaOperand right) {
        var constant = constant(right, "IN");
        List<Object> values = ValueUtil.convertToList(constant.value(),
                MongoDBValueWriteDecorator.MONGO_DB_VALUE_WRITER);
        return new Document("$in", List.of(expression(left), literal(values)));
    }

    private static Document between(CriteriaOperand left, CriteriaOperand right) {
        var constant = constant(right, "BETWEEN");
        List<Object> values = ValueUtil.convertToList(constant.value(),
                MongoDBValueWriteDecorator.MONGO_DB_VALUE_WRITER);
        var expression = expression(left);
        var lower = literal(values.get(0));
        var upper = literal(values.get(1));
        return new Document("$and", List.of(
                new Document("$ne", Arrays.asList(expression, null)),
                new Document("$gte", List.of(expression, lower)),
                new Document("$lte", List.of(expression, upper))));
    }

    private static CriteriaOperand.Constant constant(CriteriaOperand operand, String predicate) {
        if (operand instanceof CriteriaOperand.Constant constant) {
            return constant;
        }
        throw new UnsupportedOperationException("The " + DATABASE + " driver does not support " + predicate
                + " with a native function as the right operand");
    }

    private static Document notNull(Document comparison, Object left, Object right) {
        return new Document("$and", List.of(
                new Document("$ne", Arrays.asList(left, null)),
                new Document("$ne", Arrays.asList(right, null)),
                comparison));
    }

    private static void requireString(CriteriaOperand operand, String predicate) {
        if (operand instanceof CriteriaOperand.Function function
                && StandardFunction.find(function.function().name()).map(StandardFunction::result)
                .filter(result -> result != ResultType.STRING).isPresent()) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support " + predicate
                    + " on the numeric result of native " + function.function().name());
        }
    }

    private static Object expression(CriteriaOperand operand) {
        return switch (operand) {
            case CriteriaOperand.Attribute attribute -> attribute(attribute.name());
            case CriteriaOperand.Constant constant -> literal(ValueUtil.convert(constant.value(),
                    MongoDBValueWriteDecorator.MONGO_DB_VALUE_WRITER));
            case CriteriaOperand.Function function -> function(function.function(), 0);
        };
    }

    private static Object function(NativeFunction function, int depth) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        var arguments = function.arguments();
        var types = standard.parameters();
        var variables = new Document();
        var names = new ArrayList<String>();
        for (int index = 0; index < arguments.size(); index++) {
            var name = "f" + depth + "a" + index;
            names.add("$$" + name);
            variables.append(name, argument(standard, index, types.get(index), arguments.get(index), depth));
        }
        var nullCheck = names.stream().<Object>map(name -> new Document("$eq", Arrays.asList(name, null))).toList();
        var body = new Document("$cond", Arrays.asList(new Document("$or", nullCheck), null, apply(standard, names)));
        return new Document("$let", new Document("vars", variables).append("in", body));
    }

    private static Object apply(StandardFunction function, List<String> arguments) {
        var text = arguments.get(0);
        return switch (function) {
            case ABS -> new Document("$abs", text);
            case LENGTH -> new Document("$strLenCP", text);
            case LOWER -> new Document("$toLower", text);
            case UPPER -> new Document("$toUpper", text);
            case LEFT -> new Document("$substrCP", List.of(text, 0, arguments.get(1)));
            case RIGHT -> {
                var start = new Document("$max", List.of(0,
                        new Document("$subtract", List.of(new Document("$strLenCP", text), arguments.get(1)))));
                yield new Document("$substrCP", List.of(text, start, arguments.get(1)));
            }
        };
    }

    private static Object argument(StandardFunction function, int index, ArgumentType type,
                                   FunctionArgument argument, int depth) {
        return switch (argument) {
            case FunctionArgument.AttributePath path -> {
                if (type == ArgumentType.INTEGER) {
                    throw unsupportedLength(function);
                }
                yield type == ArgumentType.STRING ? stringAttribute(path.path()) : attribute(path.path());
            }
            case FunctionArgument.NativeArgument nested -> {
                if (type == ArgumentType.INTEGER && !"LENGTH".equals(nested.function().name().toUpperCase(
                        Locale.ROOT))) {
                    throw unsupportedLength(function);
                }
                yield function(nested.function(), depth + 1);
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

    private static Object constant(StandardFunction function, int index, ArgumentType type, Object value) {
        if (value != null && !type.accepts(value)) {
            throw new IllegalArgumentException("Invalid argument type for " + function.name() + " at position "
                    + (index + 1) + ": expected " + type + " but found " + value.getClass().getName());
        }
        if (type == ArgumentType.INTEGER && value != null) {
            return literal(length(function, value));
        }
        return literal(value);
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

    private static Object attribute(String name) {
        var path = DocumentQueryConversor.field(name);
        for (String part : path.split("\\.")) {
            if (part.charAt(0) == '$') {
                throw new IllegalArgumentException("Invalid MongoDB field name: " + name);
            }
        }
        return new Document("$ifNull", Arrays.asList("$" + path, null));
    }

    // $strLenCP only accepts strings: it makes the query fail instead of coercing a number or a date
    private static Object stringAttribute(String name) {
        var attribute = attribute(name);
        var isString = new Document("$eq", List.of(new Document("$type", attribute), "string"));
        return new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList(attribute, null)), null,
                new Document("$cond", List.of(isString, attribute, new Document("$strLenCP", attribute)))));
    }

    private static Document literal(Object value) {
        return new Document("$literal", value);
    }
}
