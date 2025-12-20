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
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.List;

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

    private QueryOSQLConverter() {
    }

    static Query select(SelectQuery documentQuery) {
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();
        List<ORecordId> ids = new ArrayList<>();
        query.append("SELECT FROM ");
        query.append(documentQuery.name());

        if (documentQuery.condition().isPresent()) {
            query.append(WHERE);
            definesCondition(documentQuery.condition().get(), query, params, 0, ids, false);
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
        query.append(documentQuery.name());
        if (documentQuery.condition().isPresent()) {
            query.append(WHERE);
            definesCondition(documentQuery.condition().get(), query, params, 0, ids, true);
        }
        return new Query(query.toString(), params, ids);
    }

    private static void definesCondition(CriteriaCondition condition, StringBuilder query, List<Object> params,
                                         int counter, List<ORecordId> ids, boolean countQuery) {

        Element document = condition.element();
        switch (condition.condition()) {
            case IN:
                appendCondition(query, params, document, IN, ids, countQuery);
                return;
            case EQUALS:
                appendCondition(query, params, document, EQUALS, ids, countQuery);
                return;
            case GREATER_EQUALS_THAN:
                appendCondition(query, params, document, GREATER_EQUALS_THAN, ids, countQuery);
                return;
            case GREATER_THAN:
                appendCondition(query, params, document, GREATER_THAN, ids, countQuery);
                return;
            case LESSER_THAN:
                appendCondition(query, params, document, LESSER_THAN, ids, countQuery);
                return;
            case LESSER_EQUALS_THAN:
                appendCondition(query, params, document, LESSER_EQUALS_THAN, ids, countQuery);
                return;
            case LIKE:
                appendCondition(query, params, document, LIKE, ids, countQuery);
                return;
            case STARTS_WITH:
                appendCondition(query, params, Element.of(document.name(), StringMatch.STARTS_WITH.format(document.get(String.class))), LIKE, ids, countQuery);
                return;
            case CONTAINS:
                appendCondition(query, params, Element.of(document.name(), StringMatch.CONTAINS.format(document.get(String.class))), LIKE, ids, countQuery);
                return;
            case ENDS_WITH:
                appendCondition(query, params, Element.of(document.name(), StringMatch.ENDS_WITH.format(document.get(String.class))), LIKE, ids, countQuery);
                return;
            case AND:
                for (CriteriaCondition dc : document.get(new TypeReference<List<CriteriaCondition>>() {
                })) {

                    if (isFirstCondition(query, counter)) {
                        query.append(AND);
                    }
                    definesCondition(dc, query, params, ++counter, ids, countQuery);
                }
                return;
            case OR:
                for (CriteriaCondition dc : document.get(new TypeReference<List<CriteriaCondition>>() {
                })) {
                    if (isFirstCondition(query, counter)) {
                        query.append(OR);
                    }
                    definesCondition(dc, query, params, ++counter, ids, countQuery);
                }
                return;
            case NOT:
                CriteriaCondition documentCondition = document.get(CriteriaCondition.class);
                query.append("NOT (");
                definesCondition(documentCondition, query, params, ++counter, ids, countQuery);
                query.append(")");
                return;
            default:
                throw new IllegalArgumentException("Orient DB has not support to the condition " + condition.condition());
        }
    }

    private static boolean isFirstCondition(StringBuilder query, int counter) {
        return counter > 0 && !WHERE.equals(query.substring(query.length() - 7));
    }

    private static void appendCondition(StringBuilder query, List<Object> params,
                                        Element document, String condition, List<ORecordId> ids, boolean countQuery) {

        if(!countQuery) {
            if (RID_FIELD.equals(document.name())
                    ||
                    ID_FIELD.equals(document.name())) {
                String id = document.get(String.class);
                ids.add(new ORecordId(id));
                return;
            }
        }
        String attributeName = document.name();
        if(countQuery) {
            if (ID_FIELD.equals(document.name())) {
                attributeName = RID_FIELD;
            }
        }
        query.append(attributeName)
                .append(condition).append(PARAM_APPENDER);
        if (IN.equals(condition)) {
            params.add(ValueUtil.convertToList(document.value()));
        } else {
            params.add(document.get());
        }
    }

    private static void appendSort(List<Sort<?>> sorts, StringBuilder query) {
        query.append(SORT);
        String separator = SPACE;
        for (Sort<?> sort : sorts) {
            query.append(separator)
                    .append(sort.property())
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

    record Query(String query, List<Object> params, List<ORecordId> ids) {
    }
}
