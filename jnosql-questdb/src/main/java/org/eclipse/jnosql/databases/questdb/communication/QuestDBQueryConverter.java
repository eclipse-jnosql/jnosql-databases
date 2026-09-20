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
package org.eclipse.jnosql.databases.questdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jnosql.databases.questdb.communication.QuestDBEntityConverter.ID_FIELD;
import static org.eclipse.jnosql.databases.questdb.communication.QuestDBEntityConverter.TIMESTAMP_COLUMN;
import static org.eclipse.jnosql.databases.questdb.communication.QuestDBEntityConverter.identifier;

final class QuestDBQueryConverter {

    private QuestDBQueryConverter() {
    }

    static QuestDBQuery convert(SelectQuery query) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(columns(query.columns()))
                .append(" FROM ")
                .append(identifier(query.name()));
        query.condition().ifPresent(condition -> sql.append(" WHERE ")
                .append(condition(condition, parameters)));
        appendSort(sql, query.sorts());
        appendLimit(sql, query.limit(), query.skip(), parameters);
        return new QuestDBQuery(sql.toString(), parameters);
    }

    static QuestDBQuery count(SelectQuery query) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(identifier(query.name()));
        query.condition().ifPresent(condition -> sql.append(" WHERE ")
                .append(condition(condition, parameters)));
        return new QuestDBQuery(sql.toString(), parameters);
    }

    static QuestDBQuery update(UpdateQuery query) {
        if (query.sets().isEmpty()) {
            throw new IllegalArgumentException("QuestDB update requires at least one column");
        }
        List<Object> parameters = new ArrayList<>();
        String assignments = query.sets().stream()
                .map(element -> assignment(element, parameters))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(identifier(query.name()))
                .append(" SET ")
                .append(assignments);
        query.where().ifPresent(condition -> sql.append(" WHERE ")
                .append(condition(condition, parameters)));
        return new QuestDBQuery(sql.toString(), parameters);
    }

    static QuestDBQuery update(QuestDBEntityConverter.QuestDBRow row) {
        List<Object> parameters = new ArrayList<>();
        String assignments = row.columns().stream()
                .map(column -> {
                    parameters.add(column.value());
                    return identifier(column.name()) + " = $" + parameters.size();
                })
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        parameters.add(row.timestamp());
        return new QuestDBQuery("UPDATE " + identifier(row.table()) + " SET " + assignments
                + " WHERE " + identifier(TIMESTAMP_COLUMN) + " = $" + parameters.size(), parameters);
    }

    private static String assignment(Element element, List<Object> parameters) {
        if (ID_FIELD.equals(element.name()) || TIMESTAMP_COLUMN.equalsIgnoreCase(element.name())) {
            throw new UnsupportedOperationException("QuestDB cannot update the designated timestamp");
        }
        Object value = ValueUtil.convert(element.value());
        if (value == null) {
            throw new UnsupportedOperationException(
                    "QuestDB native binds require a concrete type; null update values are not supported");
        }
        parameters.add(value);
        return identifier(element.name()) + " = $" + parameters.size();
    }

    private static String condition(CriteriaCondition condition, List<Object> parameters) {
        Element element = condition.element();
        String column = identifier(column(element.name()));
        return switch (condition.condition()) {
            case EQUALS -> column + " = " + parameter(element.name(), element.get(), parameters);
            case GREATER_THAN -> column + " > " + parameter(element.name(), element.get(), parameters);
            case GREATER_EQUALS_THAN -> column + " >= " + parameter(element.name(), element.get(), parameters);
            case LESSER_THAN -> column + " < " + parameter(element.name(), element.get(), parameters);
            case LESSER_EQUALS_THAN -> column + " <= " + parameter(element.name(), element.get(), parameters);
            case IN -> in(element, parameters);
            case BETWEEN -> between(element, parameters);
            case NOT -> "NOT (" + condition(element.get(CriteriaCondition.class), parameters) + ")";
            case AND -> combination(element, "AND", parameters);
            case OR -> combination(element, "OR", parameters);
            default -> throw new UnsupportedOperationException(
                    "QuestDB does not support the condition " + condition.condition());
        };
    }

    private static String in(Element element, List<Object> parameters) {
        List<Object> values = ValueUtil.convertToList(element.value());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        return identifier(column(element.name())) + " IN (" + values.stream()
                .map(value -> parameter(element.name(), value, parameters))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow() + ")";
    }

    private static String between(Element element, List<Object> parameters) {
        List<Object> values = ValueUtil.convertToList(element.value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return identifier(column(element.name())) + " BETWEEN "
                + parameter(element.name(), values.get(0), parameters) + " AND "
                + parameter(element.name(), values.get(1), parameters);
    }

    private static String combination(Element element, String operator, List<Object> parameters) {
        List<CriteriaCondition> conditions = element.get(new TypeReference<>() {
        });
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException(operator + " requires at least one condition");
        }
        return conditions.stream()
                .map(condition -> condition(condition, parameters))
                .reduce((left, right) -> "(" + left + " " + operator + " " + right + ")")
                .orElseThrow();
    }

    private static String columns(List<String> columns) {
        if (columns.isEmpty()) {
            return "*";
        }
        List<String> selected = new ArrayList<>();
        selected.add(identifier(TIMESTAMP_COLUMN));
        columns.stream()
                .filter(column -> !ID_FIELD.equals(column))
                .map(QuestDBEntityConverter::identifier)
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

    private static void appendLimit(StringBuilder sql, long limit, long skip, List<Object> parameters) {
        if (skip > 0) {
            if (limit <= 0) {
                throw new UnsupportedOperationException(
                        "QuestDB offset pagination requires a finite limit");
            }
            long upperBound;
            try {
                upperBound = Math.addExact(skip, limit);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("QuestDB pagination range exceeds the supported size", exception);
            }
            parameters.add(skip);
            sql.append(" LIMIT $").append(parameters.size());
            parameters.add(upperBound);
            sql.append(", $").append(parameters.size());
            return;
        }
        if (limit > 0) {
            parameters.add(limit);
            sql.append(" LIMIT $").append(parameters.size());
        }
    }

    private static String parameter(String name, Object value, List<Object> parameters) {
        parameters.add(QuestDBEntityConverter.queryValue(name, value));
        return "$" + parameters.size();
    }

    private static String column(String name) {
        return ID_FIELD.equals(name) ? TIMESTAMP_COLUMN : name;
    }

    record QuestDBQuery(String statement, List<Object> parameters) {

        QuestDBQuery {
            parameters = List.copyOf(parameters);
        }
    }
}
