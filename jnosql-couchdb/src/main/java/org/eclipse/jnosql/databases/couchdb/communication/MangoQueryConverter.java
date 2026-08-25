/*
 *
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import jakarta.data.Direction;
import jakarta.data.Sort;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

final class MangoQueryConverter implements Function<SelectQuery, JsonObject> {


    @Override
    public JsonObject apply(SelectQuery documentQuery) {
        JsonObjectBuilder select = Json.createObjectBuilder();

        if (!documentQuery.columns().isEmpty()) {
            select.add(CouchDBConstant.FIELDS_QUERY, Json.createArrayBuilder(documentQuery.columns()).build());
        }
        if (documentQuery.limit() > 0) {
            select.add(CouchDBConstant.LIMIT_QUERY, documentQuery.limit());
        }

        if (documentQuery.skip() > 0) {
            select.add(CouchDBConstant.SKIP_QUERY, documentQuery.skip());
        }

        if (!documentQuery.sorts().isEmpty()) {
            JsonArrayBuilder sorts = Json.createArrayBuilder();
            documentQuery.sorts().stream().map(this::createSortObject).forEach(sorts::add);
            select.add(CouchDBConstant.SORT_QUERY, sorts.build());
        }

        if (documentQuery instanceof CouchDBDocumentQuery) {
            Optional<String> bookmark = CouchDBDocumentQuery.class.cast(documentQuery).getBookmark();
            bookmark.ifPresent(b -> bookmark(b, select));
        }

        JsonObject selector = getSelector(documentQuery);
        return select.add(CouchDBConstant.SELECTOR_QUERY, selector).build();
    }

    private void bookmark(String bookmark, JsonObjectBuilder select) {
        select.add(CouchDBConstant.BOOKMARK, bookmark);
    }

    private JsonObject createSortObject(Sort sort) {
        return Json.createObjectBuilder().add(sort.property(), sort.isAscending() ? Direction.ASC.name().toLowerCase(Locale.US)
                : Direction.DESC.name().toLowerCase(Locale.US)).build();
    }

    private JsonObject getSelector(SelectQuery documentQuery) {
        JsonObjectBuilder selector = Json.createObjectBuilder();
        selector.add(CouchDBConstant.ENTITY, documentQuery.name());
        documentQuery.condition().ifPresent(d -> appendCondition(d, selector));
        return selector.build();
    }

    private void appendCondition(CriteriaCondition condition, JsonObjectBuilder selector) {
        Element document = condition.element();
        String name = document.name();
        Object value = ValueUtil.convert(document.value());

        switch (condition.condition()) {
            case EQUALS:
                appendCondition(selector, name, value);
                return;
            case GREATER_THAN:
                appendCondition(CouchDBConstant.GT_CONDITION, name, value, selector);
                return;
            case GREATER_EQUALS_THAN:
                appendCondition(CouchDBConstant.GTE_CONDITION, name, value, selector);
                return;
            case LESSER_THAN:
                appendCondition(CouchDBConstant.LT_CONDITION, name, value, selector);
                return;
            case LESSER_EQUALS_THAN:
                appendCondition(CouchDBConstant.LTE_CONDITION, name, value, selector);
                return;
            case IN:
                appendCondition(CouchDBConstant.IN_CONDITION, name, getArray(document.value()), selector);
                return;
            case NOT:
                appendNot(selector, value);
                return;
            case AND:
                appendCombination(selector, value, CouchDBConstant.AND_CONDITION);
                return;
            case OR:
                appendCombination(selector, value, CouchDBConstant.OR_CONDITION);
                return;
            default:
                throw new UnsupportedOperationException("This operation is not supported at couchdb: " + condition.condition());

        }
    }

    private void appendCombination(JsonObjectBuilder selector, Object value, String combination) {
        List<CriteriaCondition> conditions = List.class.cast(value);
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (CriteriaCondition documentCondition : conditions) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            appendCondition(documentCondition, builder);
            arrayBuilder.add(builder.build());
        }
        selector.add(combination, arrayBuilder.build());
    }

    private void appendNot(JsonObjectBuilder selector, Object value) {
        JsonObjectBuilder not = Json.createObjectBuilder();
        appendCondition(CriteriaCondition.class.cast(value), not);
        selector.add("$not", not.build());
    }

    private void appendCondition(String operator, String name, Object value, JsonObjectBuilder selector) {
        JsonObjectBuilder condition = Json.createObjectBuilder();
        appendCondition(condition, operator, value);
        selector.add(name, condition.build());
    }

    private void appendCondition(JsonObjectBuilder condition, String name, Object value) {
        if (value instanceof String) {
            condition.add(name, value.toString());
            return;
        }
        if (value instanceof Boolean) {
            condition.add(name, Boolean.class.cast(value));
            return;
        }
        if (value instanceof Number) {
            condition.add(name, Number.class.cast(value).doubleValue());
            return;
        }
        if (value instanceof JsonArray) {
            condition.add(name, JsonArray.class.cast(value));
            return;
        }
        condition.add(name, Value.of(value).get(String.class));
    }

    private JsonArray getArray(Value value) {
        List<Object> items = ValueUtil.convertToList(value);
        return Json.createArrayBuilder(items).build();
    }

}
