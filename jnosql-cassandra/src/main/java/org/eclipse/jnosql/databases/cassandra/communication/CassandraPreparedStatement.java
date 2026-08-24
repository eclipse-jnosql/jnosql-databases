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

package org.eclipse.jnosql.databases.cassandra.communication;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;

import java.util.stream.Stream;

/**
 * Wrapper class for {@link com.datastax.oss.driver.api.core.cql.PreparedStatement} in JNoSQL.
 * Allows execution of CQL queries and binding of parameters.
 */
public class CassandraPreparedStatement {

    private final com.datastax.oss.driver.api.core.cql.PreparedStatement prepare;
    private final CqlSession session;
    private BoundStatement boundStatement;

    /**
     * Constructs a new CassandraPreparedStatement.
     *
     * @param prepare the underlying Cassandra PreparedStatement
     * @param session the CQL session
     */
    CassandraPreparedStatement(com.datastax.oss.driver.api.core.cql.PreparedStatement prepare, CqlSession session) {
        this.prepare = prepare;
        this.session = session;
    }

    /**
     * Executes the prepared statement as a query and returns the results as a stream of CommunicationEntity.
     *
     * @return a stream of CommunicationEntity containing the results of the query
     */
    public Stream<CommunicationEntity> executeQuery() {
        load();
        ResultSet resultSet = session.execute(boundStatement);
        return resultSet.all().stream().map(CassandraConverter::toDocumentEntity);
    }

    /**
     * Binds values to the prepared statement.
     *
     * @param values the values to bind
     * @return this CassandraPreparedStatement instance
     */
    public CassandraPreparedStatement bind(Object... values) {
        boundStatement = prepare.bind(values);
        return this;
    }

    private void load() {
        if (boundStatement == null) {
            boundStatement = prepare.bind();
        }
    }

    /**
     * Returns a string representation of this CassandraPreparedStatement.
     *
     * @return a string representation of this CassandraPreparedStatement
     */
    @Override
    public String toString() {
        return "CassandraPreparedStatement{" +
                "prepare=" + prepare +
                ", session=" + session +
                ", boundStatement=" + boundStatement +
                '}';
    }
}