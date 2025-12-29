/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.java.json.JsonObject;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.driver.StringMatch;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

record N1QLUpdateQueryBuilder(UpdateQuery query, String database, String scope) implements N1QLBuilder {
    @Override
    public N1QLQuery get() {
        if (query.set().isEmpty())
            throw new IllegalArgumentException("UpdateQuery must have at least one set operation");
        var alias = "d";
        var n1ql = new StringBuilder();
        var params = JsonObject.create();
        n1ql.append("UPDATE ")
                .append(database).append(".")
                .append(scope).append(".")
                .append(query.name())
                .append(" AS ").append(alias);

        n1ql.append(" SET ");
        String updates = query.set().stream()
                .map(element -> {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    String name =  identifierOf(alias, element.name());
                    Object value = element.get();
                    String param = "$".concat(element.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
                    params.put(param, value);
                    return name + " = " + param;
                })
                .collect(Collectors.joining(", "));
        n1ql.append(updates).append(' ');

        n1ql.append(" WHERE ");

        condition(query.condition().orElseThrow(() -> new IllegalArgumentException("UpdateQuery must have a condition")),
                alias, n1ql, params);

        n1ql.append(" RETURNING ").append(alias).append(".*");

        return N1QLQuery.of(n1ql, params, Collections.emptyList());
    }

    private void condition(CriteriaCondition condition, String alias, StringBuilder n1ql, JsonObject params) {
        Element document = condition.element();
        switch (condition.condition()) {
            case EQUALS:
                predicate(alias, n1ql, " = ", document, params);
                return;
            case IN:
                predicate(alias, n1ql, " IN ", document, params);
                return;
            case LESSER_THAN:
                predicate(alias, n1ql, " < ", document, params);
                return;
            case GREATER_THAN:
                predicate(alias, n1ql, " > ", document, params);
                return;
            case LESSER_EQUALS_THAN:
                predicate(alias, n1ql, " <= ", document, params);
                return;
            case GREATER_EQUALS_THAN:
                predicate(alias, n1ql, " >= ", document, params);
                return;
            case LIKE:
                predicate(alias, n1ql, " LIKE ", document, params);
                return;
            case CONTAINS:
                predicate(alias, n1ql, " LIKE ", Element.of(document.name(), StringMatch.CONTAINS.format(document.get(String.class))), params);
                return;
            case STARTS_WITH:
                predicate(alias, n1ql, " LIKE ", Element.of(document.name(), StringMatch.STARTS_WITH.format(document.get(String.class))), params);
                return;
            case ENDS_WITH:
                predicate(alias, n1ql, " LIKE ", Element.of(document.name(), StringMatch.ENDS_WITH.format(document.get(String.class))), params);
                return;
            case NOT:
                n1ql.append(" NOT ");
                condition(document.get(CriteriaCondition.class), alias, n1ql, params);
                return;
            case OR:
                appendCondition(alias, n1ql, params, document.get(new TypeReference<>() {
                }), " OR ");
                return;
            case AND:
                appendCondition(alias, n1ql, params, document.get(new TypeReference<>() {
                }), " AND ");
                return;
            case BETWEEN:
                predicateBetween(alias, n1ql, params, document);
                return;
            default:
                throw new UnsupportedOperationException("There is not support condition for " + condition.condition());
        }
    }

    private void predicateBetween(String alias, StringBuilder n1ql, JsonObject params, Element document) {
        n1ql.append(" BETWEEN ");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = identifierOf(alias, document.name());

        List<Object> values = new ArrayList<>();
        ((Iterable<?>) document.get()).forEach(values::add);

        String param = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        String param2 = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        n1ql.append(name).append(" ").append(param).append(" AND ").append(param2);
        params.put(param, values.get(0));
        params.put(param2, values.get(1));
    }

    private void appendCondition(String alias, StringBuilder n1ql, JsonObject params,
                                 List<CriteriaCondition> conditions,
                                 String condition) {
        int index = 0;
        for (CriteriaCondition documentCondition : conditions) {
            StringBuilder query = new StringBuilder();
            condition(documentCondition, alias, query, params);
            if (index == 0) {
                n1ql.append(" ").append(query);
            } else if (!query.isEmpty()) {
                n1ql.append(condition).append(query);
            }
            index++;
        }
    }

    private void predicate(String alias,
                           StringBuilder n1ql,
                           String condition,
                           Element document,
                           JsonObject params) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = identifierOf(alias,document.name());
        Object value = document.get();
        String param = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        n1ql.append(name).append(condition).append(param);
        params.put(param, value);
    }

    private String identifierOf(String alias, String field) {
        return "%s.%s".formatted(alias, field);
    }

}