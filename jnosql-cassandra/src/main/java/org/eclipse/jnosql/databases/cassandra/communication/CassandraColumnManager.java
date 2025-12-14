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
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The Cassandra implementation of {@link DatabaseManager}, that supports all methods and also supports
 * CQL and ConsistencyLevel.
 * <p>{@link CassandraColumnManager#select(SelectQuery, ConsistencyLevel)}</p>
 * <p>{@link CassandraColumnManager#cql(String)}</p>
 * <p>{@link CassandraColumnManager#nativeQueryPrepare(String)}</p>
 * <p>{@link CassandraColumnManager#delete(DeleteQuery, ConsistencyLevel)}</p>
 */
public interface CassandraColumnManager extends DatabaseManager {


    /**
     * Saves a ColumnEntity with a defined ConsistencyLevel
     *
     * @param entity the entity
     * @param level  the {@link ConsistencyLevel}
     * @return the entity saved
     * @throws NullPointerException when both entity or level are null
     */
    CommunicationEntity save(CommunicationEntity entity, ConsistencyLevel level) throws NullPointerException;


    /**
     * Saves an entity using {@link ConsistencyLevel}
     *
     * @param entity the entity
     * @param ttl    the ttl
     * @param level  the level
     * @return the entity saved
     * @throws NullPointerException when either entity or ttl or level are null
     */
    CommunicationEntity save(CommunicationEntity entity, Duration ttl, ConsistencyLevel level) throws NullPointerException;

    /**
     * Saves a ColumnEntity with a defined ConsistencyLevel
     *
     * @param entities the entities
     * @param level    the {@link ConsistencyLevel}
     * @return the entities saved
     * @throws NullPointerException when both entity or level are null
     */
    Iterable<CommunicationEntity> save(Iterable<CommunicationEntity> entities, ConsistencyLevel level) throws NullPointerException;


    /**
     * Saves an entity using {@link ConsistencyLevel}
     *
     * @param entities the entities
     * @param ttl      the ttl
     * @param level    the level
     * @return the entities saved
     * @throws NullPointerException when either entity or ttl or level are null
     */
    Iterable<CommunicationEntity> save(Iterable<CommunicationEntity> entities, Duration ttl, ConsistencyLevel level) throws NullPointerException;

    /**
     * Deletes an information using {@link ConsistencyLevel}
     *
     * @param query the query
     * @param level the level
     * @throws NullPointerException when either query or level are null
     */
    void delete(DeleteQuery query, ConsistencyLevel level) throws NullPointerException;

    /**
     * Finds using a consistency level
     *
     * @param query the query
     * @param level the consistency level
     * @return the query using a consistency level
     * @throws NullPointerException when either query or level are null
     */
    Stream<CommunicationEntity> select(SelectQuery query, ConsistencyLevel level) throws NullPointerException;

    /**
     * Count based on a query using a consistency level
     *
     * @param query the query
     * @param level the consistency level
     * @return the query using a consistency level
     * @throws NullPointerException when either query or level are null
     */
    long count(SelectQuery query, ConsistencyLevel level) throws NullPointerException;


    /**
     * Executes CQL
     *
     * @param query the Cassandra query language
     * @return the result of this query
     * @throws NullPointerException when query is null
     */
    Stream<CommunicationEntity> cql(String query) throws NullPointerException;


    /**
     * Executes CQL using the provided named values.
     * <p>E.g.: SELECT * FROM users WHERE id = :i", Map&#60;String, Object&#62;of("i", 1)</p>
     *
     * @param query  the Cassndra query language
     * @param values values required for the execution of {@code query}
     * @return the result of this query
     * @throws NullPointerException when either query or values are null
     */
    Stream<CommunicationEntity> cql(String query, Map<String, Object> values) throws NullPointerException;

    /**
     * Executes a statement
     *
     * @param statement the statement
     * @return the result of this query
     * @throws NullPointerException when statement is null
     */
    Stream<CommunicationEntity> execute(SimpleStatement statement) throws NullPointerException;

    /**
     * Executes an query and uses as {@link CassandraPreparedStatement}
     *
     * @param query the query
     * @return the CassandraPrepareStatment instance
     * @throws NullPointerException when query is null
     */
    CassandraPreparedStatement nativeQueryPrepare(String query) throws NullPointerException;
}
