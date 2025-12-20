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

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

final class QueryOSQLFactory {


    private QueryOSQLFactory() {
    }

    static QueryResult to(SelectQuery documentQuery) {
        QueryOSQLConverter.Query query = QueryOSQLConverter.select(documentQuery);
        return new QueryResult(query.query(), query.params(), query.ids());
    }

    static QueryResult countTo(SelectQuery documentQuery) {
        QueryOSQLConverter.Query query = QueryOSQLConverter.selectCount(documentQuery);
        return new QueryResult(query.query(), query.params(), query.ids());
    }

    static QueryResultAsync toAsync(SelectQuery documentQuery, Consumer<Stream<ODocument>> callBack) {
        QueryOSQLConverter.Query query = QueryOSQLConverter.select(documentQuery);

        return new QueryResultAsync(new OSQLAsynchQuery<>(query.query(), new OCommandResultListener() {
            private List<ODocument> documents = new ArrayList<>();

            @Override
            public boolean result(Object iRecord) {
                ODocument document = (ODocument) iRecord;
                documents.add(document);
                return true;
            }

            @Override
            public void end() {
                callBack.accept(documents.stream());
            }

            @Override
            public Object getResult() {
                return null;
            }
        }), query.params());
    }

    static QueryResult toLive(SelectQuery documentQuery, OrientDBLiveCallback callbacks) {
        QueryOSQLConverter.Query query = QueryOSQLConverter.select(documentQuery);
        return new QueryResult(query.query(), query.params(), Collections.emptyList());
    }

    static QueryResultAsync toAsync(String query, Consumer<List<ODocument>> callBack, Object... params) {
        return new QueryResultAsync(new OSQLAsynchQuery<>(query, new OCommandResultListener() {
            private List<ODocument> documents = new ArrayList<>();

            @Override
            public boolean result(Object iRecord) {
                ODocument document = (ODocument) iRecord;
                documents.add(document);
                return true;
            }

            @Override
            public void end() {
                callBack.accept(documents);
            }

            @Override
            public Object getResult() {
                return null;
            }
        }), asList(params));
    }

    static class QueryResult {

        private final String query;
        private final List<Object> params;
        private final List<ORecordId> ids;

        QueryResult(String query, List<Object> params, List<ORecordId> ids) {
            this.query = query;
            this.params = params;
            this.ids = ids;
        }

        String getQuery() {
            return query;
        }

        Object[] getParams() {
            return params.toArray(new Object[0]);
        }

        public List<ORecordId> getIds() {
            return ids;
        }

        public boolean isRunQuery() {
            return ids.isEmpty() || (!ids.isEmpty() && !params.isEmpty());
        }

        public boolean isLoad() {
            return !ids.isEmpty();
        }
    }

    static class QueryResultAsync {

        private final OSQLQuery<ODocument> query;
        private final List<Object> params;

        QueryResultAsync(OSQLQuery<ODocument> query, List<Object> params) {
            this.query = query;
            this.params = params;
        }

        OSQLQuery<ODocument> getQuery() {
            return query;
        }

        Object[] getParams() {
            return params.toArray(new Object[0]);
        }
    }

}
