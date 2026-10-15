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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.grpc.Common.Condition;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.Common.Range;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Translates a {@link CriteriaCondition} into a Qdrant {@link Filter}.
 * It is shared by select, count, delete and vector search.
 */
final class QdrantFilterConverter {

    private QdrantFilterConverter() {
    }

    static Optional<Filter> convert(Optional<CriteriaCondition> condition) {
        return condition.map(QdrantFilterConverter::toFilter);
    }

    static Filter toFilter(CriteriaCondition criteria) {
        return switch (criteria.condition()) {
            case AND -> Filter.newBuilder().addAllMust(children(criteria)).build();
            case OR -> Filter.newBuilder().addAllShould(children(criteria)).build();
            case NOT -> Filter.newBuilder().addMustNot(toCondition(criteria.element().get(CriteriaCondition.class))).build();
            default -> Filter.newBuilder().addMust(toCondition(criteria)).build();
        };
    }

    private static List<Condition> children(CriteriaCondition criteria) {
        List<CriteriaCondition> conditions = criteria.element().value().get(new TypeReference<>() {
        });
        return conditions.stream().map(QdrantFilterConverter::toCondition).toList();
    }

    private static Condition toCondition(CriteriaCondition criteria) {
        String field = criteria.element().name();
        Object value = QdrantValueWriteDecorator.convert(criteria.element().value());
        return switch (criteria.condition()) {
            case AND, OR, NOT -> ConditionFactory.filter(toFilter(criteria));
            case EQUALS -> equalsTo(field, value);
            case GREATER_THAN -> ConditionFactory.range(field, Range.newBuilder().setGt(number(value)).build());
            case GREATER_EQUALS_THAN -> ConditionFactory.range(field, Range.newBuilder().setGte(number(value)).build());
            case LESSER_THAN -> ConditionFactory.range(field, Range.newBuilder().setLt(number(value)).build());
            case LESSER_EQUALS_THAN -> ConditionFactory.range(field, Range.newBuilder().setLte(number(value)).build());
            case BETWEEN -> between(field, criteria);
            case IN -> in(field, criteria);
            case CONTAINS -> ConditionFactory.matchText(field, String.valueOf(value));
            default -> throw new UnsupportedOperationException("Qdrant does not support the condition "
                    + criteria.condition());
        };
    }

    private static Condition equalsTo(String field, Object value) {
        if (value == null) {
            return ConditionFactory.filter(Filter.newBuilder()
                    .addShould(ConditionFactory.isNull(field))
                    .addShould(ConditionFactory.isEmpty(field)).build());
        }
        if (QdrantPointIds.ID_FIELD.equals(field)) {
            return ConditionFactory.hasId(QdrantPointIds.of(value));
        }
        if (value instanceof Boolean bool) {
            return ConditionFactory.match(field, bool);
        }
        if (isIntegral(value)) {
            return ConditionFactory.match(field, ((Number) value).longValue());
        }
        if (value instanceof CharSequence || value instanceof UUID || value instanceof Enum<?>) {
            return ConditionFactory.matchKeyword(field, text(value));
        }
        throw new UnsupportedOperationException("Qdrant cannot match the field " + field
                + " by equality with a value of type " + value.getClass().getName());
    }

    private static Condition between(String field, CriteriaCondition criteria) {
        List<Object> values = QdrantValueWriteDecorator.convertToList(criteria.element().value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return ConditionFactory.range(field, Range.newBuilder()
                .setGte(number(values.get(0))).setLte(number(values.get(1))).build());
    }

    private static Condition in(String field, CriteriaCondition criteria) {
        List<Object> values = QdrantValueWriteDecorator.convertToList(criteria.element().value());
        if (QdrantPointIds.ID_FIELD.equals(field)) {
            List<PointId> ids = values.stream().map(QdrantPointIds::of).toList();
            return ConditionFactory.hasId(ids);
        }
        if (!values.isEmpty() && values.stream().allMatch(QdrantFilterConverter::isIntegral)) {
            return ConditionFactory.matchValues(field, values.stream().map(v -> ((Number) v).longValue()).toList());
        }
        if (values.stream().allMatch(value -> value instanceof CharSequence
                || value instanceof UUID || value instanceof Enum<?>)) {
            return ConditionFactory.matchKeywords(field, values.stream().map(QdrantFilterConverter::text).toList());
        }
        return ConditionFactory.filter(Filter.newBuilder()
                .addAllShould(values.stream().map(value -> equalsTo(field, value)).toList()).build());
    }

    private static boolean isIntegral(Object value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
    }

    private static String text(Object value) {
        return value instanceof Enum<?> enumeration ? enumeration.name() : String.valueOf(value);
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new UnsupportedOperationException("Qdrant range conditions require numeric values but got: " + value);
    }
}
