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
package org.eclipse.jnosql.databases.influxdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jnosql.databases.influxdb.communication.InfluxDBEntityConverter.ID_FIELD;

final class InfluxDBQueryConverter {

    static final String COUNT_COLUMN = "jnosql_count";

    private InfluxDBQueryConverter() {
    }

    static InfluxDBQuery convert(SelectQuery query) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String columns = columns(query.columns());
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(columns)
                .append(" FROM ")
                .append(identifier(query.name()));

        query.condition().ifPresent(condition -> sql.append(" WHERE ")
                .append(condition(condition, parameters)));
        appendSort(sql, query.sorts());
        appendLimit(sql, query.limit(), query.skip());
        return new InfluxDBQuery(sql.toString(), parameters);
    }

    static InfluxDBQuery count(SelectQuery query) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS ")
                .append(identifier(COUNT_COLUMN))
                .append(" FROM ")
                .append(identifier(query.name()));

        query.condition().ifPresent(condition -> sql.append(" WHERE ")
                .append(condition(condition, parameters)));
        return new InfluxDBQuery(sql.toString(), parameters);
    }

    private static String condition(CriteriaCondition condition, Map<String, Object> parameters) {
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
                    "InfluxDB does not support the condition " + condition.condition());
        };
    }

    private static String in(Element element, Map<String, Object> parameters) {
        List<Object> values = ValueUtil.convertToList(element.value());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        List<String> placeholders = values.stream()
                .map(value -> parameter(element.name(), value, parameters))
                .toList();
        return identifier(column(element.name())) + " IN (" + String.join(", ", placeholders) + ")";
    }

    private static String between(Element element, Map<String, Object> parameters) {
        List<Object> values = ValueUtil.convertToList(element.value());
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN requires exactly two values");
        }
        return identifier(column(element.name())) + " BETWEEN "
                + parameter(element.name(), values.get(0), parameters) + " AND "
                + parameter(element.name(), values.get(1), parameters);
    }

    private static String combination(Element element, String operator, Map<String, Object> parameters) {
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
        selected.add(identifier("time"));
        columns.stream()
                .filter(column -> !ID_FIELD.equals(column))
                .map(InfluxDBQueryConverter::identifier)
                .forEach(selected::add);
        return String.join(", ", selected);
    }

    private static void appendSort(StringBuilder sql, List<Sort<?>> sorts) {
        if (sorts.isEmpty()) {
            return;
        }
        sql.append(" ORDER BY ");
        sql.append(sorts.stream()
                .map(sort -> identifier(column(sort.property())) + (sort.isAscending() ? " ASC" : " DESC"))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow());
    }

    private static void appendLimit(StringBuilder sql, long limit, long skip) {
        if (skip > 0 && limit <= 0) {
            throw new UnsupportedOperationException("InfluxDB offset requires a finite limit");
        }
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
            if (skip > 0) {
                sql.append(" OFFSET ").append(skip);
            }
        }
    }

    private static String parameter(String name, Object value, Map<String, Object> parameters) {
        Object converted = ID_FIELD.equals(name)
                ? InfluxDBEntityConverter.toInstant(value).toString()
                : value;
        if (converted == null) {
            throw new IllegalArgumentException("InfluxDB predicates cannot compare null values");
        }
        String parameter = "p" + parameters.size();
        parameters.put(parameter, converted);
        return "$" + parameter;
    }

    private static String column(String name) {
        return ID_FIELD.equals(name) ? "time" : name;
    }

    private static String identifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    record InfluxDBQuery(String statement, Map<String, Object> parameters) {
    }
}
