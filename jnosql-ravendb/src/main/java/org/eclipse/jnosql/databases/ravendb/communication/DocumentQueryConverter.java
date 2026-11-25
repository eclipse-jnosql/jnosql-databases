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
 */

package org.eclipse.jnosql.databases.ravendb.communication;

import jakarta.data.Sort;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

class DocumentQueryConverter {

    private DocumentQueryConverter() {
    }


    /**
     * Rejects a condition tree that contains a native function predicate. RQL has no scalar function that can be
     * used in a filter without an index or a scan, so every native function is unsupported. The check does not
     * need a session, which lets the manager reject the query before any request reaches the server.
     *
     * @param condition the condition of the query
     * @throws UnsupportedOperationException when any predicate of the tree has a native function operand
     */
    static void requireNoNativeFunction(Optional<CriteriaCondition> condition) {
        condition.ifPresent(DocumentQueryConverter::requireNoNativeFunction);
    }

    private static void requireNoNativeFunction(CriteriaCondition condition) {
        if (condition.isNative()) {
            throw new UnsupportedOperationException("The RavenDB driver does not support native functions in a "
                    + "predicate: RQL has no scalar function for filtering that does not require an index, a "
                    + "script or a scan, and the driver does not evaluate functions on the client");
        }
        switch (condition.condition()) {
            case AND, OR -> condition.element().value().get(new TypeReference<List<CriteriaCondition>>() {
            }).forEach(DocumentQueryConverter::requireNoNativeFunction);
            case NOT -> requireNoNativeFunction(condition.element().get(CriteriaCondition.class));
            default -> {
            }
        }
    }

    public static QueryResult createQuery(IDocumentSession session, SelectQuery query) {

        requireNoNativeFunction(query.condition());
        List<String> ids = new ArrayList<>();
        IDocumentQuery<HashMap> ravenQuery = session.query(HashMap.class, Query.collection(query.name()));
        Optional<CriteriaCondition> condition = query.condition();
        if (condition.isPresent() && isIdLookup(condition.get())) {
            idsOf(condition.get().element(), ids);
            return new QueryResult(ids, null);
        }
        condition.ifPresent(c -> feedQuery(ravenQuery, c));
        return appendRavenQuery(query, ids, ravenQuery);

    }

    private static QueryResult appendRavenQuery(SelectQuery query, List<String> ids, IDocumentQuery<HashMap> ravenQuery) {
        Consumer<Sort> sortConsumer = s -> {
            if(s.isDescending()){
                ravenQuery.orderByDescending(s.property());
            } else {
                ravenQuery.orderBy(s.property());
            }
        };
        query.sorts().forEach(sortConsumer);

        if (query.skip() > 0) {
            ravenQuery.skip((int) query.skip());
        }

        if (query.limit() > 0) {
            ravenQuery.take((int) query.limit());
        }
        return new QueryResult(ids, ravenQuery);
    }

    private static boolean isIdLookup(CriteriaCondition condition) {
        return (condition.condition() == Condition.EQUALS || condition.condition() == Condition.IN)
                && EntityConverter.ID_FIELD.equals(condition.element().name());
    }

    private static void idsOf(Element element, List<String> ids) {
        Object value = element.get();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(i -> ids.add(i.toString()));
        } else {
            ids.add(value.toString());
        }
    }

    private static void feedGroup(IDocumentQuery<HashMap> ravenQuery, CriteriaCondition condition,
                                  boolean and) {
        boolean first = true;
        for (CriteriaCondition child : condition.element().value()
                .get(new TypeReference<List<CriteriaCondition>>() {
                })) {
            if (!first) {
                if (and) {
                    ravenQuery.andAlso();
                } else {
                    ravenQuery.orElse();
                }
            }
            first = false;
            boolean grouped = child.condition() == Condition.AND || child.condition() == Condition.OR;
            if (grouped) {
                ravenQuery.openSubclause();
            }
            feedQuery(ravenQuery, child);
            if (grouped) {
                ravenQuery.closeSubclause();
            }
        }
    }

    private static void feedQuery(IDocumentQuery<HashMap> ravenQuery, CriteriaCondition condition) {
        Element document = condition.element();
        Object value = document.get();
        String name = EntityConverter.ID_FIELD.equals(document.name()) ? "id()" : document.name();

        switch (condition.condition()) {
            case EQUALS:
                ravenQuery.whereEquals(name, value);
                return;
            case GREATER_THAN:
                ravenQuery.whereGreaterThan(name, value);
                return;
            case GREATER_EQUALS_THAN:
                ravenQuery.whereGreaterThanOrEqual(name, value);
                return;
            case LESSER_THAN:
                ravenQuery.whereLessThan(name, value);
                return;
            case LESSER_EQUALS_THAN:
                ravenQuery.whereLessThanOrEqual(name, value);
                return;
            case IN:
                ravenQuery.whereIn(name, ValueUtil.convertToList(document.value()));
                return;
            case NOT:
                CriteriaCondition negated = document.get(CriteriaCondition.class);
                boolean grouped = negated.condition() == Condition.AND || negated.condition() == Condition.OR;
                ravenQuery.negateNext();
                if (grouped) {
                    ravenQuery.openSubclause();
                }
                feedQuery(ravenQuery, negated);
                if (grouped) {
                    ravenQuery.closeSubclause();
                }
                return;
            case LIKE:
                throw new UnsupportedOperationException("Raven does not support LIKE Operator");
            case AND:
                feedGroup(ravenQuery, condition, true);
                return;
            case OR:
                feedGroup(ravenQuery, condition, false);
                return;
            default:
                throw new UnsupportedOperationException("The condition " + condition.condition()
                        + " is not supported from ravendb diana driver");
        }
    }


    static class QueryResult {

        private final List<String> ids;

        private final IDocumentQuery<HashMap> ravenQuery;

        private QueryResult(List<String> ids, IDocumentQuery<HashMap> ravenQuery) {
            this.ids = ids;
            this.ravenQuery = ravenQuery;
        }

        public List<String> getIds() {
            return ids;
        }

        public Optional<IDocumentQuery<HashMap>> getRavenQuery() {
            return Optional.ofNullable(ravenQuery);
        }
    }
}

