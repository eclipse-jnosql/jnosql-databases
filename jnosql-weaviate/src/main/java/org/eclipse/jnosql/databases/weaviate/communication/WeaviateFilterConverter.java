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
package org.eclipse.jnosql.databases.weaviate.communication;

import io.weaviate.client6.v1.api.collections.DataType;
import io.weaviate.client6.v1.api.collections.query.Filter;
import io.weaviate.client6.v1.api.collections.query.FilterOperand;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;

import java.util.List;
import java.util.Optional;

/**
 * Translates a {@link CriteriaCondition} into a Weaviate {@link Filter}.
 * It is shared by select, count, delete and vector search. Conditions that Weaviate cannot evaluate with the
 * same semantics are rejected.
 */
final class WeaviateFilterConverter {

    private final WeaviateCollectionSchema schema;

    private WeaviateFilterConverter(WeaviateCollectionSchema schema) {
        this.schema = schema;
    }

    static Optional<Filter> convert(Optional<CriteriaCondition> condition, WeaviateCollectionSchema schema) {
        WeaviateFilterConverter converter = new WeaviateFilterConverter(schema);
        return condition.map(converter::toFilter);
    }

    private Filter toFilter(CriteriaCondition criteria) {
        return switch (criteria.condition()) {
            case AND -> Filter.and(children(criteria));
            case OR -> Filter.or(children(criteria));
            case NOT -> Filter.not(toFilter(criteria.element().get(CriteriaCondition.class)));
            case EQUALS -> equalsTo(criteria);
            case GREATER_THAN -> compare(criteria, Operator.GT);
            case GREATER_EQUALS_THAN -> compare(criteria, Operator.GTE);
            case LESSER_THAN -> compare(criteria, Operator.LT);
            case LESSER_EQUALS_THAN -> compare(criteria, Operator.LTE);
            case BETWEEN -> between(criteria);
            case IN -> in(criteria);
            default -> throw new UnsupportedOperationException("Weaviate does not support the condition "
                    + criteria.condition());
        };
    }

    private List<FilterOperand> children(CriteriaCondition criteria) {
        List<CriteriaCondition> conditions = criteria.element().value().get(new TypeReference<>() {
        });
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("The condition " + criteria.condition() + " requires children");
        }
        return conditions.stream().map(this::toFilter).map(FilterOperand.class::cast).toList();
    }

    private Filter equalsTo(CriteriaCondition criteria) {
        String property = property(criteria);
        Object value = WeaviateValueConverter.convert(criteria.element().value());
        if (value == null) {
            return Filter.property(property).isNull(true);
        }
        return apply(property, value, Operator.EQ);
    }

    private Filter compare(CriteriaCondition criteria, Operator operator) {
        String property = property(criteria);
        Object value = WeaviateValueConverter.convert(criteria.element().value());
        if (!(value instanceof Long) && !(value instanceof Double)) {
            throw new UnsupportedOperationException("Weaviate range conditions only support numeric values but got: "
                    + value);
        }
        return apply(property, value, operator);
    }

    private Filter between(CriteriaCondition criteria) {
        String property = property(criteria);
        List<Object> values = WeaviateValueConverter.convertToList(criteria.element().value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return Filter.and(
                (FilterOperand) compareValue(property, values.get(0), Operator.GTE),
                (FilterOperand) compareValue(property, values.get(1), Operator.LTE));
    }

    private Filter compareValue(String property, Object value, Operator operator) {
        if (!(value instanceof Long) && !(value instanceof Double)) {
            throw new UnsupportedOperationException("Weaviate range conditions only support numeric values but got: "
                    + value);
        }
        return apply(property, value, operator);
    }

    private Filter in(CriteriaCondition criteria) {
        String property = property(criteria);
        List<Object> values = WeaviateValueConverter.convertToList(criteria.element().value());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        List<FilterOperand> operands = values.stream()
                .map(value -> (FilterOperand) apply(property, value, Operator.EQ)).toList();
        return operands.size() == 1 ? (Filter) operands.get(0) : Filter.or(operands);
    }

    private String property(CriteriaCondition criteria) {
        String property = WeaviateNames.property(criteria.element().name());
        if (!schema.hasProperty(property)) {
            throw new IllegalArgumentException("The collection has no property " + criteria.element().name());
        }
        String type = schema.propertyType(property);
        if (type.endsWith("[]")) {
            throw new UnsupportedOperationException("Weaviate filters on array properties are not supported: "
                    + criteria.element().name());
        }
        return property;
    }

    private Filter apply(String property, Object value, Operator operator) {
        String type = schema.propertyType(property);
        Object typed = coerce(type, value, property);
        Filter.FilterBuilder builder = Filter.property(property);
        if (typed instanceof Boolean bool) {
            return switch (operator) {
                case EQ -> builder.eq(bool.booleanValue());
                default -> throw new UnsupportedOperationException("Weaviate cannot compare booleans with "
                        + operator);
            };
        }
        if (typed instanceof Long number) {
            return switch (operator) {
                case EQ -> builder.eq(number.longValue());
                case GT -> builder.gt(number.longValue());
                case GTE -> builder.gte(number.longValue());
                case LT -> builder.lt(number.longValue());
                case LTE -> builder.lte(number.longValue());
            };
        }
        if (typed instanceof Double number) {
            return switch (operator) {
                case EQ -> builder.eq(number.doubleValue());
                case GT -> builder.gt(number.doubleValue());
                case GTE -> builder.gte(number.doubleValue());
                case LT -> builder.lt(number.doubleValue());
                case LTE -> builder.lte(number.doubleValue());
            };
        }
        if (operator != Operator.EQ) {
            throw new UnsupportedOperationException("Weaviate range conditions on text properties are not supported: "
                    + property);
        }
        return builder.eq((String) typed);
    }

    private static Object coerce(String type, Object value, String property) {
        if (value instanceof List<?>) {
            throw new UnsupportedOperationException("Weaviate cannot filter by a collection value");
        }
        if (!WeaviateValueConverter.isCompatible(type, value)) {
            throw new IllegalArgumentException("The property " + property + " is " + type
                    + " but the value is " + value.getClass().getSimpleName());
        }
        if (DataType.NUMBER.equals(type) && value instanceof Long number) {
            return number.doubleValue();
        }
        return value;
    }

    private enum Operator {
        EQ, GT, GTE, LT, LTE
    }
}
