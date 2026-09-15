/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.iotdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.jnosql.databases.iotdb.communication.IoTDBEntityConverter.ID_FIELD;
import static org.eclipse.jnosql.databases.iotdb.communication.IoTDBEntityConverter.TIME_COLUMN;
import static org.eclipse.jnosql.databases.iotdb.communication.IoTDBEntityConverter.identifier;

final class IoTDBQueryConverter {

    private IoTDBQueryConverter() {
    }

    static String convert(SelectQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(columns(query.columns()))
                .append(" FROM ")
                .append(identifier(query.name()));
        query.condition().ifPresent(condition -> sql.append(" WHERE ").append(condition(condition, false)));
        appendSort(sql, query.sorts());
        appendLimit(sql, query.limit(), query.skip());
        return sql.toString();
    }

    static String count(SelectQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(identifier(query.name()));
        query.condition().ifPresent(condition -> sql.append(" WHERE ").append(condition(condition, false)));
        return sql.toString();
    }

    static String delete(DeleteQuery query) {
        if (!query.columns().isEmpty()) {
            throw new UnsupportedOperationException("IoTDB cannot delete individual FIELD columns");
        }
        CriteriaCondition condition = query.condition().orElseThrow(() ->
                new UnsupportedOperationException("IoTDB delete requires a temporal condition"));
        return "DELETE FROM " + identifier(query.name()) + " WHERE " + deleteCondition(condition);
    }

    private static String condition(CriteriaCondition condition, boolean temporalOnly) {
        Element element = condition.element();
        String name = element.name();
        String column = identifier(column(name));
        return switch (condition.condition()) {
            case EQUALS -> column + " = " + literal(name, element.get());
            case GREATER_THAN -> column + " > " + literal(name, element.get());
            case GREATER_EQUALS_THAN -> column + " >= " + literal(name, element.get());
            case LESSER_THAN -> column + " < " + literal(name, element.get());
            case LESSER_EQUALS_THAN -> column + " <= " + literal(name, element.get());
            case IN -> in(element);
            case BETWEEN -> between(element);
            case NOT -> "NOT (" + condition(element.get(CriteriaCondition.class), temporalOnly) + ")";
            case AND -> combination(element, "AND", temporalOnly);
            case OR -> combination(element, "OR", temporalOnly);
            default -> throw new UnsupportedOperationException(
                    "IoTDB does not support the condition " + condition.condition());
        };
    }

    private static String deleteCondition(CriteriaCondition condition) {
        Element element = condition.element();
        if (condition.condition() == org.eclipse.jnosql.communication.Condition.AND) {
            return combination(element, "AND", true);
        }
        if (!ID_FIELD.equals(element.name())) {
            throw new UnsupportedOperationException(
                    "IoTDB delete supports only conditions on the temporal identifier");
        }
        return switch (condition.condition()) {
            case EQUALS -> identifier(TIME_COLUMN) + " = " + literal(ID_FIELD, element.get());
            case GREATER_THAN -> identifier(TIME_COLUMN) + " > " + literal(ID_FIELD, element.get());
            case GREATER_EQUALS_THAN -> identifier(TIME_COLUMN) + " >= " + literal(ID_FIELD, element.get());
            case LESSER_THAN -> identifier(TIME_COLUMN) + " < " + literal(ID_FIELD, element.get());
            case LESSER_EQUALS_THAN -> identifier(TIME_COLUMN) + " <= " + literal(ID_FIELD, element.get());
            default -> throw new UnsupportedOperationException(
                    "IoTDB delete supports temporal comparisons combined with AND");
        };
    }

    private static String in(Element element) {
        List<?> values = values(element);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        return identifier(column(element.name())) + " IN (" + values.stream()
                .map(value -> literal(element.name(), value))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow() + ")";
    }

    private static String between(Element element) {
        List<?> values = values(element);
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return identifier(column(element.name())) + " BETWEEN "
                + literal(element.name(), values.get(0)) + " AND "
                + literal(element.name(), values.get(1));
    }

    private static String combination(Element element, String operator, boolean temporalOnly) {
        List<CriteriaCondition> conditions = element.get(new TypeReference<>() {
        });
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException(operator + " requires at least one condition");
        }
        return conditions.stream()
                .map(condition -> temporalOnly ? deleteCondition(condition) : condition(condition, false))
                .reduce((left, right) -> "(" + left + " " + operator + " " + right + ")")
                .orElseThrow();
    }

    private static String columns(List<String> columns) {
        if (columns.isEmpty()) {
            return "*";
        }
        List<String> selected = new ArrayList<>();
        selected.add(identifier(TIME_COLUMN));
        columns.stream()
                .filter(column -> !ID_FIELD.equals(column))
                .map(IoTDBEntityConverter::identifier)
                .forEach(selected::add);
        return String.join(", ", selected);
    }

    private static void appendSort(StringBuilder sql, List<Sort<?>> sorts) {
        if (!sorts.isEmpty()) {
            sql.append(" ORDER BY ").append(sorts.stream()
                    .map(sort -> identifier(column(sort.property())) + (sort.isAscending() ? " ASC" : " DESC"))
                    .reduce((left, right) -> left + ", " + right)
                    .orElseThrow());
        }
    }

    private static void appendLimit(StringBuilder sql, long limit, long skip) {
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        if (skip > 0) {
            sql.append(" OFFSET ").append(skip);
        }
    }

    static String literal(String name, Object value) {
        Object converted = IoTDBEntityConverter.unwrap(value);
        if (ID_FIELD.equals(name)) {
            return Long.toString(IoTDBEntityConverter.toEpochMillis(converted));
        }
        return switch (converted) {
            case Boolean bool -> bool.toString().toUpperCase();
            case Byte number -> number.toString();
            case Short number -> number.toString();
            case Integer number -> number.toString();
            case Long number -> number.toString();
            case BigInteger number -> number.toString();
            case Float number when Float.isFinite(number) -> number.toString();
            case Double number when Double.isFinite(number) -> number.toString();
            case BigDecimal number -> number.toPlainString();
            case Character character -> quoted(character.toString());
            case CharSequence text -> quoted(text.toString());
            case UUID uuid -> quoted(uuid.toString());
            case Instant instant -> Long.toString(instant.toEpochMilli());
            case LocalDateTime dateTime -> Long.toString(dateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
            case OffsetDateTime dateTime -> Long.toString(dateTime.toInstant().toEpochMilli());
            case ZonedDateTime dateTime -> Long.toString(dateTime.toInstant().toEpochMilli());
            case LocalDate date -> "DATE " + quoted(date.toString());
            case null -> throw new IllegalArgumentException("IoTDB predicates cannot compare null values");
            default -> throw new IllegalArgumentException(
                    "Unsupported IoTDB query value type: " + converted.getClass().getName());
        };
    }

    private static String quoted(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static List<?> values(Element element) {
        return org.eclipse.jnosql.communication.ValueUtil.convertToList(element.value());
    }

    private static String column(String name) {
        return ID_FIELD.equals(name) ? TIME_COLUMN : name;
    }
}
