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
package org.eclipse.jnosql.databases.cassandra.communication;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.stream.Stream;

interface QueryExecutor {

    static QueryExecutor of(SelectQuery query) {
        if (CassandraQuery.class.isInstance(query)) {
            return QueryExecutorType.PAGING_STATE;
        }
        return QueryExecutorType.DEFAULT;
    }

    default Stream<CommunicationEntity> execute(String keyspace, SelectQuery query, DefaultCassandraColumnManager manager) {
        return execute(keyspace, query, null, manager);
    }

    Stream<CommunicationEntity> execute(String keyspace, SelectQuery query, ConsistencyLevel level,
                                        DefaultCassandraColumnManager manager);

    default long count(String keyspace, SelectQuery query, DefaultCassandraColumnManager manager) {
        return count(keyspace, query, null, manager);
    }

    long count(String keyspace, SelectQuery query, ConsistencyLevel level, DefaultCassandraColumnManager manager);

}