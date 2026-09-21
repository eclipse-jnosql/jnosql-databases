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
package org.eclipse.jnosql.databases.scylladb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A ScyllaDB specialization of {@link SelectQuery} that allows query with paging state which can do pagination.
 *
 * @see ScyllaDBQuery#of(SelectQuery)
 * @see ScyllaDBQuery#of(SelectQuery, String)
 */
public final class ScyllaDBQuery implements SelectQuery {

    private static final String EXHAUSTED = "EXHAUSTED";
    private static final Predicate<String> EQUALS = EXHAUSTED::equals;
    private static final Predicate<String> NOT_EQUALS = EQUALS.negate();

    private final SelectQuery query;

    /**
     * This object represents the next page to be fetched if the query is multi page.
     * It can be saved and reused later on the same statement.
     */
    private String pagingState;


    private ScyllaDBQuery(SelectQuery query) {
        this.query = query;
    }


    /**
     * {@link ScyllaDBQuery#pagingState}
     *
     * @return the {@link ScyllaDBQuery#pagingState}
     */
    public Optional<String> getPagingState() {
        return Optional.ofNullable(pagingState);
    }

    Optional<ByteBuffer> toPaginate() {
        return getPagingState().filter(NOT_EQUALS).map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
    }

    void setPagingState(ByteBuffer pagingState) {
        if (pagingState != null) {
            this.pagingState = StandardCharsets.UTF_8.decode(pagingState).toString();
        }
    }


    void setExhausted(boolean exhausted) {
        synchronized (this) {
            if (exhausted) {
                this.pagingState = EXHAUSTED;
            }
        }
    }


    boolean isExhausted() {
        return EXHAUSTED.equals(pagingState);
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
        ScyllaDBQuery that = (ScyllaDBQuery) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(pagingState, that.pagingState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, pagingState);
    }

    @Override
    public String toString() {
        return "CouchDBDocumentQuery{" + "query=" + query +
                ", pagingState='" + pagingState + '\'' +
                '}';
    }

    /**
     * returns a new instance of {@link ScyllaDBQuery}
     *
     * @param query the {@link SelectQuery}
     * @return a new instance
     * @throws NullPointerException when query is null
     */
    public static ScyllaDBQuery of(SelectQuery query) {
        Objects.requireNonNull(query, "query is required ");
        return new ScyllaDBQuery(query);
    }

    /**
     * returns a new instance of {@link ScyllaDBQuery}
     *
     * @param query       the {@link SelectQuery}
     * @param pagingState {@link ScyllaDBQuery#pagingState}
     * @return a new instance
     * @throws NullPointerException when there is null parameter
     */
    public static ScyllaDBQuery of(SelectQuery query, String pagingState) {
        Objects.requireNonNull(query, "query is required ");
        Objects.requireNonNull(pagingState, "pagingState is required ");
        ScyllaDBQuery scylladbQuery = new ScyllaDBQuery(query);
        scylladbQuery.pagingState = pagingState;
        return scylladbQuery;
    }
}