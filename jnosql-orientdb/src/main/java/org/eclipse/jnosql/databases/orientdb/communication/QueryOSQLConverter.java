/*
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
 *   Lucas Furlaneto
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.orientdb.communication;


import com.orientechnologies.orient.core.id.ORecordId;
import jakarta.data.Direction;
import jakarta.data.Sort;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.driver.StringMatch;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.ID_FIELD;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.RID_FIELD;

final class QueryOSQLConverter {

    private static final String WHERE = " WHERE ";
    private static final String IN = " IN ";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String EQUALS = " = ";
    private static final String GREATER_EQUALS_THAN = " >= ";
    private static final String GREATER_THAN = " > ";
    private static final String LESSER_THAN = " < ";
    private static final String LESSER_EQUALS_THAN = " <= ";
    private static final String LIKE = " LIKE ";
    private static final String SKIP = " SKIP ";
    private static final String LIMIT = " LIMIT ";
    private static final String SORT = " ORDER BY";
    private static final String SPACE = " ";
    private static final char PARAM_APPENDER = '?';
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private QueryOSQLConverter() {
    }

    static Query select(SelectQuery documentQuery) {
        return select(documentQuery, true);
    }

    /**
     * @param allowIdLookup when false a standalone id predicate stays in the SQL, which is required
     *                      by consumers that only execute the statement, such as live queries
     */
    static Query select(SelectQuery documentQuery, boolean allowIdLookup) {
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();
        List<ORecordId> ids = new ArrayList<>();
        query.append("SELECT FROM ");
        query.append(identifier(documentQuery.name()));

        Optional<CriteriaCondition> condition = documentQuery.condition();
        if (allowIdLookup && condition.isPresent() && isIdLookup(condition.get())) {
            ids.add(new ORecordId(condition.get().element().get(String.class)));
        } else if (condition.isPresent()) {
            query.append(WHERE);
            definesCondition(condition.get(), query, params);
        }

        if (!documentQuery.sorts().isEmpty()) {
            appendSort(documentQuery.sorts(), query);
        }

        appendPagination(documentQuery, query);
        return new Query(query.toString(), params, ids);
    }

    static Query selectCount(SelectQuery documentQuery) {
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();
        List<ORecordId> ids = new ArrayList<>();
        query.append("SELECT COUNT(*) FROM ");
        query.append(identifier(documentQuery.name()));
        if (documentQuery.condition().isPresent()) {
            query.append(WHERE);
            definesCondition(documentQuery.condition().get(), query, params);
        }
        return new Query(query.toString(), params, ids);
    }

    private static void definesCondition(CriteriaCondition condition, StringBuilder query, List<Object> params) {

        if (condition.isNative()) {
            query.append(OrientNativeFunction.convert(condition, params));
            return;
        }
        Element document = condition.element();
        switch (condition.condition()) {
            case IN:
                appendCondition(query, params, document, IN);
                return;
            case EQUALS:
                appendCondition(query, params, document, EQUALS);
                return;
            case GREATER_EQUALS_THAN:
                appendCondition(query, params, document, GREATER_EQUALS_THAN);
                return;
            case GREATER_THAN:
                appendCondition(query, params, document, GREATER_THAN);
                return;
            case LESSER_THAN:
                appendCondition(query, params, document, LESSER_THAN);
                return;
            case LESSER_EQUALS_THAN:
                appendCondition(query, params, document, LESSER_EQUALS_THAN);
                return;
            case LIKE:
                appendCondition(query, params, document, LIKE);
                return;
            case STARTS_WITH:
                appendCondition(query, params, Element.of(document.name(), StringMatch.STARTS_WITH.format(document.get(String.class))), LIKE);
                return;
            case CONTAINS:
                appendCondition(query, params, Element.of(document.name(), StringMatch.CONTAINS.format(document.get(String.class))), LIKE);
                return;
            case ENDS_WITH:
                appendCondition(query, params, Element.of(document.name(), StringMatch.ENDS_WITH.format(document.get(String.class))), LIKE);
                return;

            case AND:
                appendGroup(condition, query, params, AND);
                return;
            case OR:
                appendGroup(condition, query, params, OR);
                return;
            case NOT:
                CriteriaCondition documentCondition = document.get(CriteriaCondition.class);
                query.append("NOT (");
                definesCondition(documentCondition, query, params);
                query.append(")");
                return;
            default:
                throw new IllegalArgumentException("Orient DB has not support to the condition " + condition.condition());
        }
    }

    private static void appendGroup(CriteriaCondition condition, StringBuilder query, List<Object> params,
                                    String operator) {
        String separator = "";
        for (CriteriaCondition child : condition.element().get(new TypeReference<List<CriteriaCondition>>() {
        })) {
            query.append(separator);
            boolean grouped = child.condition() == Condition.AND || child.condition() == Condition.OR;
            if (grouped) {
                query.append("(");
            }
            definesCondition(child, query, params);
            if (grouped) {
                query.append(")");
            }
            separator = operator;
        }
    }

    private static void appendCondition(StringBuilder query, List<Object> params, Element document, String condition) {
        String attributeName = ID_FIELD.equals(document.name()) ? RID_FIELD : document.name();
        query.append(identifier(attributeName))
                .append(condition).append(PARAM_APPENDER);
        if (IN.equals(condition)) {
            params.add(ValueUtil.convertToList(document.value()));
        } else {
            params.add(document.get());
        }
    }

    private static boolean isIdLookup(CriteriaCondition condition) {
        return !condition.isNative() && condition.condition() == Condition.EQUALS
                && (RID_FIELD.equals(condition.element().name()) || ID_FIELD.equals(condition.element().name()));
    }

    private static void appendSort(List<Sort<?>> sorts, StringBuilder query) {
        query.append(SORT);
        String separator = SPACE;
        for (Sort<?> sort : sorts) {
            query.append(separator)
                    .append(identifier(sort.property()))
                    .append(SPACE)
                    .append(sort.isAscending() ? Direction.ASC : Direction.DESC);
            separator = ", ";
        }
    }

    private static void appendPagination(SelectQuery documentQuery, StringBuilder query) {
        if (documentQuery.skip() > 0) {
            query.append(SKIP).append(documentQuery.skip());
        }

        if (documentQuery.limit() > 0) {
            query.append(LIMIT).append(documentQuery.limit());
        }
    }

    static String identifier(String name) {
        if (RID_FIELD.equals(name)) {
            return name;
        }
        for (String part : name.split("\\.", -1)) {
            if (!IDENTIFIER.matcher(part).matches()) {
                throw new IllegalArgumentException("Invalid OrientDB identifier: " + name);
            }
        }
        return name;
    }

    record Query(String query, List<Object> params, List<ORecordId> ids) {
    }
}
