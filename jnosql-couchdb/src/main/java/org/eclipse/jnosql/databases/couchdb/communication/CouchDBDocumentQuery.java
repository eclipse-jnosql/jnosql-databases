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

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A CouchDB specialization of {@link SelectQuery} that allows query with bookmark which can do pagination.
 *
 * @see CouchDBDocumentQuery#of(SelectQuery)
 * @see CouchDBDocumentQuery#of(SelectQuery, String)
 */
public final class CouchDBDocumentQuery implements SelectQuery {


    private final SelectQuery query;

    private String bookmark;


    private CouchDBDocumentQuery(SelectQuery query) {
        this.query = query;
    }

    /**
     * The A string that enables you to specify which page of results you require. Used for paging
     * through result sets. Every query returns an opaque string under the bookmark key that can
     * then be passed back in a query to get the next page of results. If any part of the selector query
     * changes between requests, the results are undefined. Optional, default: null
     *
     * @return the bookmark
     */
    public Optional<String> getBookmark() {
        return Optional.ofNullable(bookmark);
    }

    void setBookmark(Map<String, Object> json) {
        json.computeIfPresent(CouchDBConstant.BOOKMARK, (k, v) -> this.bookmark = v.toString());
    }

    @Override
    public long limit() {
        return query.limit();
    }

    @Override
    public long skip() {
        return query.skip();
    }

    @Override
    public String name() {
        return query.name();
    }

    @Override
    public Optional<CriteriaCondition> condition() {
        return query.condition();
    }

    @Override
    public List<Sort<?>> sorts() {
        return query.sorts();
    }

    @Override
    public List<String> columns() {
        return query.columns();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CouchDBDocumentQuery that = (CouchDBDocumentQuery) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(bookmark, that.bookmark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, bookmark);
    }

    @Override
    public String toString() {
        return "CouchDBDocumentQuery{" +
                "query=" + query +
                ", bookmark='" + bookmark + '\'' +
                '}';
    }

    /**
     * returns a new instance of {@link CouchDBDocumentQuery}
     *
     * @param query the {@link SelectQuery}
     * @return a new instance
     * @throws NullPointerException when query is null
     */
    public static CouchDBDocumentQuery of(SelectQuery query) {
        Objects.requireNonNull(query, "query is required ");
        return new CouchDBDocumentQuery(query);
    }

    /**
     * returns a new instance of {@link CouchDBDocumentQuery}
     *
     * @param query    the {@link SelectQuery}
     * @param bookmark {@link CouchDBDocumentQuery#bookmark}
     * @return a new instance
     * @throws NullPointerException when there is null parameter
     */
    public static CouchDBDocumentQuery of(SelectQuery query, String bookmark) {
        Objects.requireNonNull(query, "query is required ");
        Objects.requireNonNull(bookmark, "bookmark is required ");
        CouchDBDocumentQuery couchDBDocumentQuery = new CouchDBDocumentQuery(query);
        couchDBDocumentQuery.bookmark = bookmark;
        return couchDBDocumentQuery;
    }


}
