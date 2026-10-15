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
package org.eclipse.jnosql.databases.milvus.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Translates a {@link CriteriaCondition} into a Milvus filter expression.
 * Values are never concatenated into the expression: they are sent as template values.
 * It is shared by select, count, delete and vector search.
 */
final class MilvusFilterConverter {

    private static final Pattern FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * A Milvus filter expression and its template values.
     */
    record Filter(String expression, Map<String, Object> parameters) {
    }

    private final Map<String, Object> parameters = new LinkedHashMap<>();

    private MilvusFilterConverter() {
    }

    static Optional<Filter> convert(Optional<CriteriaCondition> condition) {
        return condition.map(criteria -> {
            MilvusFilterConverter converter = new MilvusFilterConverter();
            String expression = converter.toExpression(criteria);
            return new Filter(expression, converter.parameters);
        });
    }

    private String toExpression(CriteriaCondition criteria) {
        return switch (criteria.condition()) {
            case AND -> join(criteria, " and ");
            case OR -> join(criteria, " or ");
            case NOT -> "not (" + toExpression(criteria.element().get(CriteriaCondition.class)) + ")";
            case EQUALS -> equalsTo(criteria);
            case GREATER_THAN -> compare(criteria, ">");
            case GREATER_EQUALS_THAN -> compare(criteria, ">=");
            case LESSER_THAN -> compare(criteria, "<");
            case LESSER_EQUALS_THAN -> compare(criteria, "<=");
            case BETWEEN -> between(criteria);
            case IN -> in(criteria);
            default -> throw new UnsupportedOperationException("Milvus does not support the condition "
                    + criteria.condition());
        };
    }

    private String join(CriteriaCondition criteria, String operator) {
        List<CriteriaCondition> conditions = criteria.element().value().get(new TypeReference<>() {
        });
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("The condition " + criteria.condition() + " requires children");
        }
        return conditions.stream().map(this::toExpression)
                .map(expression -> "(" + expression + ")")
                .collect(java.util.stream.Collectors.joining(operator));
    }

    private String equalsTo(CriteriaCondition criteria) {
        String field = field(criteria);
        Object value = MilvusValueWriteDecorator.convert(criteria.element().value());
        if (value == null) {
            return field + " is null";
        }
        return field + " == " + parameter(value);
    }

    private String compare(CriteriaCondition criteria, String operator) {
        String field = field(criteria);
        Object value = MilvusValueWriteDecorator.convert(criteria.element().value());
        if (!(value instanceof Number) && !(value instanceof CharSequence)) {
            throw new UnsupportedOperationException("Milvus range conditions require numeric or text values but got: "
                    + value);
        }
        return field + " " + operator + " " + parameter(value);
    }

    private String between(CriteriaCondition criteria) {
        String field = field(criteria);
        List<Object> values = MilvusValueWriteDecorator.convertToList(criteria.element().value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return field + " >= " + parameter(values.get(0)) + " and " + field + " <= " + parameter(values.get(1));
    }

    private String in(CriteriaCondition criteria) {
        String field = field(criteria);
        List<Object> values = MilvusValueWriteDecorator.convertToList(criteria.element().value());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        return field + " in " + parameter(values.stream().map(MilvusFilterConverter::normalize).toList());
    }

    private String parameter(Object value) {
        String name = "p" + parameters.size();
        parameters.put(name, value instanceof List<?> ? value : normalize(value));
        return "{" + name + "}";
    }

    private static String field(CriteriaCondition criteria) {
        String field = criteria.element().name();
        if (!FIELD.matcher(field).matches()) {
            throw new IllegalArgumentException("Invalid Milvus field name: " + field);
        }
        return field;
    }

    static Object normalize(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            return ((Number) value).longValue();
        }
        if (value instanceof Float) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        if (value instanceof UUID || value instanceof Character) {
            return value.toString();
        }
        if (value instanceof Long || value instanceof Double || value instanceof Boolean
                || value instanceof CharSequence) {
            return value instanceof CharSequence ? value.toString() : value;
        }
        throw new UnsupportedOperationException("Milvus cannot filter by a value of type "
                + (value == null ? "null" : value.getClass().getName()));
    }
}
