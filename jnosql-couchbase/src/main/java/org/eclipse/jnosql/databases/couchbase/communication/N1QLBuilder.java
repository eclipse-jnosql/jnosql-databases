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
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.java.json.JsonObject;
import jakarta.data.Direction;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.driver.StringMatch;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class N1QLBuilder implements Supplier<N1QLQuery> {

    private final SelectQuery query;

    private final String database;

    private final String scope;

    private final boolean shouldCount;

    private N1QLBuilder(SelectQuery query, String database, String scope, boolean shouldCount) {
        this.query = query;
        this.database = database;
        this.scope = scope;
        this.shouldCount = shouldCount;
    }

    @Override
    public N1QLQuery get() {

        StringBuilder n1ql = new StringBuilder();
        JsonObject params = JsonObject.create();
        List<String> ids = new ArrayList<>();

        n1ql.append("select ");
        n1ql.append(select()).append(' ');
        n1ql.append("from ")
                .append(database).append(".")
                .append(scope).append(".")
                .append(query.name());

        query.condition().ifPresent(c -> {
            n1ql.append(" WHERE ");
            condition(c, n1ql, params, ids);
        });

        if (shouldCount) {
            return N1QLQuery.of(n1ql, params, ids);
        }

        if (!query.sorts().isEmpty()) {
            n1ql.append(" ORDER BY ");
            String order = query.sorts().stream()
                    .map(s -> s.property() + " " + (s.isAscending() ? Direction.ASC : Direction.DESC))
                    .collect(Collectors.joining(", "));
            n1ql.append(order);
        }

        if (query.limit() > 0) {
            n1ql.append(" LIMIT ").append(query.limit());
        }

        if (query.skip() > 0) {
            n1ql.append(" OFFSET ").append(query.skip());
        }

        return N1QLQuery.of(n1ql, params, ids);
    }

    private void condition(CriteriaCondition condition, StringBuilder n1ql, JsonObject params, List<String> ids) {
        Element document = condition.element();
        switch (condition.condition()) {
            case EQUALS:
                if (document.name().equals(EntityConverter.ID_FIELD) && !shouldCount) {
                    ids.add(document.get(String.class));
                } else {
                    predicate(n1ql, " = ", document, params);
                }
                return;
            case IN:
                if (document.name().equals(EntityConverter.ID_FIELD) && !shouldCount) {
                    ids.addAll(document.get(new TypeReference<List<String>>() {
                    }));
                } else {
                    predicate(n1ql, " IN ", document, params);
                }
                return;
            case LESSER_THAN:
                predicate(n1ql, " < ", document, params);
                return;
            case GREATER_THAN:
                predicate(n1ql, " > ", document, params);
                return;
            case LESSER_EQUALS_THAN:
                predicate(n1ql, " <= ", document, params);
                return;
            case GREATER_EQUALS_THAN:
                predicate(n1ql, " >= ", document, params);
                return;
            case LIKE:
                predicate(n1ql, " LIKE ", document, params);
                return;
            case CONTAINS:
                predicate(n1ql, " LIKE ", Element.of(document.name(), StringMatch.CONTAINS.format(document.get(String.class))), params);
                return;
            case STARTS_WITH:
                predicate(n1ql, " LIKE ", Element.of(document.name(), StringMatch.STARTS_WITH.format(document.get(String.class))), params);
                return;
            case ENDS_WITH:
                predicate(n1ql, " LIKE ", Element.of(document.name(), StringMatch.ENDS_WITH.format(document.get(String.class))), params);
                return;
            case NOT:
                n1ql.append(" NOT ");
                condition(document.get(CriteriaCondition.class), n1ql, params, ids);
                return;
            case OR:
                appendCondition(n1ql, params, document.get(new TypeReference<>() {
                }), " OR ", ids);
                return;
            case AND:
                appendCondition(n1ql, params, document.get(new TypeReference<>() {
                }), " AND ", ids);
                return;
            case BETWEEN:
                predicateBetween(n1ql, params, document);
                return;
            default:
                throw new UnsupportedOperationException("There is not support condition for " + condition.condition());
        }
    }

    private void predicateBetween(StringBuilder n1ql, JsonObject params, Element document) {
        n1ql.append(" BETWEEN ");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = identifierOf(document.name());

        List<Object> values = new ArrayList<>();
        ((Iterable<?>) document.get()).forEach(values::add);

        String param = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        String param2 = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        n1ql.append(name).append(" ").append(param).append(" AND ").append(param2);
        params.put(param, values.get(0));
        params.put(param2, values.get(1));
    }

    private void appendCondition(StringBuilder n1ql, JsonObject params,
                                 List<CriteriaCondition> conditions,
                                 String condition, List<String> ids) {
        int index = 0;
        for (CriteriaCondition documentCondition : conditions) {
            StringBuilder query = new StringBuilder();
            condition(documentCondition, query, params, ids);
            if (index == 0) {
                n1ql.append(" ").append(query);
            } else if (!query.isEmpty()) {
                n1ql.append(condition).append(query);
            }
            index++;
        }
    }

    private void predicate(StringBuilder n1ql,
                           String condition,
                           Element document,
                           JsonObject params) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = identifierOf(document.name());
        Object value = document.get();
        String param = "$".concat(document.name()).concat("_").concat(Integer.toString(random.nextInt(0, 100)));
        n1ql.append(name).append(condition).append(param);
        params.put(param, value);
    }

    private String identifierOf(String name) {
        return ' ' + name + ' ';
    }

    private String select() {
        if (shouldCount) {
            return "COUNT(*)";
        }
        String documents = String.join(", ", query.columns());
        if (documents.isBlank()) {
            return "*";
        }
        return documents;
    }

    public static N1QLBuilder of(SelectQuery query, String database, String scope) {
        return new N1QLBuilder(query, database, scope, false);
    }

    public static N1QLBuilder countOf(SelectQuery query, String database, String scope) {
        return new N1QLBuilder(query, database, scope, true);
    }
}
